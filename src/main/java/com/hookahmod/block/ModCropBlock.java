package com.hookahmod.block;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

import java.util.function.Supplier;

public class ModCropBlock extends CropBlock {

    private final Supplier<? extends ItemLike> seed;

    public ModCropBlock(Properties properties, Supplier<? extends ItemLike> seed) {
        super(properties);
        this.seed = seed;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }
}
