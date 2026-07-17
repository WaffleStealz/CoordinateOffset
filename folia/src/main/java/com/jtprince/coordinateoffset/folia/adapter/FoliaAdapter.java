package com.jtprince.coordinateoffset.folia.adapter;

import com.jtprince.coordinateoffset.adapter.CoordinateOffsetAdapter;
import com.jtprince.coordinateoffset.folia.CoordinateOffsetFoliaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

@NullMarked
public class FoliaAdapter implements CoordinateOffsetAdapter {
    private final CoordinateOffsetFoliaPlugin plugin;
    private final FoliaPlayerOffsetPersistence offsetPersistence;
    private final FoliaOffsetSwapper offsetSwapper;

    public FoliaAdapter(CoordinateOffsetFoliaPlugin plugin) {
        this.plugin = plugin;
        offsetPersistence = new FoliaPlayerOffsetPersistence(plugin);
        offsetSwapper = new FoliaOffsetSwapper(plugin);
    }

    @Override
    public Path getConfigDir() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public Logger getLogger() {
        return plugin.getLogger();
    }

    @Override
    public @Nullable FoliaOffsetPlayer getPlayer(UUID playerUuid) {
        Player bukkitPlayer = Bukkit.getPlayer(playerUuid);
        if (bukkitPlayer == null) {
            return null;
        }
        return new FoliaOffsetPlayer(bukkitPlayer);
    }

    @Override
    public FoliaOffsetPlayer adaptPlayer(Object platformPlayerObject) throws ClassCastException {
        if (!(platformPlayerObject instanceof Player bukkitPlayer)) {
            throw new ClassCastException("Object \"" + platformPlayerObject + "\" of class " +
                platformPlayerObject.getClass().getName() + " is not a valid Bukkit Player.");
        }
        return new FoliaOffsetPlayer(bukkitPlayer);
    }

    @Override
    public FoliaLocation adaptLocation(Object platformLocationObject) throws ClassCastException {
        if (!(platformLocationObject instanceof Location bukkitLocation)) {
            throw new ClassCastException("Object \"" + platformLocationObject + "\" of class " +
                platformLocationObject.getClass().getName() + " is not a valid Bukkit Location.");
        }
        return new FoliaLocation(bukkitLocation);
    }

    @Override
    public FoliaPlayerOffsetPersistence getPersistenceAdapter() {
        return offsetPersistence;
    }

    @Override
    public FoliaOffsetSwapper getOffsetSwapper() {
        return offsetSwapper;
    }

    private static boolean printedDHSupportWarning = false;
    @Override
    public int getMinimumOffsetMultiple() {
        if (Bukkit.getPluginManager().getPlugin("DHSupport") != null) {
            if (!printedDHSupportWarning) {
                getLogger().info("DHSupport plugin is detected. Offset X and Z values must be divisible by 64 " +
                    "blocks for offsets to be compatible with Distant Horizons LODs.");
                printedDHSupportWarning = true;
            }
            return 64;
        }

        return 16;
    }

    @Override
    public void assertMainThread(String methodName) throws IllegalStateException {
    }

    @Override
    public void shutdown() {
        Bukkit.getPluginManager().disablePlugin(plugin);
    }
}
