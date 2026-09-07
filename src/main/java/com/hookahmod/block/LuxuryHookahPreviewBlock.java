package com.hookahmod.block;

import com.hookahmod.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LuxuryHookahPreviewBlock extends HookahBlock {

    public static final MapCodec<LuxuryHookahPreviewBlock> CODEC = simpleCodec(LuxuryHookahPreviewBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(-0.5, 0, -0.5, 16.5, 12, 16.5),
            Block.box(4, 12, 4, 12, 34, 12),
            Block.box(2, 34, 2, 14, 43, 14)
    );

    public LuxuryHookahPreviewBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean supportsWearing() {
        return false;
    }

    @Override
    public boolean hasDynamicParts() {
        return false;
    }

    @Override
    protected ItemStack stackForState(BlockState state) {
        return new ItemStack(ModItems.LUXURY_HOOKAH_PREVIEW.get());
    }
}
