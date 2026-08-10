package com.hookahmod.registry;

import com.hookahmod.HookahMod;
import com.hookahmod.block.HookahBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HookahMod.MOD_ID);

    public static final Supplier<BlockEntityType<HookahBlockEntity>> HOOKAH = register(
            "hookah",
            () -> BlockEntityType.Builder.of(
                    HookahBlockEntity::new,
                    ModBlocks.HOOKAH.get(),
                    ModBlocks.LUXURY_HOOKAH_PREVIEW.get()
            ).build(null)
    );

    private static <T extends BlockEntityType<?>> DeferredHolder<BlockEntityType<?>, T> register(String name, Supplier<T> sup) {
        return BLOCK_ENTITIES.register(name, sup);
    }

    private ModBlockEntities() {}
}
