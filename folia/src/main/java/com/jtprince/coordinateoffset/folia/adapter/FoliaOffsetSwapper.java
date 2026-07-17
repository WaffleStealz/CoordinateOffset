package com.jtprince.coordinateoffset.folia.adapter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateViewPosition;
import com.jtprince.coordinateoffset.CoordinateOffsetCore;
import com.jtprince.coordinateoffset.adapter.OffsetPlayer;
import com.jtprince.coordinateoffset.adapter.OffsetSwapper;
import com.jtprince.coordinateoffset.folia.CoordinateOffsetFoliaPlugin;
import com.jtprince.coordinateoffset.folia.FoliaSupport;
import io.papermc.paper.FeatureHooks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@NullMarked
public class FoliaOffsetSwapper implements OffsetSwapper {
    private final CoordinateOffsetFoliaPlugin plugin;
    public FoliaOffsetSwapper(CoordinateOffsetFoliaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void forceOffsetSwap(OffsetPlayer offsetPlayer) {
        Player player = (Player) offsetPlayer.getPlatformPlayerObject();

        List<Chunk> chunksClosestFirst = sendUnloadAllSentChunksPackets(player);

        player.setPlayerProfile(player.getPlayerProfile());

        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
            new WrapperPlayServerUpdateViewPosition(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()));

        refreshChunksAndEntities(player, chunksClosestFirst);
    }

    public List<Chunk> getSentChunksClosestFirst(Player player) {
        int cx = player.getChunk().getX();
        int cz = player.getChunk().getZ();

        return player.getSentChunks().stream()
            .sorted(Comparator.comparing(c ->
                ((c.getX() - cx) * (c.getX() - cx) + (c.getZ() - cz) * (c.getZ() - cz))))
            .toList();
    }

    public List<Chunk> sendUnloadAllSentChunksPackets(Player player) {
        List<Chunk> chunksClosestFirst = getSentChunksClosestFirst(player);
        for (Chunk chunk : chunksClosestFirst.reversed()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerUnloadChunk(chunk.getX(), chunk.getZ()));
        }
        return chunksClosestFirst;
    }

    public void refreshChunksAndEntities(Player player, List<Chunk> chunks) {

        chunkRefreshTasks.put(player.getUniqueId(), new ChunkRefreshTask(
            Bukkit.getCurrentTick(),
            player.getWorld().getUID(),
            new ArrayDeque<>(chunks),
            player.getWorld().getEntities().stream()
                .filter(e -> e.getTrackedBy().contains(player))
                .map(Entity::getUniqueId)
                .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet))
        ));

        FoliaSupport.runOnEntityTimer(plugin, player, scheduledTask -> {
            if (!player.isOnline() || !processOneStep(player.getUniqueId())) {
                scheduledTask.cancel();
            }
        }, 1L, 1L);
    }

    private record ChunkRefreshTask(int startTick, UUID world, Queue<Chunk> chunksLeft, Set<UUID> entitiesLeft) {}
    private final Map<UUID , ChunkRefreshTask> chunkRefreshTasks = new ConcurrentHashMap<>();

    private boolean processOneStep(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        ChunkRefreshTask task = chunkRefreshTasks.get(playerUuid);
        if (player == null || task == null) {
            chunkRefreshTasks.remove(playerUuid);
            return false;
        }

        Chunk chunk = task.chunksLeft().poll();
        if (chunk == null) {
            for (UUID entityId : task.entitiesLeft()) {
                Entity entity = Bukkit.getEntity(entityId);
                if (entity == null) continue;
                player.hideEntity(plugin, entity);
                player.showEntity(plugin, entity);
            }
            if (CoordinateOffsetCore.get().isDebugEnabled()) {
                CoordinateOffsetCore.get().getLogger().info("Chunks refreshed in " +
                    (Bukkit.getCurrentTick() - task.startTick()) +
                    " ticks for " + playerUuid);
            }
            chunkRefreshTasks.remove(playerUuid);
            return false;
        }

        if (player.isChunkSent(chunk)) {
            refreshChunkForPlayer(player, chunk);
        }

        for (Entity entity : chunk.getEntities()) {
            if (task.entitiesLeft.contains(entity.getUniqueId()) && entity.getTrackedBy().contains(player)) {
                player.hideEntity(plugin, entity);
                player.showEntity(plugin, entity);
            }
            task.entitiesLeft.remove(entity.getUniqueId());
        }
        return true;
    }

    private int lastTickExceptionPrinted = 0;
    private void refreshChunkForPlayer(Player player, Chunk chunk) {
        try {

            FeatureHooks.sendChunkRefreshPackets(
                List.of(((CraftPlayer) player).getHandle()),
                (LevelChunk) ((CraftChunk) chunk).getHandle(ChunkStatus.FULL)
            );
        } catch (Exception e) {

            if (Bukkit.getCurrentTick() - lastTickExceptionPrinted > 10) {
                new RuntimeException("Failed to refresh " + chunk + " for " + player.getName() +
                    "; falling back on refreshing chunk for all players", e).printStackTrace();
                lastTickExceptionPrinted = Bukkit.getCurrentTick();
            }
            chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
        }
    }
}
