package com.jtprince.coordinateoffset.offsetter.client;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.jtprince.coordinateoffset.FixedOffset;
import com.jtprince.coordinateoffset.offsetter.PacketOffsetter;
import org.jspecify.annotations.NullMarked;

import java.util.Set;

@NullMarked
public class OffsetterClientPlayerDigging extends PacketOffsetter<WrapperPlayClientPlayerDigging> {

    private static final Set<DiggingAction> ACTIONS_WITH_ZERO_POSITION = Set.of(
            DiggingAction.DROP_ITEM_STACK,
            DiggingAction.DROP_ITEM,
            DiggingAction.RELEASE_USE_ITEM,
            DiggingAction.SWAP_ITEM_WITH_OFFHAND
    );

    public OffsetterClientPlayerDigging() {
        super(WrapperPlayClientPlayerDigging.class, PacketType.Play.Client.PLAYER_DIGGING);
    }

    @Override
    public void offset(WrapperPlayClientPlayerDigging packet, FixedOffset offset, User user) {
        if (!ACTIONS_WITH_ZERO_POSITION.contains(packet.getAction())) {
            packet.setBlockPosition(unapply(packet.getBlockPosition(), offset));
        }
    }
}
