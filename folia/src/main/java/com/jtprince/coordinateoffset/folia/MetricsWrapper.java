package com.jtprince.coordinateoffset.folia;

import com.jtprince.coordinateoffset.CoordinateOffsetCore;
import com.jtprince.coordinateoffset.config.CoordinateOffsetConfigBase;
import com.jtprince.coordinateoffset.provider.CoreOffsetProvider;
import com.jtprince.coordinateoffset.provider.OffsetProvider;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.Map;

@NullMarked
public class MetricsWrapper {

    private static final int BSTATS_PLUGIN_METRICS_ID = 19988;

    public static void reportMetrics(CoordinateOffsetFoliaPlugin plugin) {
        Metrics metrics = new Metrics(plugin, BSTATS_PLUGIN_METRICS_ID);

        CoordinateOffsetCore core = CoordinateOffsetCore.get();

        metrics.addCustomChart(new DrilldownPie("default_offset_provider", () -> {
            Map<String, Map<String, Integer>> result = new HashMap<>();
            OffsetProvider defaultProvider = core.getProviderConfig().getDefaultOffsetProviderConfig();
            if (defaultProvider instanceof CoreOffsetProvider coreOffsetProvider) {

                result.put(coreOffsetProvider.getMetricsClassName(), Map.of(coreOffsetProvider.getMetricsDetails(), 1));
            } else {

                result.put("Custom Provider", Map.of("Unknown Offset Provider", 1));
            }
            return result;
        }));

        metrics.addCustomChart(new SimplePie("world_border_obfuscation", () ->
            enabledDisabledStr(core.getConfig().getObfuscateWorldBorder())));

        metrics.addCustomChart(new SimplePie("debug_packet_obfuscation", () ->
            enabledDisabledStr(core.getConfig().getObfuscateDebugPropertySubscriptions())));

        metrics.addCustomChart(new SimplePie("fix_collision", () -> {
            boolean enabled = false;
            StringBuilder sb = new StringBuilder();
            if (core.getConfig().getFixCollisionBamboo()) {
                enabled = true;
                sb.append("B");
            } else {
                sb.append("x");
            }
            if (core.getConfig().getFixCollisionDripstone()) {
                enabled = true;
                sb.append("D");
            } else {
                sb.append("x");
            }
            if (enabled) {
                return "enabled (" + sb + ")";
            } else {
                return "disabled";
            }
        }));

        metrics.addCustomChart(new SimplePie("verbose", () ->
            enabledDisabledStr(core.getConfig().getVerbose())));

        metrics.addCustomChart(new SimplePie("offset_provider_override_count", () ->
            String.valueOf(core.getProviderConfig().getOffsetProviderOverrides().size())));

        metrics.addCustomChart(new SimplePie("coord_scale_override_count", () ->
            String.valueOf(core.getConfig().getWorldCoordinateScaleOverrides().size())));

        metrics.addCustomChart(new SimplePie("offsets_are_multiples_of_blocks", () ->
            ((CoordinateOffsetConfigBase) core.getConfig()).offsetsAreMultiplesOfBlocks.getMetricsString()));

        metrics.addCustomChart(new SimplePie("dhsupport_version", () -> {
            Plugin dhs = Bukkit.getPluginManager().getPlugin("DHSupport");
            if (dhs == null) {
                return "none";
            } else {
                return dhs.getPluginMeta().getVersion();
            }
        }));
    }

    private static String enabledDisabledStr(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }
}
