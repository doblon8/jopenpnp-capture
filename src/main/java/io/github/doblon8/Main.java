package io.github.doblon8;

import io.github.doblon8.bindings.CapFormatInfo;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static io.github.doblon8.CaptureContext.createContext;
import static io.github.doblon8.bindings.openpnp_capture.*;

public class Main {
    public static void main(String[] args) {
        Cap_getLibraryVersion capGetLibraryVersion = Cap_getLibraryVersion.makeInvoker();
        String libraryVersion = capGetLibraryVersion.apply().getString(0);
        System.out.println("OpenPnP Capture version: " + libraryVersion);

        CaptureContext context = createContext();
        int deviceCount = Cap_getDeviceCount(context.segment());
        System.out.println("Number of devices: " + deviceCount);

        for (int i = 0; i < deviceCount; i++) {
            String deviceName = Cap_getDeviceName(context.segment(), i).getString(0);
            System.out.println("Device " + i + ": " + deviceName);

            int numFormats = Cap_getNumFormats(context.segment(), i);
            System.out.println("Number of formats: " + numFormats);

            for (int j = 0; j < numFormats; j++) {
                try (Arena arena = Arena.ofConfined()) {
                    MemorySegment formatInfoSegment = CapFormatInfo.allocate(arena);
                    int returnValue = Cap_getFormatInfo(context.segment(), i, j, formatInfoSegment);
                    if (returnValue != 0) {
                        System.err.println("Error getting format info for device " + i + ", format " + j);
                        continue;
                    }
                    CaptureFormatInfo formatInfo = new CaptureFormatInfo(formatInfoSegment);
                    int width = formatInfo.width();
                    int height = formatInfo.height();
                    int fourcc = formatInfo.fourcc();
                    int fps = formatInfo.fps();
                    int bpp = formatInfo.bpp();
                    System.out.printf("Format %d: %dx%d, FourCC: %d, FPS: %d, BPP: %d%n",
                                      j, width, height, fourcc, fps, bpp);
                }
            }
        }
    }
}
