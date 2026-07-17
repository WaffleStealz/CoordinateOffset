package com.jtprince.coordinateoffset.folia.adapter;

import com.jtprince.coordinateoffset.CoordinateOffsetCore;
import com.jtprince.coordinateoffset.adapter.OffsetWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.SequencedMap;
import java.util.UUID;

@NullMarked
public class FoliaWorld implements OffsetWorld {
    UUID uuid;
    public FoliaWorld(UUID worldUuid) {
        this.uuid = worldUuid;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return getPlatformPlayerObject().getName();
    }

    @Override
    public String getKey() {
        return getPlatformPlayerObject().getKey().asString();
    }

    @Override
    public Double getCoordinateScale() {
        World world = getPlatformPlayerObject();

        SequencedMap<String, Double> overrides = CoordinateOffsetCore.get().getConfig().getWorldCoordinateScaleOverrides();
        if (overrides.containsKey(world.getUID().toString())) return overrides.get(world.getUID().toString());
        if (overrides.containsKey(world.getName())) return overrides.get(world.getName());
        if (overrides.containsKey(world.getKey().asString())) return overrides.get(world.getKey().asString());

        return getPlatformPlayerObject().getCoordinateScale();
    }

    @Override
    public World getPlatformPlayerObject() {
        return Objects.requireNonNull(Bukkit.getWorld(uuid));
    }

    @Override
    public String toString() {
        return getPlatformPlayerObject().toString();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof FoliaWorld w) && uuid.equals(w.uuid);
    }
}
