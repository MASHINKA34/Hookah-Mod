package com.hookahmod.block;

import com.hookahmod.item.HookahHoseType;
import com.hookahmod.item.HookahHoseItem;
import com.hookahmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class HookahBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_COAL = BooleanProperty.create("has_coal");

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 1, 12),
            Block.box(5, 1, 5, 11, 7, 11),
            Block.box(5.5, 7, 5.5, 10.5, 8, 10.5),
            Block.box(7, 8, 7, 9, 18, 9),
            Block.box(6, 12, 6, 10, 13, 10),
            Block.box(6, 17, 6, 10, 18, 10),
            Block.box(5.5, 22, 5.5, 10.5, 25, 10.5)
    );

    public static final com.mojang.serialization.MapCodec<HookahBlock> CODEC = simpleCodec(HookahBlock::new);

    public HookahBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_COAL, false));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_COAL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return createTickerHelper(type, ModBlockEntities.HOOKAH.get(),
                    (lvl, pos, st, be) -> be.clientTick(lvl, pos, st));
        }
        return createTickerHelper(type, ModBlockEntities.HOOKAH.get(),
                (lvl, pos, st, be) -> be.serverTick(lvl, pos, st));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HookahBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HookahBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof HookahHoseItem hose) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (be.getHoseType().isPresent()) {
                player.displayClientMessage(Component.translatable("message.hookahmod.hose_already_installed"), true);
                return ItemInteractionResult.CONSUME;
            }
            be.setHoseType(hose.getHoseType());
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return ItemInteractionResult.CONSUME;
        }
        if (stack.getItem() instanceof com.hookahmod.item.HookahMouthpieceItem) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!be.getHoseType().isPresent()) {
                player.displayClientMessage(Component.translatable("message.hookahmod.install_hose"), true);
                return ItemInteractionResult.CONSUME;
            }
            if (be.getActivePlayerUuid() == null) {
                player.displayClientMessage(Component.translatable("message.hookahmod.claim_first"), true);
                return ItemInteractionResult.CONSUME;
            }
            if (!player.getUUID().equals(be.getActivePlayerUuid())) {
                player.displayClientMessage(Component.translatable("message.hookahmod.busy"), true);
                return ItemInteractionResult.CONSUME;
            }
            if (!be.hasAllConsumables()) {
                player.displayClientMessage(Component.translatable("gui.hookahmod.fill_slots"), true);
                return ItemInteractionResult.CONSUME;
            }
            player.startUsingItem(hand);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                   Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HookahBlockEntity be)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
            if (be.getHoseType().isPresent()) {
                if (be.isInUse()) {
                    player.displayClientMessage(Component.translatable("message.hookahmod.cannot_remove_hose_in_use"), true);
                    return net.minecraft.world.InteractionResult.CONSUME;
                }
                be.dropHoseToPlayer(level, pos, player);
                return net.minecraft.world.InteractionResult.CONSUME;
            }
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            sp.openMenu(getMenuProvider(state, level, pos), buf -> buf.writeBlockPos(pos));
        }
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container.hookahmod.hookah");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                return new com.hookahmod.menu.HookahMenu(id, inv, pos);
            }
        };
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof HookahBlockEntity be) {
                be.releaseMouthpiece();
                Containers.dropContents(level, pos, be.getInventory());
            }
            super.onRemove(state, level, pos, newState, moved);
        }
    }
}
