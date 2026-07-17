package com.jtprince.coordinateoffset.command;

import com.jtprince.coordinateoffset.CoordinateOffsetCore;
import com.jtprince.coordinateoffset.OffsetChange;
import com.jtprince.coordinateoffset.OffsetData;
import com.jtprince.coordinateoffset.OffsetFactory;
import com.jtprince.coordinateoffset.adapter.OffsetLocation;
import com.jtprince.coordinateoffset.adapter.OffsetPlayer;
import com.jtprince.coordinateoffset.config.MessagesConfig;
import com.jtprince.coordinateoffset.provider.OffsetProvider;
import com.jtprince.coordinateoffset.provider.OffsetProviderContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NullMarked
public class OffsetCommandExecutor {
    private final CoordinateOffsetCore core;
    public OffsetCommandExecutor(CoordinateOffsetCore core) {
        this.core = core;
    }

    public enum Result {
        SUCCESS,
        FAIL
    }

    public Result execute(OffsetReloadCommand command) {
        boolean success = core.reloadConfig();
        if (success) {
            core.getMessages().reload.success.send(command.getCommandSender());
            return Result.SUCCESS;
        } else {
            core.getMessages().reload.fail.send(command.getCommandSender());
            return Result.FAIL;
        }
    }

    public Result execute(OffsetQueryCommand command) {
        OffsetData offset = core.getOffsetHolder().getOffset(command.getTarget());
        OffsetLocation location = command.getTarget().getLocation();

        MessagesConfig.Query.C msgs;
        if (command.getCommandSender().isPlayer(command.getTarget())) {
            if (offset.offset().isZero()) {
                msgs = core.getMessages().query.selfNoOffset;
            } else {
                msgs = core.getMessages().query.self;
            }
        } else {
            msgs = core.getMessages().query.other;
        }

        msgs.offset().send(command.getCommandSender(),
            Placeholder.component("target", Component.text(command.getTarget().getName())),
            Placeholder.component("x", Component.text(offset.offset().x())),
            Placeholder.component("z", Component.text(offset.offset().z()))
        );
        msgs.coordinates().send(command.getCommandSender(),
            Placeholder.component("target", Component.text(command.getTarget().getName())),
            Placeholder.component("x", Component.text((int) location.getX())),
            Placeholder.component("y", Component.text((int) location.getY())),
            Placeholder.component("z", Component.text((int) location.getZ()))
        );

        if (command.isVerbose()) {
            switch (offset.source()) {
                case OffsetData.Source.PermissionBypass ignored -> {

                    core.getMessages().query.verbose.zeroByPermission.send(command.getCommandSender());
                }
                case OffsetData.Source.BedrockBypass ignored -> {

                    core.getMessages().query.verbose.zeroByBedrock.send(command.getCommandSender());
                }
                case OffsetData.Source.Provider provider -> {
                    if (provider.overrideRuleIndex() != null) {

                        core.getMessages().query.verbose.generatedByProviderOverride.send(command.getCommandSender(),
                            Placeholder.component("provider", Component.text(provider.provider().name)),
                            Placeholder.component("rule_number", Component.text(provider.overrideRuleIndex().toString())));
                    } else {

                        core.getMessages().query.verbose.generatedByProviderDefault.send(command.getCommandSender(),
                            Placeholder.component("provider", Component.text(provider.provider().name)));
                    }
                }
                case OffsetData.Source.SetCommand setCommand -> {

                    core.getMessages().query.verbose.generatedByCommand.send(command.getCommandSender(),
                        Placeholder.component("sender", Component.text(setCommand.command().getCommandSender().name())));
                }
                case OffsetData.Source.PluginSet ignored -> {

                    core.getMessages().query.verbose.generatedByPlugin.send(command.getCommandSender());
                }
            }
        }

        return Result.SUCCESS;
    }

