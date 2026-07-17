package com.jtprince.coordinateoffset;

import com.jtprince.coordinateoffset.adapter.OffsetPlayer;
import com.jtprince.coordinateoffset.config.OffsetProviderOverrideConfig;
import com.jtprince.coordinateoffset.provider.OffsetProvider;
import com.jtprince.coordinateoffset.provider.OffsetProviderContext;
import org.geysermc.geyser.api.GeyserApi;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class OffsetFactory {
    private final CoordinateOffsetCore core;

    OffsetFactory(CoordinateOffsetCore core) {
        this.core = core;
    }

    OffsetData createOffset(OffsetProviderContext context) {
        OffsetProvider provider = null;

        if (canBypassByPermission(context.player())) {
            return new OffsetData(
                Offset.ZERO,
                new OffsetData.Source.PermissionBypass(),
                context
            );
        }

        if (isBedrockPlayerAndLogOnJoin(context)) {
            return new OffsetData(
                Offset.ZERO,
                new OffsetData.Source.BedrockBypass(),
                context
            );
        }

        Integer overrideRuleIndex = null;

        if (provider == null) {
            for (int i = 0; i < core.getProviderConfig().getOffsetProviderOverrides().size(); i++) {
                OffsetProviderOverrideConfig override = core.getProviderConfig().getOffsetProviderOverrides().get(i);
                if (override.appliesTo(context)) {
                    overrideRuleIndex = i + 1;
                    provider = override.getOffsetProvider();
                    break;
                }
            }
        }

        if (provider == null) {
            provider = core.getProviderConfig().getDefaultOffsetProviderConfig();
        }

        Offset offset = provider.provideOffset(context);
        FixedOffset fixed = fixOffsetVerbosely(offset, new OffsetData.Source.Provider(provider, overrideRuleIndex), context);
        return new OffsetData(fixed, new OffsetData.Source.Provider(provider, overrideRuleIndex), context);
    }

    public OffsetData createSpecificOffset(Offset offset, OffsetData.Source source, OffsetProviderContext context) {

        if (isBedrockPlayerAndLogOnJoin(context)) {
            return new OffsetData(
                Offset.ZERO,
                new OffsetData.Source.BedrockBypass(),
                context
            );
        }

        FixedOffset fixed = fixOffsetVerbosely(offset, source, context);
        return new OffsetData(fixed, source, context);
    }

    private FixedOffset fixOffsetVerbosely(Offset offset, OffsetData.Source source, OffsetProviderContext context) {
        return switch (offset) {
            case FixedOffset f -> f;
            case ScalableOffset s -> {
                double scale = context.playerLocation().getWorld().getCoordinateScale();

                if (offset.isZero() || scale == 1.0 || !core.getConfig().getVerbose()) yield s.scaleDownAndRound(scale);

                String prefix = switch (source) {
                    case OffsetData.Source.SetCommand cmd -> {
                        cmd.command().warnScaling(context.player(), scale);
                        yield "Command from " + cmd.command().getCommandSender().name() + ": ";
                    }
                    case OffsetData.Source.Provider provider -> "Provider \"" + provider.provider().name + "\": ";
                    case OffsetData.Source.BedrockBypass ignored -> "";
                    case OffsetData.Source.PermissionBypass ignored -> "";
                    case OffsetData.Source.PluginSet ignored -> "";
                };
                core.getLogger().info(prefix + "Scaling offset " + s + " by " + scale + " to match coordinate scale of world \"" + context.playerLocation().getWorld().getName() + "\"");

                yield s.scaleDownAndRound(scale);
            }
        };
    }

    public static boolean canBypassByPermission(OffsetPlayer player) {
        return CoordinateOffsetCore.get().getConfig().getBypassByPermission() &&
            player.hasPermission(CoordinateOffsetPermission.BYPASS.node);
    }

    private boolean isBedrockPlayerAndLogOnJoin(OffsetProviderContext context) {
        try {
            if (GeyserApi.api().isBedrockPlayer(context.player().getUuid())) {

                if (context.reason() == OffsetProviderContext.ProvideReason.JOIN) {
                    core.getLogger().warning("Coordinate offsets are disabled for Bedrock player " +
                        context.player().getName() + ". (Give permission coordinateoffset.bypass to disable offsets " +
                        "and hide this warning)");
                }
                return true;
            }
        } catch (NoClassDefFoundError ignored) {

        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }
}
