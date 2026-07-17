package com.jtprince.coordinateoffset.example;

import com.jtprince.coordinateoffset.FixedOffset;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.OffsetChange;
import com.jtprince.coordinateoffset.adapter.OffsetPlayer;
import com.jtprince.coordinateoffset.api.CoordinateOffset;
import com.jtprince.coordinateoffset.api.CoordinateOffsetAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class ExampleCoordinateOffsetAPIPlugin extends JavaPlugin implements Listener {

    @Nullable CoordinateOffsetAPI api;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        try {
            api = CoordinateOffset.api();
        } catch (NoClassDefFoundError e) {
            getLogger().warning("CoordinateOffset API not found. Proceeding without hooking into CoordinateOffset.");
        }

        if (api != null) {

            api.registerOffsetProviderClass("ExampleOffsetProvider", ExampleOffsetProvider::deserialize);

            boolean coVerbose = api.getConfig().getVerbose();
            boolean coPermBypass = api.getConfig().getBypassByPermission();
            getLogger().info("In CoordinateOffset config, verbose mode is " +
                (coVerbose ? "enabled" : "disabled") +
                " and permission bypass is " +
                (coPermBypass ? "enabled" : "disabled") +
                "."
            );
        }
    }

    @EventHandler
    public void onBlockPlaceLogOffset(BlockPlaceEvent event) {

        if (api == null) return;

        OffsetPlayer player = api.adaptPlayer(event.getPlayer());
        FixedOffset offset = api.getOffset(player);

        Location realBlockLocation = event.getBlock().getLocation();
        Location playerBlockLocation = offset.apply(realBlockLocation);

        getLogger().info(player.getName() + " placed " + event.getBlockPlaced().getType().key() + " at:");
        getLogger().info("    Real: " + formatBlockLocation(realBlockLocation));
        getLogger().info("  Player: " + formatBlockLocation(playerBlockLocation));
    }

    @EventHandler
    public void onConsumePoisonPotatoRegenerateOffset(PlayerItemConsumeEvent event) {

        if (api == null) return;

        if (event.getItem().getType() != Material.POISONOUS_POTATO) return;

        OffsetPlayer player = api.adaptPlayer(event.getPlayer());
        OffsetChange result = api.regenerateOffset(player);

        if (result.offsetChanged()) {
            getLogger().info(player.getName() + " ate a poisonous potato and changed their offset from " +
                Objects.requireNonNull(result.previousOffsetData()).offset() +
                " to " + result.newOffsetData().offset());
        } else {
            getLogger().info(player.getName() + "'s offset was unchanged by eating a poisonous potato.");
        }
    }

    @EventHandler
    public void onConsumeGoldenCarrotSetZeroOffset(PlayerItemConsumeEvent event) {

        if (api == null) return;

        if (event.getItem().getType() != Material.GOLDEN_CARROT) return;

        OffsetPlayer player = api.adaptPlayer(event.getPlayer());
        OffsetChange result = api.setOffset(player, Offset.ZERO);

        if (result.offsetChanged()) {
            getLogger().info(player.getName() + " ate a golden carrot and changed their offset from " +
                Objects.requireNonNull(result.previousOffsetData()).offset() +
                " to " + result.newOffsetData().offset());
        } else {
            getLogger().info(player.getName() + "'s offset was unchanged by eating a golden carrot.");
        }
    }

    private String formatBlockLocation(Location l) {
        return String.format("(%.0f, %.0f, %.0f)", l.getX(), l.getY(), l.getZ());
    }
}
