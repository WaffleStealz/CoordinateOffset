package com.jtprince.coordinateoffset;

import com.jtprince.coordinateoffset.command.OffsetSetCommand;
import com.jtprince.coordinateoffset.provider.OffsetProvider;
import com.jtprince.coordinateoffset.provider.OffsetProviderContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record OffsetData(
    FixedOffset offset,
    Source source,
    OffsetProviderContext context
) {
    public sealed interface Source
        permits Source.PermissionBypass, Source.BedrockBypass, Source.Provider, Source.SetCommand, Source.PluginSet {

        record PermissionBypass() implements Source {}

        record BedrockBypass() implements Source {}

        record Provider(OffsetProvider provider, @Nullable Integer overrideRuleIndex) implements Source {}

        record SetCommand(OffsetSetCommand command, @Nullable OffsetProvider affectedProvider) implements Source {}

        record PluginSet() implements Source {}
    }
}
