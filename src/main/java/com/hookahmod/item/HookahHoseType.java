package com.hookahmod.item;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;

public enum HookahHoseType {
    NONE(0),
    SHORT(5),
    LONG(10);

    public static final StreamCodec<RegistryFriendlyByteBuf, HookahHoseType> STREAM_CODEC =
            ByteBufCodecs.idMapper(HookahHoseType::byId, HookahHoseType::ordinal).cast();

    private final int maxLength;

    HookahHoseType(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public boolean isPresent() {
        return this != NONE;
    }

    public static HookahHoseType byId(int id) {
        HookahHoseType[] values = values();
        if (id < 0 || id >= values.length) return NONE;
        return values[id];
    }
}
