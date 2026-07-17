package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.adapter.OffsetPlayer;
import com.jtprince.coordinateoffset.api.CoordinateOffsetAPI;
import com.jtprince.coordinateoffset.command.OffsetSetCommand;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.Polymorphic;
import org.jspecify.annotations.NullMarked;

import java.util.SequencedMap;
import java.util.UUID;

@Polymorphic
@Configuration
@NullMarked
public abstract class OffsetProvider {
    public static int OFFSET_MAX = 30_000_000;

    public final String name;

    public OffsetProvider(String userDefinedProviderName) {
        this.name = userDefinedProviderName;
    }

    public abstract Offset provideOffset(OffsetProviderContext context);

    public void onPlayerQuit(OffsetPlayer player) {}

    public void onPlayerDisconnect(UUID playerUuid) {}

    public void onOffsetSetByCommand(OffsetSetCommand command, OffsetPlayer target) {

        command.warnOffsetIsNotPersistentInProvider(this, target);
    }

    public abstract SequencedMap<String, ?> serialize();
}
