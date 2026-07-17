package com.jtprince.coordinateoffset.folia;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class FoliaSupport {

    private static final boolean FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
        }
        FOLIA = folia;
    }

    private FoliaSupport() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static boolean isSafeEntityThread(Entity entity) {
        if (FOLIA) {

            return true;
        }
        return Bukkit.isPrimaryThread();
    }

    public static void runOnEntity(Plugin plugin, Entity entity, Runnable task) {
        entity.getScheduler().run(plugin, scheduled -> task.run(), null);
    }

    public static void runOnEntityLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        entity.getScheduler().runDelayed(plugin, scheduled -> task.run(), null, Math.max(1L, delayTicks));
    }

    public static void runAsyncTimer(Plugin plugin, Runnable task, long periodMs) {
        long period = Math.max(1L, periodMs);
        Bukkit.getAsyncScheduler().runAtFixedRate(
            plugin,
            scheduled -> task.run(),
            period,
            period,
            TimeUnit.MILLISECONDS
        );
    }

    public static void runOnEntityTimer(Plugin plugin, Entity entity, Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask> task,
                                        long delayTicks, long periodTicks) {
        entity.getScheduler().runAtFixedRate(plugin, task, null, Math.max(1L, delayTicks), Math.max(1L, periodTicks));
    }
}
