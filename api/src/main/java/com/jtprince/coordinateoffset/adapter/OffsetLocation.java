package com.jtprince.coordinateoffset.adapter;

import com.jtprince.coordinateoffset.FixedOffset;
import org.checkerframework.dataflow.qual.Pure;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface OffsetLocation {
    OffsetWorld getWorld();
    double getX();
    double getY();
    double getZ();

    @Deprecated
    @Nullable default String getWorldName() {
        return getWorld().getName();
    }

    @Pure
    OffsetLocation apply(FixedOffset offset);

    @Pure
    OffsetLocation unapply(FixedOffset offset);

    default @Nullable Double getDistance(OffsetLocation other) {
        if (!getWorld().equals(other.getWorld())) {
            return null;
        }
        return Math.sqrt(Math.pow(getX() - other.getX(), 2) + Math.pow(getY() - other.getY(), 2) + Math.pow(getZ() - other.getZ(), 2));
    }

    Object getPlatformLocationObject();
}
