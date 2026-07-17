package com.jtprince.coordinateoffset.api;

import com.jtprince.coordinateoffset.FixedOffset;
import com.jtprince.coordinateoffset.Offset;
import com.jtprince.coordinateoffset.OffsetChange;
import com.jtprince.coordinateoffset.OffsetData;
import com.jtprince.coordinateoffset.adapter.OffsetLocation;
import com.jtprince.coordinateoffset.adapter.OffsetPlayer;
import com.jtprince.coordinateoffset.config.CoordinateOffsetConfig;
import com.jtprince.coordinateoffset.config.CoordinateOffsetProviderConfig;
import com.jtprince.coordinateoffset.provider.OffsetProvider;
import com.jtprince.coordinateoffset.provider.OffsetProviderConfig;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;

@NullMarked
public interface CoordinateOffsetAPI {

    FixedOffset getOffset(OffsetPlayer player);

    OffsetData getOffsetData(OffsetPlayer player);

    OffsetChange regenerateOffset(OffsetPlayer player);

    OffsetChange setOffset(OffsetPlayer player, Offset offset);

    @Nullable OffsetPlayer getPlayer(UUID playerUuid);

    OffsetPlayer adaptPlayer(Object platformPlayerObject) throws ClassCastException;

    OffsetLocation adaptLocation(Object platformLocationObject) throws ClassCastException;

    CoordinateOffsetConfig getConfig();

    CoordinateOffsetProviderConfig getProviderConfig();

    void registerOffsetProviderClass(
        String className,
        Function<OffsetProviderConfig, OffsetProvider> deserializeFunction
    );
}
