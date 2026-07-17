package com.jtprince.coordinateoffset.adapter;

import org.jspecify.annotations.NullMarked;

import java.util.Set;
import java.util.UUID;

@NullMarked
public interface OffsetPlayer {
    UUID getUuid();
    String getName();
    boolean hasPermission(String permission);
    Set<String> getAllPermissions();
    OffsetLocation getLocation();

    Object getPlatformPlayerObject();
}
