package io.github.doblon8.utils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NativeLoader {

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
            Path tempDir = Files.createTempDirectory("openpnp-capture-native");
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
