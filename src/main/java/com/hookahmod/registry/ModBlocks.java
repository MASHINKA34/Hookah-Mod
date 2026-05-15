package com.hookahmod.registry;

import com.hookahmod.HookahMod;
import com.hookahmod.block.HookahBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HookahMod.MOD_ID);

    public static final DeferredBlock<HookahBlock> HOOKAH = BLOCKS.register(
            "hookah",
            () -> new HookahBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(HookahBlock.HAS_COAL) ? 8 : 0))
    );

    private ModBlocks() {}
}
