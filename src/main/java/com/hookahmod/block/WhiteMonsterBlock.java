package com.hookahmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WhiteMonsterBlock extends Block {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(6.0D, 0.0D, 5.0D, 10.0D, 14.0D, 11.0D),
            Block.box(5.0D, 0.0D, 6.0D, 11.0D, 14.0D, 10.0D)
    );

    public WhiteMonsterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
