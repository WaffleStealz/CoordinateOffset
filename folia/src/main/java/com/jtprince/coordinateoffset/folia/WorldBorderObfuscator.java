package com.jtprince.coordinateoffset.folia;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerInitializeWorldBorder;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorderCenter;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorderSize;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayWorldBorderLerpSize;
import com.jtprince.coordinateoffset.CoordinateOffsetCore;
import com.jtprince.coordinateoffset.FixedOffset;
import com.jtprince.coordinateoffset.folia.adapter.FoliaOffsetPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
class WorldBorderObfuscator {
    private static final double BASELINE_SIZE = 60_000_000;

    private final CoordinateOffsetFoliaPlugin plugin;
    private final ConcurrentHashMap<UUID, EnumSet<Wall>> knownSeenWalls = new ConcurrentHashMap<>();

    WorldBorderObfuscator(CoordinateOffsetFoliaPlugin plugin) {
        this.plugin = plugin;
    }

    void tryUpdatePlayerBorders(Player player, Location movingTo) {
        EnumSet<Wall> currentlyVisible = visibleBorders(movingTo);
        if (!currentlyVisible.equals(knownSeenWalls.get(player.getUniqueId()))) {
            if (CoordinateOffsetCore.get().isDebugEnabled()) {
                CoordinateOffsetCore.get().getLogger().info("Seen walls update for " + player.getName() + ": " + currentlyVisible);
            }
            knownSeenWalls.put(player.getUniqueId(), currentlyVisible);

            if (player.isOnline() && enableObfuscation()) {
                try {
                    player.setWorldBorder(player.getWorldBorder());
                } catch (NoSuchMethodError e) {

                }
            }
        }
    }

    void onPlayerDisconnect(UUID playerUuid) {
        knownSeenWalls.remove(playerUuid);
    }

    boolean enableObfuscation() {
        return CoordinateOffsetCore.get().getConfig().getObfuscateWorldBorder();
    }

    private EnumSet<Wall> visibleBorders(Location location) {
        double viewDistanceBlocks = Objects.requireNonNull(location.getWorld()).getViewDistance() * 16;

        WorldBorder realBorder = location.getWorld().getWorldBorder();
        double xMax = realBorder.getCenter().getX() + realBorder.getSize() / 2;
        double xMin = realBorder.getCenter().getX() - realBorder.getSize() / 2;
        double zMax = realBorder.getCenter().getZ() + realBorder.getSize() / 2;
        double zMin = realBorder.getCenter().getZ() - realBorder.getSize() / 2;

        EnumSet<Wall> seen = EnumSet.noneOf(Wall.class);
        if (xMax - location.getX() < viewDistanceBlocks) {
            seen.add(Wall.X_POSITIVE);
        }
        if (location.getX() - xMin < viewDistanceBlocks) {
            seen.add(Wall.X_NEGATIVE);
        }
        if (zMax - location.getZ() < viewDistanceBlocks) {
            seen.add(Wall.Z_POSITIVE);
        }
        if (location.getZ() - zMin < viewDistanceBlocks) {
            seen.add(Wall.Z_NEGATIVE);
        }

        return seen;
    }