    public Result execute(OffsetRegenerateCommand command) {
        List<OffsetPlayer> successfulTargets = new ArrayList<>();
        for (OffsetPlayer target : command.getTargets()) {
            OffsetChange result = core.getOffsetHolder().generateNextOffset(
                target, target.getLocation(), target.getLocation(), OffsetProviderContext.ProvideReason.COMMAND_REGENERATE);

            if (!result.offsetChanged()) {
                MessagesConfig.Message msg = switch (result.newOffsetData().source()) {
                    case OffsetData.Source.BedrockBypass ignored -> core.getMessages().regenerate.unchangedBedrock;
                    case OffsetData.Source.PermissionBypass ignored -> core.getMessages().regenerate.unchangedPermission;
                    case OffsetData.Source.Provider ignored -> core.getMessages().regenerate.unchanged;
                    case OffsetData.Source.SetCommand ignored -> core.getMessages().regenerate.unchanged;
                    case OffsetData.Source.PluginSet ignored -> core.getMessages().regenerate.unchanged;
                };
                msg.send(command.getCommandSender(),
                    Placeholder.component("target", Component.text(target.getName())));
                continue;
            }

            core.getAdapter().getOffsetSwapper().forceOffsetSwap(target);
            successfulTargets.add(target);
        }

        if (successfulTargets.size() == 1) {

            core.getMessages().regenerate.successSingle.send(command.getCommandSender(),
                Placeholder.component("target", Component.text(successfulTargets.getFirst().getName())));
        } else if (successfulTargets.size() > 1) {

            core.getMessages().regenerate.successMultiple.send(command.getCommandSender(),
                Placeholder.component("count", Component.text(successfulTargets.size())),
                Placeholder.component("targets", Component.text(successfulTargets.stream()
                    .map(OffsetPlayer::getName)
                    .collect(Collectors.joining(", ")))));
        }

        return successfulTargets.isEmpty() ? Result.FAIL : Result.SUCCESS;
    }

    public Result execute(OffsetSetCommand command) {
        List<OffsetPlayer> successfulTargets = new ArrayList<>();
        for (OffsetPlayer target : command.getTargets()) {
            OffsetChange result = core.getOffsetHolder().setNextOffsetByCommand(target, command);

            Double warnScaling = command.getWarnScaling(target);
            if (warnScaling != null && warnScaling != 1.0) {
                core.getMessages().set.warningCoordinateScaling.send(command.getCommandSender(),
                    Placeholder.component("target", Component.text(target.getName())),
                    Placeholder.component("scaling", Component.text(warnScaling)),
                    Placeholder.component("world", Component.text(target.getLocation().getWorld().getName())));
            }

            if (!result.offsetChanged()) {
                MessagesConfig.Message msg = switch (result.newOffsetData().source()) {
                    case OffsetData.Source.BedrockBypass ignored -> core.getMessages().set.unchangedBedrock;
                    case OffsetData.Source.PermissionBypass ignored -> core.getMessages().set.unchanged;
                    case OffsetData.Source.Provider ignored -> core.getMessages().set.unchanged;
                    case OffsetData.Source.SetCommand ignored -> core.getMessages().set.unchanged;
                    case OffsetData.Source.PluginSet ignored -> core.getMessages().set.unchanged;
                };
                msg.send(command.getCommandSender(),
                    Placeholder.component("target", Component.text(target.getName())));
                continue;
            }

            core.getAdapter().getOffsetSwapper().forceOffsetSwap(target);

            OffsetProvider notPersistentProvider = command.getOffsetIsNotPersistentForProvider(target);
            if (OffsetFactory.canBypassByPermission(target)) {

                core.getMessages().set.warningNotPersistentByPermission.send(command.getCommandSender(),
                    Placeholder.component("target", Component.text(target.getName())));
            } else if (notPersistentProvider != null) {

                core.getMessages().set.warningNotPersistentByProvider.send(command.getCommandSender(),
                    Placeholder.component("target", Component.text(target.getName())),
                    Placeholder.component("provider", Component.text(notPersistentProvider.name)));
            }

            successfulTargets.add(target);
        }

        if (successfulTargets.size() == 1) {

            core.getMessages().set.successSingle.send(command.getCommandSender(),
                Placeholder.component("target", Component.text(successfulTargets.getFirst().getName())),
                Placeholder.component("x", Component.text(command.getOffset().x())),
                Placeholder.component("z", Component.text(command.getOffset().z())));
        } else if (successfulTargets.size() > 1) {

            core.getMessages().set.successMultiple.send(command.getCommandSender(),
                Placeholder.component("count", Component.text(successfulTargets.size())),
                Placeholder.component("targets", Component.text(successfulTargets.stream()
                    .map(OffsetPlayer::getName)
                    .collect(Collectors.joining(", ")))),
                Placeholder.component("x", Component.text(command.getOffset().x())),
                Placeholder.component("z", Component.text(command.getOffset().z())));
        }

        return successfulTargets.isEmpty() ? Result.FAIL : Result.SUCCESS;
    }
}
