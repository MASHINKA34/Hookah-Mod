package com.hookahmod.combat;

import com.hookahmod.integration.KingdomsIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class SmokeBlockChanges {
    private SmokeBlockChanges() {}

    public static boolean canChange(ServerLevel level, ServerPlayer player, BlockPos pos) {
        return !player.isSpectator() && player.getAbilities().mayBuild
                && !level.isOutsideBuildHeight(pos)
                && level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
                && level.mayInteract(player, pos)
                && KingdomsIntegration.canMoveHookahBlock(player, pos);
    }

    public static boolean remove(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (!canChange(level, player, pos) || level.captureBlockSnapshots || level.restoringBlockSnapshots) return false;
        BlockState state = level.getBlockState(pos);
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) return false;
        return level.getBlockState(pos) == state && level.removeBlock(pos, false);
    }

    public static boolean place(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, Direction face) {
        if (!canChange(level, player, pos) || level.captureBlockSnapshots || level.restoringBlockSnapshots) return false;
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        int snapshotCount = level.capturedBlockSnapshots.size();
        boolean changed;
        level.captureBlockSnapshots = true;
        try {
            changed = level.setBlock(pos, state, Block.UPDATE_ALL);
        } finally {
            level.captureBlockSnapshots = false;
            level.capturedBlockSnapshots.subList(snapshotCount, level.capturedBlockSnapshots.size()).clear();
        }
        if (!changed) return false;

        boolean allowed = false;
        try {
            allowed = !EventHooks.onBlockPlace(player, snapshot, face);
        } finally {
            if (!allowed) {
                level.restoringBlockSnapshots = true;
                try {
                    snapshot.restore();
                } finally {
                    level.restoringBlockSnapshots = false;
                }
            }
        }
        if (!allowed) return false;

        BlockState placed = level.getBlockState(pos);
        placed.onPlace(level, pos, snapshot.getState(), false);
        level.markAndNotifyBlock(pos, level.getChunkAt(pos), snapshot.getState(), placed, Block.UPDATE_ALL, 512);
        return true;
    }
}
