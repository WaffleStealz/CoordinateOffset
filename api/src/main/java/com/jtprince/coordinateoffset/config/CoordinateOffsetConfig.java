package com.jtprince.coordinateoffset.config;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.SequencedMap;

@NullMarked
public interface CoordinateOffsetConfig {
    @Nullable Integer getConfigVersion();
    boolean getBypassByPermission();
    boolean getFixCollisionBamboo();
    boolean getFixCollisionDripstone();
    boolean getObfuscateWorldBorder();
    boolean getObfuscateDebugPropertySubscriptions();
    int getOffsetsAreMultiplesOfBlocks();
    boolean getVerbose();
    SequencedMap<String, Double> getWorldCoordinateScaleOverrides();
}
