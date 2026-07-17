package com.jtprince.coordinateoffset.offsetter;

import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.HeightmapType;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.jtprince.coordinateoffset.FixedOffset;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
public class OffsettedColumn extends Column {
    private final Column inner;
    private final FixedOffset offset;
    private final User user;

    private static final BaseChunk[] emptyChunkList = {};

    public OffsettedColumn(Column column, FixedOffset offset, User user) {
        super(0, 0, false, emptyChunkList, null);
        this.inner = column;
        this.offset = offset;
        this.user = user;
    }

    @Override
    public int getX() {
        return inner.getX() - offset.chunkX();
    }

    @Override
    public int getZ() {
        return inner.getZ() - offset.chunkZ();
    }

    @Override
    public boolean isFullChunk() {
        return inner.isFullChunk();
    }

    @Override
    public BaseChunk[] getChunks() {
        return inner.getChunks();
    }

    @Override
    public TileEntity[] getTileEntities() {

        if (user.getClientVersion().isOlderThan(ClientVersion.V_1_18)) {
            TileEntity[] entities = inner.getTileEntities();
            for (TileEntity entity : entities) {
                entity.setX(entity.getX() - offset.x());
                entity.setZ(entity.getZ() - offset.z());
            }
            return entities;
        } else {
            return inner.getTileEntities();
        }
    }

    @Override
    public boolean hasHeightMaps() {
        return inner.hasHeightMaps();
    }

    @SuppressWarnings("deprecation")
    @Override
    public NBTCompound getHeightMaps() {
        return inner.getHeightMaps();
    }

    @Override
    public Map<HeightmapType, long[]> getHeightmaps() {
        return inner.getHeightmaps();
    }

    @Override
    public boolean hasBiomeData() {
        return inner.hasBiomeData();
    }

    @Override
    public int[] getBiomeDataInts() {
        return inner.getBiomeDataInts();
    }

    @Override
    public byte[] getBiomeDataBytes() {
        return inner.getBiomeDataBytes();
    }
}
