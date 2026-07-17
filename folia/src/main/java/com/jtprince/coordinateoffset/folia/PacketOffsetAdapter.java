package com.jtprince.coordinateoffset.folia;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.jtprince.coordinateoffset.CoordinateOffsetCore;
import com.jtprince.coordinateoffset.FixedOffset;
import com.jtprince.coordinateoffset.offsetter.OffsetterRegistry;
import com.jtprince.coordinateoffset.folia.adapter.FoliaOffsetPlayer;
import com.jtprince.coordinateoffset.provider.OffsetProvider;
import com.jtprince.coordinateoffset.util.PartialStacktraceLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

class PacketOffsetAdapter {
    private final CoordinateOffsetCore core;
    private final CoordinateOffsetFoliaPlugin coPlugin;
    private final Logger logger;
    private final PartialStacktraceLogger partialStacktraceLogger;
    @Nullable private Listener listener;

    private final long stacktraceRateLimitMs = 2500;

    PacketOffsetAdapter(CoordinateOffsetFoliaPlugin plugin) {
        this.core = CoordinateOffsetCore.get();
        this.coPlugin = plugin;
        this.logger = plugin.getLogger();

        this.partialStacktraceLogger = new PartialStacktraceLogger(logger);

        FoliaSupport.runAsyncTimer(coPlugin, () ->
            this.partialStacktraceLogger.flushRateLimits(stacktraceRateLimitMs), stacktraceRateLimitMs);
    }

    void registerAdapters() {
        listener = new Listener();
        PacketEvents.getAPI().getEventManager().registerListener(listener);
    }

    void onDisable() {
        PacketEvents.getAPI().getEventManager().unregisterListener(listener);
        listener = null;
        this.partialStacktraceLogger.flushRateLimits(0);
    }

    private class Listener extends PacketListenerAbstract {
        Listener() {
            super(PacketListenerPriority.HIGH);
        }

        private static final Set<PacketType.Play.Server> PACKETS_WORLD_BORDER = Set.of(

                PacketType.Play.Server.INITIALIZE_WORLD_BORDER,
                PacketType.Play.Server.WORLD_BORDER_CENTER,
                PacketType.Play.Server.WORLD_BORDER_LERP_SIZE,
                PacketType.Play.Server.WORLD_BORDER_SIZE,
                PacketType.Play.Server.WORLD_BORDER_WARNING_DELAY,
                PacketType.Play.Server.WORLD_BORDER_WARNING_REACH
        );

        @Override
        public void onPacketSend(PacketSendEvent event) {

            if (event.getPlayer() == null || !(event.getPacketType() instanceof PacketType.Play.Server)) return;

            try {
                if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
                    core.getOffsetHolder().swapInNextOffset(new FoliaOffsetPlayer(event.getPlayer()));
                }

                FixedOffset offset;
                if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {

                    try {
                        offset = core.getOffsetHolder().waitForJoiningOffset(event.getUser().getUUID(), 5000);
                    } catch (TimeoutException e) {
                        logger.severe("Timed out waiting for an offset to generate for " + event.getUser().getName() + ".");
                        logger.severe("This is a bug in CoordinateOffset. Please report it.");
                        e.printStackTrace();
                        event.setCancelled(true);
                        return;
                    }
                } else if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
                    offset = core.getOffsetHolder().getNextOffset(new FoliaOffsetPlayer(event.getPlayer())).offset();
                } else {
                    offset = core.getOffsetHolder().getOffset(new FoliaOffsetPlayer(event.getPlayer())).offset();
                }

                if (offset.isZero()) return;

                if (core.getConfig().getObfuscateDebugPropertySubscriptions()) {
                    if (event.getPacketType().getName().startsWith("DEBUG")) {
                        event.setCancelled(true);
                        return;
                    }
                }

                if (PACKETS_WORLD_BORDER.contains(event.getPacketType()) && coPlugin.getWorldBorderObfuscator() != null) {
                    coPlugin.getWorldBorderObfuscator().translate(event, event.getPlayer());
                    return;
                }

                OffsetterRegistry.attemptToOffset(event, offset);
            } catch (Exception e) {
                boolean logged = partialStacktraceLogger.logStacktraceRateLimited(logger,
                    "Failed to apply offset for outgoing packet " +
                        event.getPacketType().getName() + " to " + event.getUser().getName(),
                    e, stacktraceRateLimitMs, event.getUser().getName());
            }
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {

            if (event.getPlayer() == null || !(event.getPacketType() instanceof PacketType.Play.Client)) return;

            try {
                FixedOffset offset = core.getOffsetHolder().getOffset(new FoliaOffsetPlayer(event.getPlayer())).offset();
                if (offset.isZero()) return;

                OffsetterRegistry.attemptToUnOffset(event, offset);
            } catch (Exception e) {
                boolean logged = partialStacktraceLogger.logStacktraceRateLimited(logger,
                    "Failed to reverse offset for incoming packet " +
                        event.getPacketType().getName() + " from " + event.getUser().getName(),
                    e, stacktraceRateLimitMs, event.getUser().getName());
            }
        }

        @Override
        public void onUserDisconnect(UserDisconnectEvent event) {
            UUID playerUuid = event.getUser().getUUID();
            if (playerUuid == null) return;

            Player onlinePlayer = Bukkit.getPlayer(playerUuid);
            if (onlinePlayer != null
                    && PacketEvents.getAPI().getPlayerManager().getUser(onlinePlayer) != event.getUser()) {

                return;
            }

            core.getOffsetHolder().remove(playerUuid);
            for (OffsetProvider provider : core.getProviderConfig().getAllOffsetProviderConfigs().values()) {
                try {
                    provider.onPlayerDisconnect(playerUuid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (coPlugin.getWorldBorderObfuscator() != null) {
                coPlugin.getWorldBorderObfuscator().onPlayerDisconnect(playerUuid);
            }
            OffsetterRegistry.onUserDisconnect(event.getUser());
        }
    }
}
