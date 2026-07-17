package com.jtprince.coordinateoffset.folia.adapter;

import com.jtprince.coordinateoffset.FixedOffset;
import com.jtprince.coordinateoffset.adapter.OffsetLocation;
import com.jtprince.coordinateoffset.adapter.OffsetWorld;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class FoliaLocation implements OffsetLocation {
    private final Location location;
    public FoliaLocation(Location location) {
        this.location = location;
    }

    @Override
    public @Nullable OffsetWorld getWorld() {
        if (location.getWorld() == null) return null;
        return new FoliaWorld(location.getWorld().getUID());
    }

    @Override
    public double getX() {
        return location.getX();
    }

    @Override
    public double getY() {
        return location.getY();
    }

    @Override
    public double getZ() {
        return location.getZ();
    }

    @Override
    public OffsetLocation apply(FixedOffset offset) {
        return new FoliaLocation(location.clone().subtract(offset.x(), 0, offset.z()));
    }

    @Override
    public OffsetLocation unapply(FixedOffset offset) {
        return new FoliaLocation(location.clone().add(offset.x(), 0, offset.z()));
    }

    @Override
    public Location getPlatformLocationObject() {
        return location;
    }

    @Override
    public String toString() {
        return location.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof FoliaLocation p) && location.equals(p.location);
    }
}
