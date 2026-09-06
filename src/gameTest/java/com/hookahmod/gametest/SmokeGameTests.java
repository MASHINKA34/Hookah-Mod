package com.hookahmod.gametest;

import com.hookahmod.event.ServerEvents;
import com.hookahmod.smoke.HookahSmoke;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder("hookahmod_tests")
@PrefixGameTestTemplate(false)
public class SmokeGameTests {
    @GameTest(template = "empty")
    public static void smokeDoesNotLoadAnUnloadedOrigin(GameTestHelper helper) throws ReflectiveOperationException {
        ServerLevel level = helper.getLevel();
        BlockPos origin = new BlockPos(1_000_000, 100, 1_000_000);
        helper.assertTrue(!level.hasChunkAt(origin), "Remote test chunk must start unloaded");
        HookahSmoke.clear();
        try {
            exhale(level, origin);
            helper.assertTrue(!level.hasChunkAt(origin), "Room discovery must not load remote chunks");
            helper.assertTrue(rooms().isEmpty(), "An unknown area must not become an enclosed room");
            HookahSmoke.serverTick(level.getServer());
            helper.assertTrue(lingering().isEmpty(), "Lingering smoke must be removed from unloaded chunks");
            helper.succeed();
        } finally {
            HookahSmoke.clear();
        }
    }

    @GameTest(template = "empty")
    public static void unloadedNeighborIsAnUnknownBoundaryNotARoomWall(GameTestHelper helper) throws ReflectiveOperationException {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos missing = new BlockPos((base.getX() & ~15) + 16, 200, base.getZ());
        for (int i = 0; i < 128 && level.hasChunkAt(missing); i++) missing = missing.offset(16, 0, 0);
        helper.assertTrue(!level.hasChunkAt(missing) && level.hasChunkAt(missing.west()), "Test must straddle a loaded chunk boundary");
        BlockPos center = missing.west();
        Map<BlockPos, BlockState> previous = new HashMap<>();
        HookahSmoke.clear();
        try {
            for (BlockPos cursor : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
                if (!level.hasChunkAt(cursor)) continue;
                BlockPos pos = cursor.immutable();
                previous.put(pos, level.getBlockState(pos));
                level.setBlock(pos, pos.equals(center) ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
            helper.assertTrue(!level.hasChunkAt(missing), "Room fixture must keep the neighboring chunk unloaded");
            exhale(level, center);
            helper.assertTrue(!level.hasChunkAt(missing), "Flood fill must not load neighboring chunks");
            helper.assertTrue(rooms().isEmpty(), "An unloaded boundary must not count as a solid wall");
            helper.succeed();
        } finally {
            previous.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
            HookahSmoke.clear();
        }
    }

    @GameTest(template = "empty")
    public static void serverShutdownClearsSmokeAndRoomAccumulation(GameTestHelper helper) throws ReflectiveOperationException {
        HookahSmoke.clear();
        try {
            BlockPos center = new BlockPos(2, 2, 2);
            for (BlockPos pos : BlockPos.betweenClosed(1, 1, 1, 3, 3, 3)) {
                helper.setBlock(pos, pos.equals(center) ? Blocks.AIR : Blocks.STONE);
            }
            BlockPos absolute = helper.absolutePos(center);
            for (int i = 0; i < 5; i++) exhale(helper.getLevel(), absolute);
            helper.assertTrue(rooms().size() == 1 && lingering().size() == 5, "Loaded enclosed room must accumulate smoke");
            ServerEvents.onServerStopping(new ServerStoppingEvent(helper.getLevel().getServer()));
            helper.assertTrue(rooms().isEmpty() && lingering().isEmpty(), "Server shutdown must clear all smoke");
            helper.assertTrue((int) field(HookahSmoke.class, null, "roomPhaseCounter") == 0, "Server shutdown must reset room scheduling");
            HookahSmoke.serverTick(helper.getLevel().getServer());
            exhale(helper.getLevel(), absolute);
            helper.assertTrue(rooms().size() == 1, "Smoke must work again after reset");
            Object room = rooms().values().iterator().next();
            helper.assertTrue((int) field(room.getClass(), room, "puffs") == 1, "New room must not inherit accumulated puffs");
            helper.succeed();
        } finally {
            HookahSmoke.clear();
        }
    }

    private static void exhale(ServerLevel level, BlockPos origin) {
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "SmokeTest"));
        player.setYRot(0);
        player.setXRot(0);
        player.setPos(origin.getX() + 0.5, origin.getY() + 0.58 - player.getEyeHeight(), origin.getZ());
        HookahSmoke.spawnExhaleSmoke(level, Vec3.atCenterOf(origin), player, 1);
    }

    private static Map<?, ?> rooms() throws ReflectiveOperationException {
        return (Map<?, ?>) field(HookahSmoke.class, null, "ROOM_SMOKE");
    }

    private static List<?> lingering() throws ReflectiveOperationException {
        return (List<?>) field(HookahSmoke.class, null, "LINGERING_SMOKE");
    }

    private static Object field(Class<?> type, Object instance, String name) throws ReflectiveOperationException {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }
}
