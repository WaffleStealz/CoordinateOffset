package com.jtprince.coordinateoffset.example;

import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.provider.OffsetProvider;
import com.jtprince.coordinateoffset.provider.OffsetProviderConfig;
import com.jtprince.coordinateoffset.provider.OffsetProviderContext;
import org.bukkit.World;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

public class ExampleOffsetProvider extends OffsetProvider {
    private final int myScaleSetting;
    @Nullable private final String myStringSetting;

    public ExampleOffsetProvider(String userDefinedProviderName, int myScaleSetting, @Nullable String myStringSetting) {

        super(userDefinedProviderName);
        this.myScaleSetting = myScaleSetting;
        this.myStringSetting = myStringSetting;
    }

    @Override
    public @NonNull Offset provideOffset(@NonNull OffsetProviderContext context) {

        World world = (World) context.playerLocation().getWorld();
        if (world.getEnvironment() == World.Environment.NETHER) {
            return Offset.scalable(16000, 16000);
        }

        if (context.player().hasPermission("example.offset.randomized")) {
            return Offset.random(10000);
        }

        org.bukkit.entity.Player player = (org.bukkit.entity.Player) context.player().getPlatformPlayerObject();
        if (player.hasSeenWinScreen()) {
            return Offset.ZERO;
        }

        return Offset.align(16001 * myScaleSetting, -15999 * myScaleSetting);
    }

    public static ExampleOffsetProvider deserialize(@NonNull OffsetProviderConfig config) throws IllegalArgumentException {

        Object scale = config.getConfigSection().get("myScaleSetting");
        if (!(scale instanceof Number scaleNum)) {
            throw new IllegalArgumentException("Provider \"" + config.getUserDefinedProviderName() +
                ": Required key myScaleSetting for ExampleOffsetProvider is missing or invalid.");
        }

        Object stringSettingObj = config.getConfigSection().get("myStringSetting");
        String stringSetting = null;
        if (stringSettingObj != null) {

            if (!(stringSettingObj instanceof String)) {
                throw new IllegalArgumentException("Provider \"" + config.getUserDefinedProviderName() +
                    "\": Optional key myStringSetting for ExampleOffsetProvider is not a string.");
            }
            stringSetting = (String) stringSettingObj;
        }

        return new ExampleOffsetProvider(config.getUserDefinedProviderName(), scaleNum.intValue(), stringSetting);
    }

    @Override
    public @NonNull SequencedMap<String, ?> serialize() {

        SequencedMap<String, Object> map = new LinkedHashMap<>();

        map.put("myScaleSetting", (long) myScaleSetting);
        if (myStringSetting != null) {
            map.put("myStringSetting", myStringSetting);
        }
        return map;
    }
}
