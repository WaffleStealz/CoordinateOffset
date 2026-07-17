package com.jtprince.coordinateoffset.offsetter.client;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.jtprince.coordinateoffset.FixedOffset;
import com.jtprince.coordinateoffset.offsetter.PacketOffsetter;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@NullMarked
public class OffsetterClientClickWindow extends PacketOffsetter<WrapperPlayClientClickWindow> {
    public OffsetterClientClickWindow() {
        super(WrapperPlayClientClickWindow.class, PacketType.Play.Client.CLICK_WINDOW);
    }

    @Override
    public void offset(WrapperPlayClientClickWindow packet, FixedOffset offset, User user) {
        if (packet.getSlots().isPresent()) {
            Map<Integer, ItemStack> clientItems = packet.getSlots().get();
            Map<Integer, ItemStack> serverItems = clientItems.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, v -> {
                        ItemStack modifiedItem = unapplyItemStack(v.getValue(), offset);
                        if (modifiedItem != null) {
                            return modifiedItem;
                        } else {
                            return v.getValue();
                        }
                    }));
            packet.setSlots(Optional.of(serverItems));
        }

        ItemStack modifiedCarriedItemStack = unapplyItemStack(packet.getCarriedItemStack(), offset);
        if (modifiedCarriedItemStack != null) {
            packet.setCarriedItemStack(modifiedCarriedItemStack);
        }
    }
}
