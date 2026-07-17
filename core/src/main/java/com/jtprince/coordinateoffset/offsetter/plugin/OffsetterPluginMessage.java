package com.jtprince.coordinateoffset.offsetter.plugin;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import com.jtprince.coordinateoffset.FixedOffset;
import com.jtprince.coordinateoffset.offsetter.PacketOffsetter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@NullMarked
public final class OffsetterPluginMessage {

    private static final List<PluginOffsetter> PLUGIN_OFFSETTER_OFFSETTERS = List.of(
        new PluginOffsetterDistantHorizons(),
        new PluginOffsetterWorldEditCUI()
    );

    private static final Map<String, PluginOffsetter.Client> byChannelNameClient;
    private static final Map<String, PluginOffsetter.Server> byChannelNameServer;
    static  {
        try {
            byChannelNameClient = new HashMap<>();
            byChannelNameServer = new HashMap<>();
            for (PluginOffsetter offsetter : PLUGIN_OFFSETTER_OFFSETTERS) {
                PluginOffsetter.Client client = offsetter.getClientOffsetter();
                PluginOffsetter.Server server = offsetter.getServerOffsetter();
                for (String channel : offsetter.getHandledChannels()) {
                    if (client != null) {
                        byChannelNameClient.put(channel, client);
                    }
                    if (server != null) {
                        byChannelNameServer.put(channel, server);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private OffsetterPluginMessage() {}

    public static class Client extends PacketOffsetter<WrapperPlayClientPluginMessage> {
        public Client() {
            super(WrapperPlayClientPluginMessage.class, PacketType.Play.Client.PLUGIN_MESSAGE);
        }

        @Override
        public void offset(WrapperPlayClientPluginMessage packet, FixedOffset offset, User user) {
            PluginOffsetter.Client offsetter = byChannelNameClient.get(packet.getChannelName());
            if (offsetter != null) {
                offsetter.offset(packet, offset, user);
            }
        }

        @Override
        public void onUserDisconnect(User user) {
            for (PluginOffsetter offsetter : PLUGIN_OFFSETTER_OFFSETTERS) {
                offsetter.onUserDisconnect(user);
            }
        }
    }

    public static class Server extends PacketOffsetter<WrapperPlayServerPluginMessage> {
        public Server() {
            super(WrapperPlayServerPluginMessage.class, PacketType.Play.Server.PLUGIN_MESSAGE);
        }

        @Override
        public void offset(WrapperPlayServerPluginMessage packet, FixedOffset offset, User user) {
            PluginOffsetter.Server offsetter = byChannelNameServer.get(packet.getChannelName());
            if (offsetter != null) {
                offsetter.offset(packet, offset, user);
            }
        }
    }

    public interface PluginOffsetter {
        Set<String> getHandledChannels();
        default @Nullable Client getClientOffsetter() { return null; }
        default @Nullable Server getServerOffsetter() { return null; }
        default void onUserDisconnect(User user) {}

        abstract class Client extends PacketOffsetter<WrapperPlayClientPluginMessage> {
            public Client() {
                super(WrapperPlayClientPluginMessage.class, PacketType.Play.Client.PLUGIN_MESSAGE);
            }
        }

        abstract class Server extends PacketOffsetter<WrapperPlayServerPluginMessage> {
            public Server() {
                super(WrapperPlayServerPluginMessage.class, PacketType.Play.Server.PLUGIN_MESSAGE);
            }
        }
    }
}
