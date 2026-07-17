package com.jtprince.coordinateoffset.adapter;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public interface OffsetWorld {
    UUID getUuid();

    String getName();

    String getKey();

    Double getCoordinateScale();

    Object getPlatformPlayerObject();
}
