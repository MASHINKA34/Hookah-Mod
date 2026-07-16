package com.hookahmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WhiteMonsterBlock extends Block {

    public static final IntegerProperty COUNT = IntegerProperty.create("count", 1, 3);
    private static final VoxelShape ONE = Block.box(5.75D, 0.0D, 5.75D, 10.25D, 16.0D, 10.25D);
    private static final VoxelShape TWO = Shapes.or(
            Block.box(2.5D, 0.0D, 5.75D, 7.0D, 16.0D, 10.25D),
            Block.box(9.0D, 0.0D, 5.75D, 13.5D, 16.0D, 10.25D)
    );
    private static final VoxelShape THREE = Shapes.or(
            Block.box(2.0D, 0.0D, 8.5D, 6.5D, 16.0D, 13.0D),
            Block.box(9.5D, 0.0D, 8.5D, 14.0D, 16.0D, 13.0D),
            Block.box(5.75D, 0.0D, 2.0D, 10.25D, 16.0D, 6.5D)
    );

    public WhiteMonsterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COUNT, 1));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.is(this)) {
            return state.setValue(COUNT, Math.min(3, state.getValue(COUNT) + 1));
        }
        return defaultBlockState();
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return (!context.isSecondaryUseActive()
                && context.getItemInHand().is(asItem())
                && state.getValue(COUNT) < 3)
                || super.canBeReplaced(state, context);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COUNT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(COUNT)) {
            case 2 -> TWO;
            case 3 -> THREE;
            default -> ONE;
        };
    }
}
