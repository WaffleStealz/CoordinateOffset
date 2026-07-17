package com.jtprince.coordinateoffset.offsetter.plugin;

import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import com.jtprince.coordinateoffset.CoordinateOffsetCore;
import com.jtprince.coordinateoffset.FixedOffset;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public class PluginOffsetterDistantHorizons implements OffsetterPluginMessage.PluginOffsetter {

    private static final String CHANNEL_DHS_PRE_3_0 = "distant_horizons:message";
    private static final String CHANNEL_DHS_3_0_PLUS = "distant_horizons:msg";
    private static final Set<String> CHANNELS = Set.of(CHANNEL_DHS_PRE_3_0, CHANNEL_DHS_3_0_PLUS);

    private static final short SUPPORTED_PROTOCOL_VERSION_MIN = 11;
    private static final short SUPPORTED_PROTOCOL_VERSION_MAX = 15;

    private enum DhPluginMessageType {
        REMOTE_PLAYER_CONFIG,
        EXCEPTION,
        FULL_DATA_SOURCE_REQUEST,
        FULL_DATA_SOURCE_RESPONSE,
        FULL_DATA_PARTIAL_UPDATE,
        FULL_DATA_CHUNK,
        ;

        int getMessageId(int protocolVersion) {

            if (protocolVersion < 14) {
                return switch (this) {
                    case REMOTE_PLAYER_CONFIG -> 3;
                    case EXCEPTION -> 5;
                    case FULL_DATA_SOURCE_REQUEST -> 6;
                    case FULL_DATA_SOURCE_RESPONSE -> 7;
                    case FULL_DATA_PARTIAL_UPDATE -> 8;
                    case FULL_DATA_CHUNK -> 9;
                };
            } else {
                return switch (this) {
                    case REMOTE_PLAYER_CONFIG -> 4;
                    case EXCEPTION -> 6;
                    case FULL_DATA_SOURCE_REQUEST -> 7;
                    case FULL_DATA_SOURCE_RESPONSE -> 8;
                    case FULL_DATA_PARTIAL_UPDATE -> 9;
                    case FULL_DATA_CHUNK -> 10;
                };
            }
        }
    }

    private static final int DH_MSG_EXCEPTION_REQUEST_REJECTED = 2;
    private static final int DH_MSG_EXCEPTION_SECTION_REQUIRES_SPLITTING = 3;

    private @Nullable String activeChannelName = null;

    @Override
    public Set<String> getHandledChannels() {
        return CHANNELS;
    }

    @Override
    public OffsetterPluginMessage.PluginOffsetter.Server getServerOffsetter() {
        return new Server();
    }
    public class Server extends OffsetterPluginMessage.PluginOffsetter.Server {
        @Override
        public void offset(WrapperPlayServerPluginMessage packet, FixedOffset offset, User user) {
            if (activeChannelName == null) {

                activeChannelName = packet.getChannelName();
            }

            ByteBuf data = Unpooled.wrappedBuffer(packet.getData());

            short protocolVersion = data.readShort();
            short messageTypeId = data.readShort();

            if (protocolVersion < SUPPORTED_PROTOCOL_VERSION_MIN || protocolVersion > SUPPORTED_PROTOCOL_VERSION_MAX) {
                if (offset.isZero()) return;
                PlayerWarningCache warningCache = playerWarningCache.computeIfAbsent(user.getUUID(), k -> new PlayerWarningCache());
                if (!warningCache.hasWarnedProtocolVersion) {
                    warningCache.hasWarnedProtocolVersion = true;
                    CoordinateOffsetCore.get().getLogger().warning("A version of the Distant Horizons Support " +
                        "plugin (DHSupport) is installed which is not compatible with this version of " +
                        "CoordinateOffset. Player " + user.getName() + " may experience issues receiving LODs from " +
                        "the server while they have an offset applied. " +
                        (protocolVersion < SUPPORTED_PROTOCOL_VERSION_MIN ?
                            "Update DHSupport to resolve this issue." :
                            "Please update CoordinateOffset, or inform the CoordinateOffset developer of this issue " +
                                "if you are already on the latest version.") +
                        " (DHS protocol version = " + protocolVersion + ", CoordinateOffset supported versions = "
                        + SUPPORTED_PROTOCOL_VERSION_MIN + "-" + SUPPORTED_PROTOCOL_VERSION_MAX + ")");
                }
            }

            if (messageTypeId == DhPluginMessageType.REMOTE_PLAYER_CONFIG.getMessageId(protocolVersion)) {
                boolean distantGenerationEnabled = data.readBoolean();
                int renderDistance = data.readInt();

                int borderCenterX, borderCenterZ, borderRadius;
                if (CoordinateOffsetCore.get().getConfig().getObfuscateWorldBorder()) {

                    data.setInt(data.readerIndex(), 0);
                    borderCenterX = data.readInt();

                    data.setInt(data.readerIndex(), 0);
                    borderCenterZ = data.readInt();

                    data.setInt(data.readerIndex(), 30_000_000);
                    borderRadius = data.readInt();
                } else {

                    borderCenterX = data.getInt(data.readerIndex());
                    data.setInt(data.readerIndex(), borderCenterX - offset.x());
                    borderCenterX = data.readInt();

                    borderCenterZ = data.getInt(data.readerIndex());
                    data.setInt(data.readerIndex(), borderCenterZ - offset.z());
                    borderCenterZ = data.readInt();

                    borderRadius = data.readInt();
                }

                if (CoordinateOffsetCore.get().isDebugEnabled()) {
                    CoordinateOffsetCore.get().getLogger().info(String.format("Outgoing Distant Horizons config message: " +
                            "distantGenerationEnabled=%b, renderDistance=%d, borderCenterX=%d, borderCenterZ=%d, borderRadius=%d",
                        distantGenerationEnabled, renderDistance, borderCenterX, borderCenterZ, borderRadius));
                }
            }

            if (messageTypeId == DhPluginMessageType.EXCEPTION.getMessageId(protocolVersion)) {
                int tracker = data.readInt();
                int typeId = data.readInt();
                short messageLen = data.readShort();
                String exceptionMessage = data.readCharSequence(messageLen, StandardCharsets.UTF_8).toString();
                exceptionMessage.getBytes();
            }

            if (messageTypeId == DhPluginMessageType.FULL_DATA_SOURCE_RESPONSE.getMessageId(protocolVersion)) {
                int tracker = data.readInt();
                boolean hasBufferId = data.readBoolean();
                if (hasBufferId) {
                    int bufferId = data.readInt();
                    int numBeacons = data.readInt();
                    for (int i = 0; i < numBeacons; i++) {
                        int x = data.readInt();
                        data.setInt(data.readerIndex() - 4, x - offset.x());
                        int y = data.readInt();
                        int z = data.readInt();
                        data.setInt(data.readerIndex() - 4, z - offset.z());
                        int color = data.readInt();
                    }
                }
            }

            if (messageTypeId == DhPluginMessageType.FULL_DATA_PARTIAL_UPDATE.getMessageId(protocolVersion)) {
                short worldNameLen = data.readShort();
                data.skipBytes(worldNameLen);

                int bufferId = data.readInt();
                int numBeacons = data.readInt();
                for (int i = 0; i < numBeacons; i++) {
                    int x = data.readInt();
                    data.setInt(data.readerIndex() - 4, x - offset.x());
                    int y = data.readInt();
                    int z = data.readInt();
                    data.setInt(data.readerIndex() - 4, z - offset.z());
                    int color = data.readInt();
                }
            }

            if (messageTypeId == DhPluginMessageType.FULL_DATA_CHUNK.getMessageId(protocolVersion)) {
                int bufferId = data.readInt();
                int dataLength = data.readInt();

                boolean isFirst = data.getBoolean(data.capacity() - 1);
                if (isFirst) {
                    long sectionPosition = data.getLong(data.readerIndex());
                    DhSectionPosition sectionPositionObj = DhSectionPosition.fromLong(sectionPosition);
                    DhSectionPosition offsetted = sectionPositionObj.offset(offset);
                    long offsettedLong = offsetted.toLong();
                    data.setLong(data.readerIndex(), offsettedLong);
                }
            }

            data.release();
        }
    }

    @Override
    public OffsetterPluginMessage.PluginOffsetter.Client getClientOffsetter() {
        return new Client();
    }
    public class Client extends OffsetterPluginMessage.PluginOffsetter.Client {
        @Override
        public void offset(WrapperPlayClientPluginMessage packet, FixedOffset offset, User user) {
            ByteBuf data = Unpooled.wrappedBuffer(packet.getData());

            short protocolVersion = data.readShort();
            short messageTypeId = data.readShort();

            if (messageTypeId == DhPluginMessageType.REMOTE_PLAYER_CONFIG.getMessageId(protocolVersion)
                    && CoordinateOffsetCore.get().isDebugEnabled()) {
                printConfig(data, true);
            }

            if (messageTypeId == DhPluginMessageType.FULL_DATA_SOURCE_REQUEST.getMessageId(protocolVersion)) {
                int tracker = data.readInt();
                short worldNameLen = data.readShort();
                data.skipBytes(worldNameLen);

                long sectionPosition = data.getLong(data.readerIndex());
                DhSectionPosition sectionPositionObj = DhSectionPosition.fromLong(sectionPosition);
                DhSectionPosition unOffsetted = sectionPositionObj.offset(offset.negate());

                if (unOffsetted != null) {
                    long unOffsettedLong = unOffsetted.toLong();
                    data.setLong(data.readerIndex(), unOffsettedLong);
                } else if (sectionPositionObj.detailLevel() != 6) {

                    packet.setChannelName(packet.getChannelName() + "_cancelled_by_coordinateoffset");
                    sendDHExceptionMessage(user, protocolVersion, tracker,
                        DH_MSG_EXCEPTION_SECTION_REQUIRES_SPLITTING, "Only detail level 6 is supported");
                } else {

                    packet.setChannelName(packet.getChannelName() + "_cancelled_by_coordinateoffset");
                    sendDHExceptionMessage(user, protocolVersion, tracker,
                        DH_MSG_EXCEPTION_REQUEST_REJECTED, "Incompatible with current coordinate offset");

                    PlayerWarningCache warningCache = playerWarningCache.computeIfAbsent(user.getUUID(), k -> new PlayerWarningCache());
                    if (!warningCache.hasWarnedOffsetMultiple) {
                        warningCache.hasWarnedOffsetMultiple = true;
                        CoordinateOffsetCore.get().getLogger().warning("Player " + user.getName() +
                            " has Distant Horizons installed, but has an active coordinate offset " +
                            offset + " which is not a multiple of 64 blocks. The Distant Horizons plugin will NOT " +
                            "send any data to this player. To fix this, you will need to alter their coordinate " +
                            "offset so that both components are evenly divisible by 64.");
                    }
                }
            }

            data.release();
        }

        private void sendDHExceptionMessage(User user, short protocolVersion, int tracker, int exceptionType, String message) {

            ByteBuf responseData = Unpooled.buffer();
            responseData.writeShort(protocolVersion);
            responseData.writeShort(DhPluginMessageType.EXCEPTION.getMessageId(protocolVersion));
            responseData.writeInt(tracker);
            responseData.writeInt(exceptionType);
            responseData.writeShort((short) message.length());
            responseData.writeCharSequence(message, StandardCharsets.UTF_8);

            byte[] responseDataArray = new byte[responseData.readableBytes()];
            responseData.readBytes(responseDataArray);
            WrapperPlayServerPluginMessage responseMsg = new WrapperPlayServerPluginMessage(activeChannelName, responseDataArray);
            user.sendPacket(responseMsg);
            responseData.release();
        }
    }

    record DhSectionPosition(int detailLevel, int x, int z) {
        static DhSectionPosition fromLong(long sectionPosition) {

            int detailLevel = (int) (sectionPosition & 0xFF);

            int x = (int) ((sectionPosition >> 8) & 0x0FFFFFFF);
            if ((x & (1 << 27)) != 0) {
                x |= ~0x0FFFFFFF;
            }

            int z = (int) ((sectionPosition >> 36) & 0x0FFFFFFF);
            if ((z & (1 << 27)) != 0) {
                z |= ~0x0FFFFFFF;
            }
            return new DhSectionPosition(detailLevel, x, z);
        }

        long toLong() {
            long data = 0;
            data |= (detailLevel & 0xFFL);
            data |= (x & 0x0FFFFFFFL) << 8;
            data |= (z & 0x0FFFFFFFL) << 36;
            return data;
        }

        @Nullable DhSectionPosition offset(FixedOffset offset) {
            if (offset.x() % (1 << detailLevel) != 0) {
                return null;
            }
            if (offset.z() % (1 << detailLevel) != 0) {
                return null;
            }

            return new DhSectionPosition(detailLevel, x - (offset.x() >> detailLevel), z - (offset.z() >> detailLevel));
        }
    }

    private static class PlayerWarningCache {
        boolean hasWarnedProtocolVersion = false;
        boolean hasWarnedOffsetMultiple = false;
    }
    private final ConcurrentHashMap<UUID, PlayerWarningCache> playerWarningCache = new ConcurrentHashMap<>();

    @Override
    public void onUserDisconnect(User user) {
        playerWarningCache.remove(user.getUUID());
    }

    static void printConfig(ByteBuf data, boolean isIncoming) {
        boolean distantGenerationEnabled = data.readBoolean();
        int renderDistance = data.readInt();
        int borderCenterX = data.readInt();
        int borderCenterZ = data.readInt();
        int borderRadius = data.readInt();
        int fullDataRequestConcurrencyLimit = data.readInt();
        boolean realTimeUpdatesEnabled = data.readBoolean();
        int realTimeUpdateRadius = data.readInt();
        boolean loginDataSyncEnabled = data.readBoolean();
        int loginDataSyncRadius = data.readInt();
        int loginDataSyncRcLimit = data.readInt();
        int maxDataTransferSpeed = data.readInt();

        CoordinateOffsetCore.get().getLogger().info(String.format((isIncoming ? "Incoming" : "Outgoing") + " config message from Distant Horizons: " +
                "distantGenerationEnabled=%b, renderDistance=%d, borderCenterX=%d, borderCenterZ=%d, borderRadius=%d, fullDataRequestConcurrencyLimit=%d, realTimeUpdatesEnabled=%b, realTimeUpdateRadius=%d, loginDataSyncEnabled=%b, loginDataSyncRadius=%d, loginDataSyncRcLimit=%d, maxDataTransferSpeed=%d",
            distantGenerationEnabled, renderDistance, borderCenterX, borderCenterZ, borderRadius, fullDataRequestConcurrencyLimit, realTimeUpdatesEnabled, realTimeUpdateRadius, loginDataSyncEnabled, loginDataSyncRadius, loginDataSyncRcLimit, maxDataTransferSpeed));
    }
}
