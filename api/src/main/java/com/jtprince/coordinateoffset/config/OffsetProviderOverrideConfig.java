package com.jtprince.coordinateoffset.config;

import com.jtprince.coordinateoffset.provider.OffsetProvider;
import com.jtprince.coordinateoffset.provider.OffsetProviderContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface OffsetProviderOverrideConfig {

    OffsetProvider getOffsetProvider();

    @Nullable String getWorld();

    @Nullable String getPermission();

    @Nullable String getPlayer();

    default boolean appliesTo(OffsetProviderContext context) {
        if (getPlayer() != null
            && !getPlayer().equalsIgnoreCase(context.player().getName())
            && !getPlayer().equalsIgnoreCase(context.player().getUuid().toString())) return false;
        if (getWorld() != null
            && !getWorld().equalsIgnoreCase(context.playerLocation().getWorld().getName())
            && !getWorld().equalsIgnoreCase(context.playerLocation().getWorld().getKey())
            && !getWorld().equalsIgnoreCase(context.playerLocation().getWorld().getUuid().toString())) return false;
        if (getPermission() != null && !context.player().hasPermission(getPermission())) return false;

        return true;
    }
}
