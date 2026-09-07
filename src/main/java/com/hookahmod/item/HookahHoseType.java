package com.hookahmod.item;

import com.hookahmod.config.HookahConfig;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;

public enum HookahHoseType {
    NONE,
    SHORT,
    LONG;

    public static final StreamCodec<RegistryFriendlyByteBuf, HookahHoseType> STREAM_CODEC =
            ByteBufCodecs.idMapper(HookahHoseType::byId, HookahHoseType::ordinal).cast();

    public int getMaxLength() {
        return switch (this) {
            case SHORT -> HookahConfig.shortHoseRange;
            case LONG -> HookahConfig.longHoseRange;
            default -> 0;
        };
    }

    public boolean isPresent() {
        return this != NONE;
    }

    public static double maxRangeSqr() {
        int longest = 0;
        for (HookahHoseType type : values()) longest = Math.max(longest, type.getMaxLength());
        return (double) longest * longest;
    }

    public static HookahHoseType byId(int id) {
        HookahHoseType[] values = values();
        if (id < 0 || id >= values.length) return NONE;
        return values[id];
    }
}
