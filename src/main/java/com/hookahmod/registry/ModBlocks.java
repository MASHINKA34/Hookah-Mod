package com.hookahmod.registry;

import com.hookahmod.HookahMod;
import com.hookahmod.block.HookahBlock;
import com.hookahmod.block.ModCropBlock;
import com.hookahmod.block.WhiteMonsterBlock;
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

    public static final DeferredBlock<ModCropBlock> TOBACCO_CROP = BLOCKS.register(
            "tobacco_crop",
            () -> new ModCropBlock(cropProperties(), ModItems.TOBACCO_SEED)
    );

    public static final DeferredBlock<ModCropBlock> MINT_CROP = BLOCKS.register(
            "mint_crop",
            () -> new ModCropBlock(cropProperties(), ModItems.MINT_SEED)
    );

    public static final DeferredBlock<ModCropBlock> LAVENDER_CROP = BLOCKS.register(
            "lavender_crop",
            () -> new ModCropBlock(cropProperties(), ModItems.LAVENDER_SEED)
    );

    public static final DeferredBlock<WhiteMonsterBlock> WHITE_MONSTER = BLOCKS.register(
            "white_monster",
            () -> new WhiteMonsterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(0.3F)
                    .sound(SoundType.METAL)
                    .noOcclusion())
    );

    public static final DeferredBlock<WhiteMonsterBlock> EMPTY_WHITE_MONSTER = BLOCKS.register(
            "empty_white_monster",
            () -> new WhiteMonsterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(0.3F)
                    .sound(SoundType.METAL)
                    .noOcclusion())
    );

    private static BlockBehaviour.Properties cropProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP);
    }

    private ModBlocks() {}
}
