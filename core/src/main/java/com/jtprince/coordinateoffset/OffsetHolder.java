package com.jtprince.coordinateoffset;

import com.jtprince.coordinateoffset.adapter.OffsetLocation;
import com.jtprince.coordinateoffset.adapter.OffsetPlayer;
import com.jtprince.coordinateoffset.command.OffsetSetCommand;
import com.jtprince.coordinateoffset.provider.OffsetProvider;
import com.jtprince.coordinateoffset.provider.OffsetProviderContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@NullMarked
public class OffsetHolder {
    private final CoordinateOffsetCore core;
    private final OffsetFactory offsetFactory;
    OffsetHolder(CoordinateOffsetCore core) {
        this.core = core;
        this.offsetFactory = new OffsetFactory(core);
    }

    private record PlayerOffsetData(
        OffsetData previousOffset,
        OffsetData currentOffset,
        @Nullable OffsetData nextOffset
    ) {}
    private final ConcurrentHashMap<UUID, PlayerOffsetData> playerOffsetData = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> pendingDataLocks = new ConcurrentHashMap<>();

    public OffsetData getOffset(OffsetPlayer player) {
        PlayerOffsetData data = playerOffsetData.get(player.getUuid());
        if (data == null) {
            throw new NoSuchElementException("Player " + player.getName() + " has no offset data!");
        }
        return data.currentOffset;
    }

    public OffsetData getNextOffset(OffsetPlayer player) {
        PlayerOffsetData data = playerOffsetData.get(player.getUuid());
        if (data == null) {
            throw new NoSuchElementException("Player " + player.getName() + " has no offset data!");
        }
        return (data.nextOffset == null) ? data.currentOffset : data.nextOffset;
    }