    void translate(PacketSendEvent packet, Player player) {
        FixedOffset offset = CoordinateOffsetCore.get().getOffsetHolder().getOffset(new FoliaOffsetPlayer(player)).offset();

        double scaleFactor;
        if (packet.getServerVersion().isOlderThan(ServerVersion.V_1_21_9)
            && player.getWorld().getEnvironment().equals(World.Environment.NETHER)) {
            scaleFactor = 8.0;
        } else {
            scaleFactor = 1.0;
        }

        EnumSet<Wall> seenWalls = knownSeenWalls.getOrDefault(player.getUniqueId(), EnumSet.noneOf(Wall.class));
        if (!enableObfuscation() ||
                (seenWalls.contains(Wall.X_POSITIVE) && seenWalls.contains(Wall.X_NEGATIVE)) ||
                (seenWalls.contains(Wall.Z_POSITIVE) && seenWalls.contains(Wall.Z_NEGATIVE))) {

            if (packet.getPacketType().equals(PacketType.Play.Server.INITIALIZE_WORLD_BORDER)) {
                WrapperPlayServerInitializeWorldBorder wrapper = new WrapperPlayServerInitializeWorldBorder(packet);
                wrapper.setX(wrapper.getX() - (offset.x() * scaleFactor));
                wrapper.setZ(wrapper.getZ() - (offset.z() * scaleFactor));
                wrapper.setPortalTeleportBoundary(60_000_000);
            } else if (packet.getPacketType().equals(PacketType.Play.Server.WORLD_BORDER_CENTER)) {
                WrapperPlayServerWorldBorderCenter wrapper = new WrapperPlayServerWorldBorderCenter(packet);
                wrapper.setX(wrapper.getX() - (offset.x() * scaleFactor));
                wrapper.setZ(wrapper.getZ() - (offset.z() * scaleFactor));
            }
            return;
        }

        WorldBorder border = player.getWorldBorder();

        if (border == null) border = player.getWorld().getWorldBorder();

        double centerX = 0.0, centerZ = 0.0;
        final double diameter = BASELINE_SIZE;

        if (seenWalls.size() >= 1) {

            if (seenWalls.contains(Wall.X_POSITIVE)) {
                double realXMax = border.getCenter().getX() + border.getSize() / 2;
                centerX = realXMax - (BASELINE_SIZE / 2);
                centerX -= offset.x();
            }
            if (seenWalls.contains(Wall.X_NEGATIVE)) {
                double realXMin = border.getCenter().getX() - border.getSize() / 2;
                centerX = realXMin + (BASELINE_SIZE / 2);
                centerX -= offset.x();
            }
            if (seenWalls.contains(Wall.Z_POSITIVE)) {
                double realZMax = border.getCenter().getZ() + border.getSize() / 2;
                centerZ = realZMax - (BASELINE_SIZE / 2);
                centerZ -= offset.z();
            }
            if (seenWalls.contains(Wall.Z_NEGATIVE)) {
                double realZMin = border.getCenter().getZ() - border.getSize() / 2;
                centerZ = realZMin + (BASELINE_SIZE / 2);
                centerZ -= offset.z();
            }
        } else {

            centerX = centerZ = 0.0;
        }

        if (packet.getPacketType().equals(PacketType.Play.Server.INITIALIZE_WORLD_BORDER)) {
            var wrapper = new WrapperPlayServerInitializeWorldBorder(packet);
            wrapper.setX(centerX * scaleFactor);
            wrapper.setZ(centerZ * scaleFactor);
            wrapper.setOldDiameter(diameter);
            wrapper.setNewDiameter(diameter);
            wrapper.setPortalTeleportBoundary(60_000_000);
        } else if (packet.getPacketType().equals(PacketType.Play.Server.WORLD_BORDER_CENTER)) {
            var wrapper = new WrapperPlayServerWorldBorderCenter(packet);
            wrapper.setX(centerX * scaleFactor);
            wrapper.setZ(centerZ * scaleFactor);
        } else if (packet.getPacketType().equals(PacketType.Play.Server.WORLD_BORDER_LERP_SIZE)) {
            var wrapper = new WrapperPlayWorldBorderLerpSize(packet);
            wrapper.setOldDiameter(diameter);
            wrapper.setNewDiameter(diameter);
        } else if (packet.getPacketType().equals(PacketType.Play.Server.WORLD_BORDER_SIZE)) {
            var wrapper = new WrapperPlayServerWorldBorderSize(packet);
            wrapper.setDiameter(diameter);
        }
    }

    enum Wall {
        X_POSITIVE,
        X_NEGATIVE,
        Z_POSITIVE,
        Z_NEGATIVE
    }
}
