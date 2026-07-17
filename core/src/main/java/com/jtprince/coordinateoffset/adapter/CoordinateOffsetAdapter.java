package com.jtprince.coordinateoffset.adapter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

@NullMarked
public interface CoordinateOffsetAdapter {
    Path getConfigDir();

    Logger getLogger();

    @Nullable OffsetPlayer getPlayer(UUID playerUuid);
    OffsetPlayer adaptPlayer(Object platformPlayerObject) throws ClassCastException;
    OffsetLocation adaptLocation(Object platformLocationObject) throws ClassCastException;

    OffsetPersistenceAdapter getPersistenceAdapter();

    OffsetSwapper getOffsetSwapper();

    int getMinimumOffsetMultiple();

    void assertMainThread(String methodName) throws IllegalStateException;

    void shutdown();
}
