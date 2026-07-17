package com.jtprince.coordinateoffset;

import com.jtprince.coordinateoffset.adapter.OffsetLocation;
import com.jtprince.coordinateoffset.adapter.OffsetWorld;
import com.jtprince.coordinateoffset.api.CoordinateOffset;
import org.jspecify.annotations.NullMarked;

import java.util.Random;

@NullMarked
public record ScalableOffset(int x, int z) implements Offset {
    static final Random RANDOM = new Random();

    @Override
    public String toString() {
        return "[x=" + x + ", z=" + z + "]";
    }

    @Override
    public <T> T apply(T location) throws ClassCastException {
        OffsetLocation l;
        if (location instanceof OffsetLocation) {
            l = (OffsetLocation) location;
        } else {
            l = CoordinateOffset.api().adaptLocation(location);
        }

        OffsetLocation applied = l.apply(this.scaleToWorld(l.getWorld()));

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

        OffsetLocation unapplied = l.unapply(this.scaleToWorld(l.getWorld()));

        if (location instanceof OffsetLocation) {
            return (T) unapplied;
        } else {
            return (T) unapplied.getPlatformLocationObject();
        }
    }

    @Override
    public ScalableOffset negate() {
        return new ScalableOffset(-x, -z);
    }

    @Override
    public boolean isZero() {
        return x == 0 && z == 0;
    }

    public FixedOffset scaleDownAndRound(double divisor) {
        return new FixedOffset(
            Offset.alignComponentToConfiguredMultiple((int) (x / divisor)),
            Offset.alignComponentToConfiguredMultiple((int) (z / divisor))
        );
    }

    @Deprecated(forRemoval = true)
    public FixedOffset scaleDownBy(double divisor) {
        if (divisor == 0.0) {

            return new FixedOffset(0, 0);
        }
        return new FixedOffset(
            Offset.alignComponent((int) (x / divisor), 0),
            Offset.alignComponent((int) (z / divisor), 0)
        );
    }

    public FixedOffset scaleToWorld(OffsetWorld world) {
        return this.scaleDownAndRound(world.getCoordinateScale());
    }
}
