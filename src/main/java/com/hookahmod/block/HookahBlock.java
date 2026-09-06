package com.hookahmod.block;

import com.hookahmod.item.HookahHoseType;
import com.hookahmod.item.HookahHoseItem;
import com.hookahmod.item.HookahMouthpieceItem;
import com.hookahmod.item.HookahTier;
import com.hookahmod.item.TieredHookahItem;
import com.hookahmod.integration.KingdomsIntegration;
import com.hookahmod.registry.ModBlockEntities;
import com.hookahmod.registry.ModBlocks;
import com.hookahmod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

public class HookahBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_COAL = BooleanProperty.create("has_coal");
    public static final EnumProperty<HookahTier> TIER = EnumProperty.create("tier", HookahTier.class);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 1, 12),
            Block.box(5, 1, 5, 11, 7, 11),
            Block.box(5.5, 7, 5.5, 10.5, 8, 10.5),
            Block.box(7, 8, 7, 9, 18, 9),
            Block.box(6, 12, 6, 10, 13, 10),
            Block.box(6, 17, 6, 10, 18, 10),
            Block.box(5.5, 22, 5.5, 10.5, 25, 10.5)
    );

    private static final VoxelShape LUXURY_PREVIEW_SHAPE = Shapes.or(
            Block.box(-0.5, 0, -0.5, 16.5, 12, 16.5),
            Block.box(4, 12, 4, 12, 34, 12),
            Block.box(2, 34, 2, 14, 43, 14)
    );

    public static final com.mojang.serialization.MapCodec<HookahBlock> CODEC = simpleCodec(HookahBlock::new);

    public HookahBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_COAL, false)
                .setValue(TIER, HookahTier.NORMAL));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_COAL, TIER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        HookahTier tier = ctx.getItemInHand().getItem() instanceof TieredHookahItem item ? item.tier() : HookahTier.NORMAL;
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(TIER, tier);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.is(ModBlocks.LUXURY_HOOKAH_PREVIEW.get()) ? LUXURY_PREVIEW_SHAPE : SHAPE;
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
        if (stack.getItem() instanceof HookahMouthpieceItem mouthpiece) {
            if (level.isClientSide) {
                mouthpiece.startSmoking(level, player, hand);
                return ItemInteractionResult.CONSUME;
            }
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
            if (!be.isPlayerInRange(player)) return ItemInteractionResult.FAIL;
            mouthpiece.startSmoking(level, player, hand);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                   Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HookahBlockEntity be)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (player.isShiftKeyDown() && !state.is(ModBlocks.LUXURY_HOOKAH_PREVIEW.get())) {
            if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer sp) || !sp.isAlive() || sp.isSpectator()
                    || !sp.getAbilities().mayBuild || !level.mayInteract(sp, pos)
                    || !KingdomsIntegration.canMoveHookahBlock(sp, pos)
                    || !KingdomsIntegration.canEquipHookah(sp)) {
                player.displayClientMessage(Component.translatable("message.hookahmod.protected_area"), true);
                return net.minecraft.world.InteractionResult.CONSUME;
            }
            if (!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
                player.displayClientMessage(Component.translatable("message.hookahmod.chest_slot_occupied"), true);
                return net.minecraft.world.InteractionResult.CONSUME;
            }

            if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) {
                return net.minecraft.world.InteractionResult.FAIL;
            }
            if (level.getBlockEntity(pos) != be || level.getBlockState(pos) != state
                    || !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
                return net.minecraft.world.InteractionResult.FAIL;
            }

            ItemStack stack = stackForState(state);
            be.saveItemsToStack(stack);
            if (!level.removeBlock(pos, false)) return net.minecraft.world.InteractionResult.FAIL;
            be.clearItemsForPickup();
            player.setItemSlot(EquipmentSlot.CHEST, stack);
            player.displayClientMessage(Component.translatable("message.hookahmod.worn"), true);
            return net.minecraft.world.InteractionResult.CONSUME;
        }
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            sp.openMenu(getMenuProvider(state, level, pos), buf -> {
                buf.writeBoolean(false);
                buf.writeBlockPos(pos);
            });
        }
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    private static ItemStack stackForState(BlockState state) {
        if (state.is(ModBlocks.LUXURY_HOOKAH_PREVIEW.get())) {
            return new ItemStack(ModItems.LUXURY_HOOKAH_PREVIEW.get());
        }
        return switch (state.getValue(TIER)) {
            case LEATHER -> new ItemStack(ModItems.HOOKAH_LEATHER.get());
            case GOLD -> new ItemStack(ModItems.HOOKAH_GOLD.get());
            case IRON -> new ItemStack(ModItems.HOOKAH_IRON.get());
            case DIAMOND -> new ItemStack(ModItems.HOOKAH_DIAMOND.get());
            case NETHERITE -> new ItemStack(ModItems.HOOKAH_NETHERITE.get());
            default -> new ItemStack(ModItems.HOOKAH.get());
        };
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return stackForState(state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof HookahBlockEntity be) {
            for (ItemStack stack : drops) {
                if (stack.getItem() instanceof BlockItem item && item.getBlock() == this) {
                    be.saveItemsToStack(stack);
                }
            }
        }
        return drops;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative()
                && level.getBlockEntity(pos) instanceof HookahBlockEntity be && !be.getInventory().isEmpty()) {
            ItemStack stack = stackForState(state);
            be.saveItemsToStack(stack);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HookahBlockEntity be) {
            be.loadItemsFromStack(stack);
        }
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
            if (!level.isClientSide && !level.restoringBlockSnapshots
                    && level.getBlockEntity(pos) instanceof HookahBlockEntity be) {
                be.releaseMouthpiece();
            }
            super.onRemove(state, level, pos, newState, moved);
        }
    }
}
