package com.jtprince.coordinateoffset.config;

import com.jtprince.coordinateoffset.provider.OffsetProvider;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

@NullMarked
public interface CoordinateOffsetProviderConfig {
    OffsetProvider getDefaultOffsetProviderConfig();
    List<OffsetProviderOverrideConfig> getOffsetProviderOverrides();
    Map<String, OffsetProvider> getAllOffsetProviderConfigs();
}
