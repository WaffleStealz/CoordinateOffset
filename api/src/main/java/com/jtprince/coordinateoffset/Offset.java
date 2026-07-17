package com.jtprince.coordinateoffset;

import com.jtprince.coordinateoffset.adapter.OffsetLocation;
import com.jtprince.coordinateoffset.api.CoordinateOffset;
import org.checkerframework.dataflow.qual.Pure;
import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface Offset permits FixedOffset, ScalableOffset {

    static FixedOffset fixed(int x, int z) {
        return new FixedOffset(x, z);
    }

    static ScalableOffset scalable(int x, int z) {
        return new ScalableOffset(x, z);
    }

    FixedOffset ZERO = new FixedOffset(0, 0);

    @Deprecated(forRemoval = true)
    int ALIGN_DEFAULT_OVERWORLD = 3;

    static ScalableOffset align(int x, int z) {
        return new ScalableOffset(
            alignComponentToConfiguredMultiple(x),
            alignComponentToConfiguredMultiple(z)
        );
    }

    @Deprecated(forRemoval = true)
    static ScalableOffset align(int x, int z, int toChunksPower) {
        return new ScalableOffset(alignComponent(x, toChunksPower), alignComponent(z, toChunksPower));
    }

    static ScalableOffset random(int bound) {
        return new ScalableOffset(
            Offset.alignComponentToConfiguredMultiple(ScalableOffset.RANDOM.nextInt(bound)),
            Offset.alignComponentToConfiguredMultiple(ScalableOffset.RANDOM.nextInt(bound))
        );
    }

    @Deprecated(forRemoval = true)
    static ScalableOffset random(int bound, int alignToChunksPower) {
        return new ScalableOffset(
            alignComponent(ScalableOffset.RANDOM.nextInt(bound), alignToChunksPower),
            alignComponent(ScalableOffset.RANDOM.nextInt(bound), alignToChunksPower)
        );
    }

    static int alignComponent(int component, int alignToChunksPower) {
        return Math.round((float) component / (1 << (alignToChunksPower + 4))) * (1 << (alignToChunksPower + 4));
    }

    static int alignComponentToConfiguredMultiple(int component) {
        int configuredMultiple = CoordinateOffset.api().getConfig().getOffsetsAreMultiplesOfBlocks();
        return Math.round((float) component / configuredMultiple) * configuredMultiple;
    }

    @Pure
    <T> T apply(T location) throws ClassCastException;

    @Pure
    <T> T unapply(T location) throws ClassCastException;

    @Pure
    Offset negate();

    boolean isZero();
}
