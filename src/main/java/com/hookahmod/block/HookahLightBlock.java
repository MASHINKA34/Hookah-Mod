package com.hookahmod.block;

import com.hookahmod.item.WornHookah;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class HookahLightBlock extends LightBlock {
    public static final MapCodec<LightBlock> CODEC = simpleCodec(HookahLightBlock::new);
    public static final int LIGHT_LEVEL = 8;
    public static final int CHECK_INTERVAL = 20;

    public HookahLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEVEL, LIGHT_LEVEL));
    }

    @Override
    public MapCodec<LightBlock> codec() {
        return CODEC;
    }

    public static BlockPos lightPosition(Player player) {
        return BlockPos.containing(player.getX(), player.getY() + player.getBbHeight() * 0.72D, player.getZ());
    }

    public static boolean hasLightSource(Player player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        return player.isAlive() && !player.isSpectator() && WornHookah.isHookahStack(stack) && WornHookah.hasCoal(stack);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (Player player : level.players()) {
            if (hasLightSource(player) && pos.equals(lightPosition(player))) {
                level.scheduleTick(pos, this, CHECK_INTERVAL);
                return;
            }
        }
        level.removeBlock(pos, false);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