    public FixedOffset waitForJoiningOffset(UUID playerUuid, int timeoutMillis) throws TimeoutException {
        PlayerOffsetData data = playerOffsetData.get(playerUuid);
        if (data == null && timeoutMillis > 0) {

            Object pendingOffsetDataLock = pendingDataLocks.computeIfAbsent(playerUuid, uuid -> new Object());
            long now = System.currentTimeMillis();
            long deadline = now + timeoutMillis;
            synchronized (pendingOffsetDataLock) {
                while (data == null && (now = System.currentTimeMillis()) < deadline) {
                    try {
                        pendingOffsetDataLock.wait( deadline - now);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    data = playerOffsetData.get(playerUuid);
                }
            }
        }
        if (data == null) {
            throw new TimeoutException("Player " + playerUuid + " has no offset data!");
        }
        return data.currentOffset.offset();
    }

    public OffsetChange generateNextOffset(
        OffsetPlayer player,
        @Nullable OffsetLocation previousLocation,
        OffsetLocation nextLocation,
        OffsetProviderContext.ProvideReason reason
    ) {
        PlayerOffsetData data = playerOffsetData.get(player.getUuid());
        OffsetProviderContext context = new OffsetProviderContext(
            player, previousLocation, nextLocation, data == null ? null : data.currentOffset.offset(), reason);
        OffsetData creation = offsetFactory.createOffset(context);
        return setNextOffset(context.player().getUuid(), creation);
    }

    public OffsetChange setNextOffsetByCommand(
        OffsetPlayer player,
        OffsetSetCommand setCommand
    ) {
        PlayerOffsetData playerCache = playerOffsetData.get(player.getUuid());
        OffsetData currentOffsetData = (playerCache == null ? null : playerCache.currentOffset);
        Offset currentOffset = (currentOffsetData == null ? null : currentOffsetData.offset());

        OffsetProviderContext context = new OffsetProviderContext(
            player, player.getLocation(), player.getLocation(), currentOffset,
            OffsetProviderContext.ProvideReason.COMMAND_SET);

        OffsetProvider affectedProvider = getAffectedProvider(currentOffsetData);
        if (affectedProvider != null) {
            try {
                affectedProvider.onOffsetSetByCommand(setCommand, player);
            } catch (Exception e) {
                new RuntimeException("Error informing affected offset provider " + affectedProvider.name +
                    " of offset set by command.", e).printStackTrace();
            }
        }

        OffsetData creation = offsetFactory.createSpecificOffset(
            setCommand.getOffset(), new OffsetData.Source.SetCommand(setCommand, affectedProvider), context);
        return setNextOffset(player.getUuid(), creation);
    }

    public OffsetChange setNextOffsetByPlugin(
        OffsetPlayer player,
        Offset newOffset
    ) {
        PlayerOffsetData playerCache = playerOffsetData.get(player.getUuid());
        OffsetData currentOffsetData = (playerCache == null ? null : playerCache.currentOffset);
        Offset currentOffset = (currentOffsetData == null ? null : currentOffsetData.offset());

        OffsetProviderContext context = new OffsetProviderContext(
            player, player.getLocation(), player.getLocation(), currentOffset,
            OffsetProviderContext.ProvideReason.PLUGIN_SET);

        return setNextOffset(player.getUuid(),
            offsetFactory.createSpecificOffset(newOffset, new OffsetData.Source.PluginSet(), context));
    }

    private OffsetChange setNextOffset(UUID playerUuid, OffsetData newOffset) {
        PlayerOffsetData d = playerOffsetData.compute(playerUuid, (uuid, existingOffsetData) -> {
            if (existingOffsetData == null) {
                debugLog("Generate first: " + newOffset + ", " + newOffset + ", " + null);
                log(newOffset);
                return new PlayerOffsetData(
                    newOffset,
                    newOffset,
                    null
                );
            }

            if (existingOffsetData.currentOffset.offset().equals(newOffset.offset())) {

                debugLog("Unchanged offset:" + existingOffsetData.previousOffset + " , " + newOffset + ", " + existingOffsetData.nextOffset);
                return new PlayerOffsetData(
                    existingOffsetData.previousOffset,
                    newOffset,
                    existingOffsetData.nextOffset
                );
            }

            debugLog("Generate next: " + existingOffsetData.previousOffset + ", " + existingOffsetData.currentOffset + ", " + newOffset);
            return new PlayerOffsetData(
                existingOffsetData.previousOffset,
                existingOffsetData.currentOffset,
                newOffset
            );
        });

        Object pendingOffsetDataLock = pendingDataLocks.computeIfAbsent(playerUuid, uuid -> new Object());
        synchronized (pendingOffsetDataLock) {
            pendingOffsetDataLock.notifyAll();
        }

        OffsetChange offsetChange = new OffsetChange(
            d.currentOffset,
            (d.nextOffset == null ? d.currentOffset : d.nextOffset)
        );
        if (offsetChange.offsetChanged()) {
            log(offsetChange.newOffsetData());
        }

        return offsetChange;
    }

    public void swapInNextOffset(OffsetPlayer player) {
        playerOffsetData.computeIfPresent(player.getUuid(), (uuid, existingOffsetData) -> {
            if (existingOffsetData.nextOffset == null) {

                return existingOffsetData;
            }
            debugLog("Swap in next: " +
                existingOffsetData.currentOffset + ", " +
                existingOffsetData.nextOffset + ", null");
            return new PlayerOffsetData(
                existingOffsetData.currentOffset,
                existingOffsetData.nextOffset,
                null
            );
        });
    }

    public void remove(UUID uuid) {
        playerOffsetData.remove(uuid);
        pendingDataLocks.remove(uuid);
    }

    private void log(OffsetData offset) {
        if (!core.getConfig().getVerbose()) return;

        StringBuilder s = new StringBuilder();
        s.append("Using offset ");
        s.append(offset.offset());
        s.append(" from ");
        switch (offset.source()) {
            case OffsetData.Source.PermissionBypass ignored -> s.append("permission bypass");
            case OffsetData.Source.BedrockBypass ignored -> { return;  }
            case OffsetData.Source.Provider p -> {
                s.append("provider \"").append(p.provider().name).append("\"");
                if (p.overrideRuleIndex() != null) {
                    s.append(" (provider override rule #").append(p.overrideRuleIndex()).append(")");
                } else {
                    s.append(" (default provider)");
                }
            }
            case OffsetData.Source.SetCommand p -> s.append("command by ").append(p.command().getCommandSender().name());
            case OffsetData.Source.PluginSet ignored -> s.append("external plugin");
        }
        s.append(" for player ");
        s.append(offset.context().player().getName());
        s.append(" in world \"");
        s.append(offset.context().playerLocation().getWorld().getName());
        s.append("\"");
        s.append(switch (offset.context().reason()) {
            case JOIN -> " (player joined)";
            case DEATH_RESPAWN -> " (player respawned)";
            case WORLD_CHANGE -> " (player changed worlds)";
            case TELEPORT -> " (player teleported)";
            case COMMAND_REGENERATE -> " (regenerated by command)";
            case COMMAND_SET, PLUGIN_SET -> "";
            case PLUGIN_REGENERATE -> " (regenerated by external plugin)";
        });

        core.getLogger().info(s.toString());
    }

    private void debugLog(String message) {
        if (core.isDebugEnabled()) {
            core.getLogger().info("[Debug] " + message);
        }
    }

    private @Nullable OffsetProvider getAffectedProvider(@Nullable OffsetData currentOffset) {
        if (currentOffset == null) return null;
        OffsetProvider affectedProvider = switch (currentOffset.source()) {
            case OffsetData.Source.BedrockBypass ignored -> null;
            case OffsetData.Source.PermissionBypass ignored -> null;
            case OffsetData.Source.Provider provider -> provider.provider();
            case OffsetData.Source.SetCommand setCommand -> setCommand.affectedProvider();
            case OffsetData.Source.PluginSet ignored -> null;
        };
        if (affectedProvider == null) return null;

        OffsetProvider reloadedProvider =
            core.getProviderConfig().getAllOffsetProviderConfigs().get(affectedProvider.name);
        if (reloadedProvider != null) {
            return reloadedProvider;
        }

        return affectedProvider;
    }
}
