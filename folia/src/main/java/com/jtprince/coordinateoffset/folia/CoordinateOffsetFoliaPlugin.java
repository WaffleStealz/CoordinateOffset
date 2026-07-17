package com.jtprince.coordinateoffset.folia;

import com.jtprince.coordinateoffset.CoordinateOffsetCore;
import com.jtprince.coordinateoffset.CoordinateOffsetPermission;
import com.jtprince.coordinateoffset.folia.adapter.FoliaAdapter;
import com.jtprince.coordinateoffset.folia.lib.org.geysermc.hurricane.CollisionFix;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.stream.Collectors;

@NullMarked
public final class CoordinateOffsetFoliaPlugin extends JavaPlugin {
    private static @Nullable CoordinateOffsetFoliaPlugin instance;
    private @Nullable FoliaAdapter adapter;

    private @Nullable CoordinateOffsetCore core;
    private @Nullable WorldBorderObfuscator worldBorderObfuscator;
    private @Nullable PacketOffsetAdapter packetOffsetAdapter;
    private @Nullable CollisionFix collisionFix;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("CoordinateOffset Folia fork enabled.");

        adapter = new FoliaAdapter(this);
        core = CoordinateOffsetCore.bootstrap(adapter);

        worldBorderObfuscator = new WorldBorderObfuscator(this);

        new BukkitEventListener(this, core, worldBorderObfuscator).registerListeners();

        new FoliaOffsetCommand(this, core).registerCommands();

        packetOffsetAdapter = new PacketOffsetAdapter(this);
        packetOffsetAdapter.registerAdapters();

        if (core.isDebugEnabled()) {
            new PacketEventSequencer(this).install();
        }

        for (CoordinateOffsetPermission p : CoordinateOffsetPermission.values()) {
            Bukkit.getPluginManager().addPermission(new Permission(p.node, p.description, PermissionDefault.OP,
                p.getChildren().stream().collect(Collectors.toMap(p1 -> p1.node, p1 -> true))));
        }

        if (core.getConfig().getFixCollisionBamboo() || core.getConfig().getFixCollisionDripstone()) {
            try {
                collisionFix = new CollisionFix(this, core.getConfig().getFixCollisionBamboo(), core.getConfig().getFixCollisionDripstone());
            } catch (Exception e) {
                getLogger().severe("Failed to enable bamboo/dripstone collision fix: " + e.getMessage());
                if (core.getConfig().getVerbose()) {

                    e.printStackTrace();
                }
            }
        }
    }

    void onAllPluginsEnabled() {
        CoordinateOffsetCore.get().signalCompletedLoading();

        if (this.isEnabled()) {
            MetricsWrapper.reportMetrics(this);
        }
    }

    @Override
    public void onDisable() {
        if (packetOffsetAdapter != null) {
            packetOffsetAdapter.onDisable();
            packetOffsetAdapter = null;
        }
    }

    @SuppressWarnings("unused")
    public static @Nullable CoordinateOffsetFoliaPlugin getInstance() {
        return instance;
    }

    @Nullable WorldBorderObfuscator getWorldBorderObfuscator() {
        return worldBorderObfuscator;
    }
}
