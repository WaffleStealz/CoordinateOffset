package com.jtprince.coordinateoffset;

import com.jtprince.coordinateoffset.adapter.OffsetLocation;
import com.jtprince.coordinateoffset.api.CoordinateOffset;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record FixedOffset(int x, int z) implements Offset {
    public FixedOffset {
        if (x % 16 != 0) {
            throw new IllegalArgumentException("Offset x=" + x + " is not chunk-aligned! (must be a multiple of 16)");
        }
        if (z % 16 != 0) {
            throw new IllegalArgumentException("Offset z=" + z + " is not chunk-aligned! (must be a multiple of 16)");
        }
    }

    @Override
    public String toString() {
        return "[x=" + x + ", z=" + z + "]";
    }

    @Override
    public FixedOffset negate() {
        return new FixedOffset(-x, -z);
    }

    @Override
    public boolean isZero() {
        return x == 0 && z == 0;
    }

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }

    @Override
    public <T> T apply(T location) throws ClassCastException {
        OffsetLocation l;
        if (location instanceof OffsetLocation) {
            l = (OffsetLocation) location;
        } else {
            l = CoordinateOffset.api().adaptLocation(location);
        }

        OffsetLocation applied = l.apply(this);

        if (location instanceof OffsetLocation) {
            return (T) applied;
        } else {
            return (T) applied.getPlatformLocationObject();
        }
    }

    @Override
    public <T> T unapply(T location) throws ClassCastException {
        OffsetLocation l;
        if (location instanceof OffsetLocation) {
            l = (OffsetLocation) location;
        } else {
            l = CoordinateOffset.api().adaptLocation(location);
        }

        OffsetLocation unapplied = l.unapply(this);

        if (location instanceof OffsetLocation) {
            return (T) unapplied;
        } else {
            return (T) unapplied.getPlatformLocationObject();
        }
    }
}
