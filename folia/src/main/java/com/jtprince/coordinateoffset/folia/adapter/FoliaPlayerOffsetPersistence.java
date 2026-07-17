package com.jtprince.coordinateoffset.folia.adapter;

import com.jeff_media.morepersistentdatatypes.DataType;
import com.jeff_media.morepersistentdatatypes.datatypes.GenericDataType;
import com.jtprince.coordinateoffset.CoordinateOffsetCore;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.ScalableOffset;
import com.jtprince.coordinateoffset.adapter.OffsetPersistenceAdapter;
import com.jtprince.coordinateoffset.adapter.OffsetPlayer;
import com.jtprince.coordinateoffset.folia.CoordinateOffsetFoliaPlugin;
import com.jtprince.coordinateoffset.provider.util.RegenerateConfig;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

@NullMarked
public class FoliaPlayerOffsetPersistence implements OffsetPersistenceAdapter {
    private static final PersistentDataType<int[], ScalableOffset> PDT_OFFSET =
        new GenericDataType<>(DataType.INTEGER_ARRAY.getPrimitiveType(), ScalableOffset.class,
            FoliaPlayerOffsetPersistence::fromPdt, FoliaPlayerOffsetPersistence::toPdt);

    private final CoordinateOffsetFoliaPlugin plugin;
    public FoliaPlayerOffsetPersistence(CoordinateOffsetFoliaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable ScalableOffset get(OffsetPlayer player, Key persistenceKey) {
        if (!(player instanceof FoliaOffsetPlayer foliaPlayer)) {
            throw new IllegalArgumentException("Player must be an instance of FoliaOffsetPlayer");
        }

        PersistentDataContainer pdc = foliaPlayer.getPlayer().getPersistentDataContainer();
        if (pdc.has(persistenceKeyToBukkitKey(persistenceKey), PDT_OFFSET)) {
            return pdc.get(persistenceKeyToBukkitKey(persistenceKey), PDT_OFFSET);
        } else {

            ScalableOffset recovered = preV6.recover(foliaPlayer.getPlayer(), persistenceKey);
            if (recovered != null) {
                put(foliaPlayer, persistenceKey, recovered);
            }
            return recovered;
        }
    }

    @Override
    public void put(OffsetPlayer player, Key persistenceKey, ScalableOffset offset) {
        if (!(player instanceof FoliaOffsetPlayer foliaPlayer)) {
            throw new IllegalArgumentException("Player must be an instance of FoliaOffsetPlayer");
        }

        foliaPlayer.getPlayer().getPersistentDataContainer().set(persistenceKeyToBukkitKey(persistenceKey), PDT_OFFSET, offset);
    }

    @Override
    public void clear(UUID playerUuid, Key persistenceKey) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
            plugin.getLogger().warning("Failed to clear persistent offset for offline player " + offlinePlayer.getName() + " (" + playerUuid + ")");
            return;
        }

        player.getPersistentDataContainer().remove(persistenceKeyToBukkitKey(persistenceKey));
        preV6.clearOldPersistence(player, persistenceKey);
    }

    private static ScalableOffset fromPdt(int[] arr) {
        return Offset.scalable(arr[0], arr[1]);
    }

    private static int[] toPdt(ScalableOffset offset) {
        return new int[] { offset.x(), offset.z() };
    }

    private NamespacedKey persistenceKeyToBukkitKey(Key persistenceKey) {

        return new NamespacedKey(plugin, persistenceKey.getPersistenceKey());
    }

    private final PreV6 preV6 = new PreV6();
    private class PreV6 {
        private static final PersistentDataType<PersistentDataContainer, Map<String, ScalableOffset>> PDT_WORLD_OFFSET_CONTAINER =
            DataType.asMap(DataType.STRING, PDT_OFFSET);
        private static final String OLD_KEY_PREFIX = "random-persistence.";
        private static final String MIGRATED_KEY_SUFFIX = ".old-migrated-v6";

        @Nullable ScalableOffset recover(Player player, Key persistenceKey) {
            try {
                String key = null;
                NamespacedKey fullKey = null;
                Map<String , ScalableOffset> foundData = null;

                if (persistenceKey.persistenceKeyOverride() != null) {
                    key = OLD_KEY_PREFIX + persistenceKey.persistenceKeyOverride();
                    fullKey = new NamespacedKey(plugin, key);
                    foundData = player.getPersistentDataContainer().get(fullKey, PDT_WORLD_OFFSET_CONTAINER);
                }

                if (foundData == null) {
                    key = OLD_KEY_PREFIX + RegenerateConfig.LEGACY_DEFAULT_PERSISTENCE_KEY;
                    fullKey = new NamespacedKey(plugin, key);
                    foundData = player.getPersistentDataContainer().get(fullKey, PDT_WORLD_OFFSET_CONTAINER);
                }

                if (foundData == null) return null;

                ScalableOffset offset = find(player, foundData);
                if (offset != null) {

                    NamespacedKey oldKey = new NamespacedKey(plugin, key + MIGRATED_KEY_SUFFIX);
                    player.getPersistentDataContainer().set(oldKey, PDT_WORLD_OFFSET_CONTAINER, foundData);
                    player.getPersistentDataContainer().remove(fullKey);
                }
                return offset;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to recover old persistent offset for " + player.getName());
                plugin.getLogger().warning("This is a bug, please report it to the developer.");
                e.printStackTrace();
                return null;
            }
        }

        private @Nullable ScalableOffset find(Player player, Map<String , ScalableOffset> map) {

            for (World w : Bukkit.getWorlds()) {
                if (w.getCoordinateScale() == 1 && map.containsKey(w.getName())) {
                    return map.get(w.getName());
                }
            }

            for (World w : Bukkit.getWorlds()) {
                if (map.containsKey(w.getName())) {

                    ScalableOffset saved = map.get(w.getName());
                    ScalableOffset scaledUp = Offset.scalable(
                        (int) (saved.x() * w.getCoordinateScale()),
                        (int) (saved.z() * w.getCoordinateScale())
                    );
                    CoordinateOffsetCore.get().getLogger().warning("Migrating old format of a persistent random offset for "
                        + player.getName() + " from world " + w.getName() + " - this may not be accurate (scaling it by "
                        + w.getCoordinateScale() + " to make " + scaledUp + "). Manually change this with /offset set if this is "
                        + "not what you want.");
                    return scaledUp;
                }
            }

            for (Map.Entry<String, ScalableOffset> entry : map.entrySet()) {
                CoordinateOffsetCore.get().getLogger().warning("Migrating old format of a persistent random offset for "
                    + player.getName() + " from world " + entry.getKey() + " - this is almost certainly inaccurate. "
                    + "Taking " + entry.getValue() + ". Manually change this with /offset set if this is not what you want.");
                return entry.getValue();
            }

            return null;
        }

        private void clearOldPersistence(Player player, Key persistenceKey) {
            if (persistenceKey.persistenceKeyOverride() != null) {
                String key = OLD_KEY_PREFIX + persistenceKey.persistenceKeyOverride();
                NamespacedKey fullKey = new NamespacedKey(plugin, key);
                player.getPersistentDataContainer().remove(fullKey);
            }

            String key = OLD_KEY_PREFIX + RegenerateConfig.LEGACY_DEFAULT_PERSISTENCE_KEY;
            NamespacedKey fullKey = new NamespacedKey(plugin, key);
            player.getPersistentDataContainer().remove(fullKey);
        }
    }
}
