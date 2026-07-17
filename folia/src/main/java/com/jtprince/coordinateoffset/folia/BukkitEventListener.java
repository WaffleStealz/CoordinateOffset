package com.jtprince.coordinateoffset.folia;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateViewPosition;
import com.jtprince.coordinateoffset.CoordinateOffsetCore;
import com.jtprince.coordinateoffset.OffsetChange;
import com.jtprince.coordinateoffset.adapter.OffsetPlayer;
import com.jtprince.coordinateoffset.folia.adapter.FoliaLocation;
import com.jtprince.coordinateoffset.folia.adapter.FoliaOffsetPlayer;
import com.jtprince.coordinateoffset.folia.adapter.FoliaOffsetSwapper;
import com.jtprince.coordinateoffset.provider.OffsetProvider;
import com.jtprince.coordinateoffset.provider.OffsetProviderContext;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerLoadEvent;
import org.jspecify.annotations.NullMarked;

import java.util.*;

@NullMarked
class BukkitEventListener implements Listener {
    private final CoordinateOffsetFoliaPlugin plugin;
    private final CoordinateOffsetCore core;
    private final WorldBorderObfuscator worldBorderObfuscator;

    BukkitEventListener(CoordinateOffsetFoliaPlugin plugin, CoordinateOffsetCore core, WorldBorderObfuscator worldBorderObfuscator) {
        this.plugin = plugin;
        this.core = core;
        this.worldBorderObfuscator = worldBorderObfuscator;
    }

    public void registerListeners() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(ServerLoadEvent event) {
        plugin.onAllPluginsEnabled();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        worldBorderObfuscator.tryUpdatePlayerBorders(event.getPlayer(), event.getPlayer().getLocation());

        FoliaOffsetPlayer player = new FoliaOffsetPlayer(event.getPlayer());
        core.getOffsetHolder().generateNextOffset(
            player,
            null,
            new FoliaLocation(event.getPlayer().getLocation()),
            OffsetProviderContext.ProvideReason.JOIN
        );
    }

    private final Map<UUID, Location> lastDeathLocation = new HashMap<>();
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        lastDeathLocation.put(event.getEntity().getUniqueId(), event.getEntity().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        OffsetProviderContext.ProvideReason reason;
        if (event.getRespawnReason() == PlayerRespawnEvent.RespawnReason.END_PORTAL) {
            reason = OffsetProviderContext.ProvideReason.WORLD_CHANGE;
        } else {
            reason = OffsetProviderContext.ProvideReason.DEATH_RESPAWN;
        }

        Location lastDeathLocation = this.lastDeathLocation.get(event.getPlayer().getUniqueId());
        OffsetPlayer player = new FoliaOffsetPlayer(event.getPlayer());
        core.getOffsetHolder().generateNextOffset(
            player,
            lastDeathLocation == null ? null : new FoliaLocation(lastDeathLocation),
            new FoliaLocation(event.getRespawnLocation()),
            reason
        );
    }

    private static final Set<String> IGNORED_TELEPORT_CAUSES = Set.of(

        "DISMOUNT",
        "EXIT_BED",
        "UNKNOWN"
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        FoliaOffsetPlayer offsetPlayer = new FoliaOffsetPlayer(event.getPlayer());
        OffsetProviderContext.ProvideReason reason;

        if (IGNORED_TELEPORT_CAUSES.contains(event.getCause().name())) {
            if (core.isDebugEnabled()) {
                core.getLogger().info("Ignoring teleport event for " + event.getPlayer().getName() +
                    " due to ignored cause: " + event.getCause().name());
            }
            return;
        }

        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            reason = OffsetProviderContext.ProvideReason.WORLD_CHANGE;
        } else {
            reason = OffsetProviderContext.ProvideReason.TELEPORT;
        }

        OffsetChange result = core.getOffsetHolder().generateNextOffset(
            offsetPlayer, new FoliaLocation(event.getFrom()), new FoliaLocation(event.getTo()), reason);

        if (result.offsetChanged() && reason == OffsetProviderContext.ProvideReason.TELEPORT) {

            int viewDistanceChunks = Math.max(
                event.getPlayer().getViewDistance(),
                event.getPlayer().getSendViewDistance()
            ) + 2;
            double viewDistanceBlocks = (double) viewDistanceChunks * 16;
            double tpDistanceSq = event.getFrom().distanceSquared(event.getTo());
            if (tpDistanceSq < viewDistanceBlocks * viewDistanceBlocks) {
                List<Chunk> chunksClosestFirst =
                    ((FoliaOffsetSwapper) core.getAdapter().getOffsetSwapper())
                        .sendUnloadAllSentChunksPackets(event.getPlayer());

                Player teleporting = event.getPlayer();

                FoliaSupport.runOnEntityLater(plugin, teleporting, () -> {
                    if (!teleporting.isOnline()) return;

                    PacketEvents.getAPI().getPlayerManager().sendPacket(teleporting,
                        new WrapperPlayServerUpdateViewPosition(
                            teleporting.getLocation().getChunk().getX(),
                            teleporting.getLocation().getChunk().getZ()));

                    ((FoliaOffsetSwapper) core.getAdapter().getOffsetSwapper())
                        .refreshChunksAndEntities(teleporting, chunksClosestFirst);
                }, 1L);
            }
        }

        worldBorderObfuscator.tryUpdatePlayerBorders(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        worldBorderObfuscator.tryUpdatePlayerBorders(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        for (OffsetProvider provider : core.getProviderConfig().getAllOffsetProviderConfigs().values()) {
            try {
                provider.onPlayerQuit(new FoliaOffsetPlayer(event.getPlayer()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
