package com.jtprince.coordinateoffset.command;

import com.jtprince.coordinateoffset.adapter.OffsetPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collections;

@NullMarked
public record OffsetCommandSender(Audience audience, String name, @Nullable OffsetPlayer player) implements ForwardingAudience {
    @Override
    public Iterable<? extends Audience> audiences() {
        return Collections.singleton(audience);
    }

    public boolean isPlayer(OffsetPlayer player) {
        return this.player != null && this.player.equals(player);
    }
}
