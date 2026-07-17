package com.jtprince.coordinateoffset;

import org.jspecify.annotations.Nullable;

public record OffsetChange(
    @Nullable OffsetData previousOffsetData,
    OffsetData newOffsetData
) {
    public boolean offsetChanged() {
        return previousOffsetData == null || !previousOffsetData.offset().equals(newOffsetData.offset());
    }
}
