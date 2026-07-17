package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.adapter.OffsetLocation;
import com.jtprince.coordinateoffset.adapter.OffsetPlayer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record OffsetProviderContext(
    OffsetPlayer player,
    @Nullable OffsetLocation previousLocation,
    OffsetLocation playerLocation,
    @Nullable Offset previousOffset,
    ProvideReason reason
) {
    public enum ProvideReason {

        JOIN,

        DEATH_RESPAWN,

        WORLD_CHANGE,

        TELEPORT,

        COMMAND_REGENERATE,

        COMMAND_SET,

        PLUGIN_REGENERATE,

        PLUGIN_SET
    }

    @Deprecated
    public String worldName() {
        return playerLocation.getWorld().getName();
    }
}
