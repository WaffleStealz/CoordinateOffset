package com.jtprince.coordinateoffset.api;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class CoordinateOffset {
    private static @Nullable CoordinateOffsetAPI apiSingleton = null;

    public static CoordinateOffsetAPI api() {
        if (apiSingleton == null) {
            throw new IllegalStateException("CoordinateOffset API is not yet initialized.");
        }
        return apiSingleton;
    }

    static void set(CoordinateOffsetAPI api) {
        if (apiSingleton != null) {
            throw new IllegalStateException("CoordinateOffset API is already initialized.");
        }
        apiSingleton = api;
    }
}
