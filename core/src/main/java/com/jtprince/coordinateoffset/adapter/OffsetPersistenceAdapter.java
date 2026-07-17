package com.jtprince.coordinateoffset.adapter;

import com.jtprince.coordinateoffset.ScalableOffset;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@NullMarked
public interface OffsetPersistenceAdapter {
    @Nullable ScalableOffset get(OffsetPlayer player, Key persistenceKey);
    void put(OffsetPlayer player, Key persistenceKey, ScalableOffset offset);
    void clear(UUID playerUuid, Key persistenceKey);

    record Key(
        String providerName,
        @Nullable String persistenceKeyOverride
    ) {
        public Key(String providerName) {
            this(providerName, null);
        }

        public String getPersistenceKey() {
            return "provider." + (persistenceKeyOverride != null ? persistenceKeyOverride : providerName);
        }
    }
}
