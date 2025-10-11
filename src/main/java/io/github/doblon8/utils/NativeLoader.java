package io.github.doblon8.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.*;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class NativeLoader {
    /**
     * Load the OpenPnP Capture native library for the current platform.
     * <p>
     * The library is loaded from the classpath and extracted to a temporary directory.
     *
     * @throws RuntimeException if loading the native library fails
     */
    public static void loadOpenpnpCapture() {
        String os = getOsName();
        String arch = getArchName();

        String basePath = "/native/" + os + "/" + arch + "/";
        String lib = switch (os) {
            case "linux" -> "libopenpnp-capture-ubuntu-22.04-" + arch + ".so";
            case "osx" -> "libopenpnp-capture-macos-latest-" + arch + ".dylib";
            case "windows" -> "libopenpnp-capture-windows-latest-" + arch + ".dll";
            default -> throw new UnsupportedOperationException("Unknown OS: " + os);
        };

        try {
            Path tempDir = createOwnerOnlyTempDir("openpnp-capture-native");
            tempDir.toFile().deleteOnExit();
            try (InputStream in = NativeLoader.class.getResourceAsStream(basePath + lib)) {
                if (in == null) {
                    throw new IllegalStateException("Native library not found: " + basePath + lib);
                }
                Path out = tempDir.resolve(lib);
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                out.toFile().deleteOnExit();
                System.load(out.toAbsolutePath().toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load native libraries", e);
        }

    }

    /**
     * Create a temporary directory with owner-only permissions.
     * <p>
     * This method attempts to create a temporary directory with permissions
     * that restrict access to the owner only. It first tries to use POSIX
     * file permissions, then falls back to ACLs on Windows, and finally
     * uses the java.io.File API as a last resort.
     *
     * @param prefix the prefix string to be used in generating the directory's name; may be null
     * @return the path to the newly created temporary directory
     * @throws IOException if an I/O error occurs or if setting permissions fails
     */
    public static Path createOwnerOnlyTempDir(String prefix) throws IOException {
        Path tmpRoot = Paths.get(System.getProperty("java.io.tmpdir"));

        // POSIX: try atomic creation with owner-only perms (rwx------)
        try {
            Set<PosixFilePermission> posixPerms = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
            );
            return Files.createTempDirectory(tmpRoot, prefix, PosixFilePermissions.asFileAttribute(posixPerms));
        } catch (UnsupportedOperationException e) {
            // Not a POSIX FS / platform; fall through to fallback logic
        }

        // Fallback: create the dir, then try ACLs (Windows) or File API
        Path dir = Files.createTempDirectory(tmpRoot, prefix);

        // Try ACL view (best-effort)
        try {
            AclFileAttributeView aclView = Files.getFileAttributeView(dir, AclFileAttributeView.class);
            if (aclView != null) {
                UserPrincipalLookupService lookup = dir.getFileSystem().getUserPrincipalLookupService();
                String userName = System.getProperty("user.name");
                UserPrincipal user = lookup.lookupPrincipalByName(userName);

                // Allow full control to current user only
                Set<AclEntryPermission> allPerms = EnumSet.allOf(AclEntryPermission.class);
                AclEntry allow = AclEntry.newBuilder()
                        .setType(AclEntryType.ALLOW)
                        .setPrincipal(user)
                        .setPermissions(allPerms)
                        .build();

                aclView.setAcl(Collections.singletonList(allow));
                return dir;
            }
        } catch (Exception ignored) {
            // ACLs not available or failed — fall back to File permission API
        }

        // Final fallback: best-effort owner-only via java.io.File flags
        File f = dir.toFile();
        // Note: Using bitwise & (not &&) to ensure all three methods run
        boolean ok =
                f.setWritable(true, true) &
                        f.setReadable(true, true) &
                        f.setExecutable(true, true);

        if (!ok) {
            throw new IOException("Failed to set owner-only permissions on temp directory: " + dir);
        }
        return dir;
    }

    /**
     * Get the name of the current operating system.
     *
     * @return the OS name as a String, either "linux", "osx", or "windows"
     */
    private static String getOsName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("nux")) {
            return "linux";
        } else if (os.contains("mac")) {
            return "osx";
        } else if (os.contains("win")) {
            return "windows";
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + os);
        }
    }

    /**
     * Get the name of the current architecture.
     *
     * @return the architecture name as a String, either "aarch64" or "x64"
     */
    private static String getArchName() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "arm64";
        } else if (arch.contains("amd64") || arch.contains("x86_64")) {
            return "x86_64";
        } else {
            throw new UnsupportedOperationException("Unsupported architecture: " + arch);
        }
    }
}
