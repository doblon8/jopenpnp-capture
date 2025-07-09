package io.github.doblon8;

import java.lang.foreign.MemorySegment;

import static io.github.doblon8.bindings.openpnp_capture.Cap_createContext;

public record CaptureContext(MemorySegment segment) {

    public static CaptureContext createContext() {
        MemorySegment segment = Cap_createContext();
        return new CaptureContext(segment);
    }
}
