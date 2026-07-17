package com.jtprince.coordinateoffset.command;

import com.jtprince.coordinateoffset.ScalableOffset;
import com.jtprince.coordinateoffset.adapter.OffsetPlayer;
import com.jtprince.coordinateoffset.provider.OffsetProvider;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

@NullMarked
public class OffsetSetCommand implements OffsetCommand {
    private final OffsetCommandSender commandSender;
    private final List<OffsetPlayer> targets;
    private final ScalableOffset offset;

    private @Nullable OffsetProvider notPersistentForProvider = null;
    private final Set<OffsetPlayer> notPersistentInProviderTargets = new HashSet<>();
    private final Map<OffsetPlayer, Double> scalingForTargets = new HashMap<>();

    public OffsetSetCommand(OffsetCommandSender commandSender, List<? extends OffsetPlayer> targets, ScalableOffset offset) {
        this.commandSender = commandSender;
        this.targets = List.copyOf(targets);
        this.offset = offset;
    }

    @Override
    public OffsetCommandSender getCommandSender() {
        return commandSender;
    }

    public List<OffsetPlayer> getTargets() {
        return targets;
    }

    public ScalableOffset getOffset() {
        return offset;
    }

    public void warnOffsetIsNotPersistentInProvider(OffsetProvider provider, OffsetPlayer target) {
        notPersistentForProvider = provider;
        notPersistentInProviderTargets.add(target);
    }

    public @Nullable OffsetProvider getOffsetIsNotPersistentForProvider(OffsetPlayer target) {
        if (notPersistentInProviderTargets.contains(target)) {
            return notPersistentForProvider;
        } else {
            return null;
        }
    }

    public void warnScaling(OffsetPlayer target, double scalingFactor) {
        scalingForTargets.put(target, scalingFactor);
    }

    public @Nullable Double getWarnScaling(OffsetPlayer target) {
        return scalingForTargets.get(target);
    }
}
