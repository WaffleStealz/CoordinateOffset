package com.jtprince.coordinateoffset.offsetter.server;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDebugBlockValue;
import com.jtprince.coordinateoffset.FixedOffset;
import com.jtprince.coordinateoffset.offsetter.PacketOffsetter;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class OffsetterServerDebugBlockValue extends PacketOffsetter<WrapperPlayServerDebugBlockValue> {
    public OffsetterServerDebugBlockValue() {
        super(WrapperPlayServerDebugBlockValue.class, PacketType.Play.Server.DEBUG_BLOCK_VALUE);
    }

    @Override
    public void offset(WrapperPlayServerDebugBlockValue packet, FixedOffset offset, User user) {
        packet.setBlockPos(apply(packet.getBlockPos(), offset));

    }
}
