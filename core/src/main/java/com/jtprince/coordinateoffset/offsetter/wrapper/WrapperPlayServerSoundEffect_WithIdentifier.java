package com.jtprince.coordinateoffset.offsetter.wrapper;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("unused")
public class WrapperPlayServerSoundEffect_WithIdentifier extends PacketWrapper<@NonNull WrapperPlayServerSoundEffect_WithIdentifier> {
    private int soundID;
    private @Nullable ResourceLocation soundName;
    private @Nullable Boolean hasFixedRange;
    private @Nullable Float range;
    private SoundCategory soundCategory;
    private Vector3i effectPosition;
    private float volume;
    private float pitch;
    private long seed;

    public WrapperPlayServerSoundEffect_WithIdentifier(PacketSendEvent event) {
        super(event);
    }

    public WrapperPlayServerSoundEffect_WithIdentifier(int soundID, SoundCategory soundCategory,
                                                       Vector3i effectPosition, float volume, float pitch) {
        this(soundID, soundCategory, effectPosition, volume, pitch, -1);
    }

    public WrapperPlayServerSoundEffect_WithIdentifier(int soundID, SoundCategory soundCategory,
                                                       Vector3i effectPosition, float volume, float pitch, long seed) {
        super(PacketType.Play.Server.SOUND_EFFECT);
        this.soundID = soundID;
        this.soundCategory = soundCategory;
        this.effectPosition = effectPosition;
        this.volume = volume;
        this.pitch = pitch;
        this.seed = seed;
    }

    @Override
    public void read() {
        soundID = readVarInt();
        if (soundID == 0) {
            soundName = readIdentifier();
            hasFixedRange = readBoolean();
            if (hasFixedRange) {
                range = readFloat();
            }
        }
        soundCategory = SoundCategory.fromId(readVarInt());
        effectPosition = new Vector3i(readInt(), readInt(), readInt());
        volume = readFloat();
        pitch = readFloat();
        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_19)) {
            this.seed = readLong();
        }
    }

    @Override
    public void write() {
        writeVarInt(soundID);
        if (soundName != null) {
            writeIdentifier(soundName);
        }
        if (hasFixedRange != null) {
            writeBoolean(hasFixedRange);
            if (range != null) {
                writeFloat(range);
            }
        }
        writeVarInt(soundCategory.ordinal());
        writeInt(effectPosition.x);
        writeInt(effectPosition.y);
        writeInt(effectPosition.z);
        writeFloat(volume);
        writeFloat(pitch);
        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_19)) {
            writeLong(seed);
        }
    }

    @Override
    public void copy(WrapperPlayServerSoundEffect_WithIdentifier wrapper) {
        soundID = wrapper.soundID;
        soundCategory = wrapper.soundCategory;
        effectPosition = wrapper.effectPosition;
        volume = wrapper.volume;
        pitch = wrapper.pitch;
        seed = wrapper.seed;
    }

    public int getSoundId() {
        return soundID;
    }

    public void setSoundId(int soundID) {
        this.soundID = soundID;
    }

    public SoundCategory getSoundCategory() {
        return soundCategory;
    }

    public void setSoundCategory(SoundCategory soundCategory) {
        this.soundCategory = soundCategory;
    }

    public Vector3i getEffectPosition() {
        return effectPosition;
    }

    public void setEffectPosition(Vector3i effectPosition) {
        this.effectPosition = effectPosition;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }
}
