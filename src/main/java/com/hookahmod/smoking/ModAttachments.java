package com.hookahmod.smoking;

import com.hookahmod.HookahMod;
import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, HookahMod.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> INTOXICATION = ATTACHMENTS.register(
            "intoxication",
            () -> AttachmentType.builder(() -> 0.0f).serialize(Codec.FLOAT, value -> value > 0.0f).build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> TRIP_LOCK = ATTACHMENTS.register(
            "trip_lock",
            () -> AttachmentType.builder(() -> Boolean.FALSE).serialize(Codec.BOOL, value -> value).build()
    );

    private ModAttachments() {}
}
