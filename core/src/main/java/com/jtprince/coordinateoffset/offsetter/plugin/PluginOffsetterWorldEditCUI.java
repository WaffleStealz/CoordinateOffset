package com.jtprince.coordinateoffset.offsetter.plugin;

import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import com.jtprince.coordinateoffset.CoordinateOffsetCore;
import com.jtprince.coordinateoffset.FixedOffset;
import org.jspecify.annotations.NullMarked;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@NullMarked
public class PluginOffsetterWorldEditCUI implements OffsetterPluginMessage.PluginOffsetter {
    private static final Set<String> CHANNELS = Set.of("worldedit:cui");
    @Override
    public Set<String> getHandledChannels() {
        return CHANNELS;
    }

    @Override
    public OffsetterPluginMessage.PluginOffsetter.Server getServerOffsetter() {
        return new Server();
    }
    public static class Server extends OffsetterPluginMessage.PluginOffsetter.Server {
        @Override
        public void offset(WrapperPlayServerPluginMessage packet, FixedOffset offset, User user) {

            String data = new String(packet.getData(), StandardCharsets.ISO_8859_1);
            String[] segments = data.split("\\|");

            if (CoordinateOffsetCore.get().isDebugEnabled()) {
                CoordinateOffsetCore.get().getLogger().info("Outgoing WorldEditCUI plugin message: " + data);
            }

            switch (segments[0]) {
                case "cyl" -> {

                    int x = Integer.parseInt(segments[1]);
                    int z = Integer.parseInt(segments[3]);
                    segments[1] = String.valueOf(x - offset.x());
                    segments[3] = String.valueOf(z - offset.z());
                }
                case "e" -> {

                    if (!segments[1].equals("0")) return;

                    int x = Integer.parseInt(segments[2]);
                    int z = Integer.parseInt(segments[4]);
                    segments[2] = String.valueOf(x - offset.x());
                    segments[4] = String.valueOf(z - offset.z());
                }
                case "mm" -> {

                    return;
                }
                case "p" -> {

                    int x = Integer.parseInt(segments[2]);
                    int z = Integer.parseInt(segments[4]);
                    segments[2] = String.valueOf(x - offset.x());
                    segments[4] = String.valueOf(z - offset.z());
                }
                case "p2" -> {

                    int x = Integer.parseInt(segments[2]);
                    int z = Integer.parseInt(segments[3]);
                    segments[2] = String.valueOf(x - offset.x());
                    segments[3] = String.valueOf(z - offset.z());
                }
                case "poly" -> {

                    return;
                }
                case "s" -> {

                    return;
                }

                case "col", "grid", "u" -> {

                    return;
                }

                default -> {
                    String safeData = new String(packet.getData(), StandardCharsets.US_ASCII);
                    CoordinateOffsetCore.get().getLogger().warning("Failed to offset an outgoing WorldEditCUI plugin message: " + safeData);
                    return;
                }
            }

            packet.setData(String.join("|", segments).getBytes(StandardCharsets.ISO_8859_1));
        }
    }
}
