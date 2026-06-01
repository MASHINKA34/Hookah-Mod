package com.hookahmod.smoke;

import com.hookahmod.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HookahSmoke {

    private static final int PARTICLE_RANGE = 128;
    private static final int OPEN_LINGER_TICKS = 45;
    private static final int MAX_ROOM_DISTANCE = 64;
    private static final int MAX_ROOM_AIR_BLOCKS = 8192;
    private static final int MAX_ROOM_CLOUDS = 128;
    private static final int ROOM_MIN_PUFFS = 5;
    private static final int ROOM_LINGER_TICKS = 20 * 30;
    private static final int ROOM_RECHECK_TICKS = 20;
    private static final float ROOM_MAX_DENSITY = 14.0f;

    private static final List<LingeringSmoke> LINGERING_SMOKE = new ArrayList<>();
    private static final Map<RoomKey, RoomSmoke> ROOM_SMOKE = new HashMap<>();
    private static int roomPhaseCounter = 0;

    private HookahSmoke() {}

    public static float chargeStrength(float charge) {
        float clamped = Mth.clamp(charge, 0.0f, 1.0f);
        return clamped * (0.25f + clamped * 0.75f);
    }

    public static int clientMouthPuffs(float charge) {
        return 6 + (int) (chargeStrength(charge) * 30.0f);
    }

    public static float clientMouthSpeed(float charge) {
        return 0.018f + chargeStrength(charge) * 0.045f;
    }

    public static void spawnExhaleSmoke(ServerLevel level, Vec3 hookahPoint, ServerPlayer player, float charge) {
        spawnExhaleSmoke(level, hookahPoint, player, charge, null);
    }

    public static void spawnExhaleSmoke(ServerLevel level, Vec3 hookahPoint, ServerPlayer player, float charge, @Nullable Vector3f color) {
        float strength = chargeStrength(charge);

        int hookahPuffs = 3 + (int) (strength * 10.0f);
        sendSmoke(level, hookahPoint.x, hookahPoint.y, hookahPoint.z,
                0.12f, 0.05f, 0.12f, 0.018f, hookahPuffs, color);

        Vec3 look = player.getLookAngle();
        Vec3 mouthPoint = new Vec3(
                player.getX() + look.x * 0.5,
                player.getY() + player.getEyeHeight() - 0.08,
                player.getZ() + look.z * 0.5
        );

        int mouthPuffs = 8 + (int) (strength * 44.0f);
        float spread = 0.10f + strength * 0.30f;
        float speed = 0.018f + strength * 0.055f;
        sendSmoke(level, mouthPoint.x, mouthPoint.y, mouthPoint.z,
                spread, 0.07f, spread, speed, mouthPuffs, color);

        LINGERING_SMOKE.add(new LingeringSmoke(level.dimension(), mouthPoint, look, strength, OPEN_LINGER_TICKS, color));
        accumulateRoomSmoke(level, BlockPos.containing(mouthPoint.x, mouthPoint.y, mouthPoint.z), strength, color);
    }

    public static void serverTick(MinecraftServer server) {
        tickLingeringSmoke(server);
        tickRoomSmoke(server);
    }

    private static void tickLingeringSmoke(MinecraftServer server) {
        Iterator<LingeringSmoke> iterator = LINGERING_SMOKE.iterator();
        while (iterator.hasNext()) {
            LingeringSmoke smoke = iterator.next();
            ServerLevel level = server.getLevel(smoke.dimension);
            if (level == null || smoke.remainingTicks-- <= 0) {
                iterator.remove();
                continue;
            }

            smoke.position = smoke.position.add(smoke.direction.scale(0.035)).add(0.0, 0.012, 0.0);
            if (smoke.remainingTicks % 4 == 0) {
                int count = 1 + (int) (smoke.strength * 4.0f);
                float spread = 0.10f + smoke.strength * 0.14f;
                sendSmoke(level, smoke.position.x, smoke.position.y, smoke.position.z,
                        spread, 0.05f, spread, 0.006f + smoke.strength * 0.012f, count, smoke.color);
            }
        }
    }

    private static void accumulateRoomSmoke(ServerLevel level, BlockPos origin, float strength, @Nullable Vector3f color) {
        // Fast path: the puff happened inside a room we are already tracking, so
        // reuse it instead of running another flood fill. The periodic recheck in
        // tickRoomSmoke still re-validates the room, so staleness stays bounded.
        RoomSmoke active = findActiveRoomContaining(level.dimension(), origin);
        if (active != null) {
            applyPuffToRoom(level, active, strength, color);
            return;
        }

        // Slow path: discover the enclosing room with a one-off flood fill.
        RoomProbe probe = findEnclosedRoom(level, origin);
        if (probe == null) return;
        if (!ROOM_SMOKE.containsKey(probe.key) && ROOM_SMOKE.size() >= MAX_ROOM_CLOUDS) return;

        RoomSmoke smoke = ROOM_SMOKE.computeIfAbsent(probe.key, key -> new RoomSmoke(key, probe.airBlocks));
        smoke.airBlocks = probe.airBlocks;
        smoke.origin = probe.start;
        applyPuffToRoom(level, smoke, strength, color);
    }

    private static RoomSmoke findActiveRoomContaining(ResourceKey<Level> dimension, BlockPos pos) {
        for (RoomSmoke smoke : ROOM_SMOKE.values()) {
            RoomKey k = smoke.key;
            if (!k.dimension.equals(dimension)) continue;
            // Cheap bounding-box reject before the (rarely reached) list scan.
            if (pos.getX() < k.min.getX() || pos.getX() > k.max.getX()
                    || pos.getY() < k.min.getY() || pos.getY() > k.max.getY()
                    || pos.getZ() < k.min.getZ() || pos.getZ() > k.max.getZ()) {
                continue;
            }
            if (smoke.airBlocks.contains(pos)) return smoke;
        }
        return null;
    }

    private static void applyPuffToRoom(ServerLevel level, RoomSmoke smoke, float strength, @Nullable Vector3f color) {
        smoke.puffs = Math.min(ROOM_MIN_PUFFS, smoke.puffs + 1);
        smoke.remainingTicks = ROOM_LINGER_TICKS;
        smoke.color = copyColor(color);
        if (smoke.puffs < ROOM_MIN_PUFFS) return;

        smoke.density = Math.min(ROOM_MAX_DENSITY, smoke.density + 0.55f + strength * 1.45f);
        smoke.ticks = 0;
        spawnRoomSmoke(level, smoke, true);
    }

    private static void tickRoomSmoke(MinecraftServer server) {
        Iterator<Map.Entry<RoomKey, RoomSmoke>> iterator = ROOM_SMOKE.entrySet().iterator();
        while (iterator.hasNext()) {
            RoomSmoke smoke = iterator.next().getValue();
            ServerLevel level = server.getLevel(smoke.key.dimension);
            if (level == null) {
                iterator.remove();
                continue;
            }

            smoke.ticks++;
            smoke.age++;
            smoke.remainingTicks--;
            if (smoke.remainingTicks <= 0 || smoke.airBlocks.isEmpty()) {
                iterator.remove();
                continue;
            }

            // Re-validate on a fixed cadence regardless of how often the room is
            // puffed into, staggered per-room (via phase) so the flood fills
            // spread across ticks instead of all landing on the same one.
            if ((smoke.age + smoke.phase) % ROOM_RECHECK_TICKS == 0 && !isStillSameClosedRoom(level, smoke)) {
                iterator.remove();
                continue;
            }

            if (smoke.puffs >= ROOM_MIN_PUFFS && smoke.ticks % 5 == 0) {
                spawnRoomSmoke(level, smoke, false);
            }
        }
    }

    private static boolean isStillSameClosedRoom(ServerLevel level, RoomSmoke smoke) {
        RoomProbe probe = findEnclosedRoom(level, smoke.origin);
        if (probe == null || !probe.key.equals(smoke.key)) return false;

        smoke.airBlocks = probe.airBlocks;
        smoke.origin = probe.start;
        return true;
    }

    private static void spawnRoomSmoke(ServerLevel level, RoomSmoke smoke, boolean immediate) {
        RandomSource random = level.random;
        int bursts = Mth.clamp(1 + (int) (smoke.density * (immediate ? 1.0f : 1.35f)), 1, 18);
        float spread = 0.18f + Math.min(smoke.density, 8.0f) * 0.015f;

        for (int i = 0; i < bursts; i++) {
            BlockPos pos = smoke.airBlocks.get(random.nextInt(smoke.airBlocks.size()));
            if (!canSmokeOccupy(level, pos)) continue;

            double x = pos.getX() + 0.18 + random.nextDouble() * 0.64;
            double y = pos.getY() + 0.15 + random.nextDouble() * 0.70;
            double z = pos.getZ() + 0.18 + random.nextDouble() * 0.64;
            sendSmoke(level, x, y, z, spread, 0.08f, spread, 0.004f, 1, smoke.color);
        }
    }

    private static RoomProbe findEnclosedRoom(ServerLevel level, BlockPos origin) {
        BlockPos start = findSmokeStart(level, origin);
        if (start == null) return null;
        if (hasVerticalLeak(level, start)) return null;

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> airBlocks = new ArrayList<>();
        BlockPos min = start;
        BlockPos max = start;

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            airBlocks.add(current);

            min = new BlockPos(
                    Math.min(min.getX(), current.getX()),
                    Math.min(min.getY(), current.getY()),
                    Math.min(min.getZ(), current.getZ()));
            max = new BlockPos(
                    Math.max(max.getX(), current.getX()),
                    Math.max(max.getY(), current.getY()),
                    Math.max(max.getZ(), current.getZ()));

            if (airBlocks.size() > MAX_ROOM_AIR_BLOCKS) return null;

            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (isOutsideProbeBounds(level, origin, next)) return null;
                if (!canSmokeOccupy(level, next) || !visited.add(next)) continue;
                queue.add(next);
            }
        }

        RoomKey key = new RoomKey(level.dimension(), min.immutable(), max.immutable());
        return new RoomProbe(key, airBlocks, start);
    }

    private static BlockPos findSmokeStart(ServerLevel level, BlockPos origin) {
        if (canSmokeOccupy(level, origin)) return origin.immutable();

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (canSmokeOccupy(level, candidate)) return candidate.immutable();
                }
            }
        }
        return null;
    }

    private static boolean hasVerticalLeak(ServerLevel level, BlockPos start) {
        int maxY = Math.min(level.getMaxBuildHeight() - 1, start.getY() + MAX_ROOM_DISTANCE);
        for (int y = start.getY() + 1; y <= maxY; y++) {
            if (!canSmokeOccupy(level, new BlockPos(start.getX(), y, start.getZ()))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isOutsideProbeBounds(ServerLevel level, BlockPos origin, BlockPos pos) {
        return pos.getY() < level.getMinBuildHeight()
                || pos.getY() >= level.getMaxBuildHeight()
                || Math.abs(pos.getX() - origin.getX()) > MAX_ROOM_DISTANCE
                || Math.abs(pos.getY() - origin.getY()) > MAX_ROOM_DISTANCE
                || Math.abs(pos.getZ() - origin.getZ()) > MAX_ROOM_DISTANCE;
    }

    private static boolean canSmokeOccupy(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return false;
        if (state.isAir() || state.getCollisionShape(level, pos).isEmpty()) return true;
        if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN)) return true;
        if (state.getBlock() instanceof TrapDoorBlock && state.getValue(TrapDoorBlock.OPEN)) return true;
        return state.getBlock() instanceof FenceGateBlock && state.getValue(FenceGateBlock.OPEN);
    }

    private static void sendSmoke(ServerLevel level, double x, double y, double z,
                                  float xSpread, float ySpread, float zSpread, float speed, int count) {
        sendSmoke(level, x, y, z, xSpread, ySpread, zSpread, speed, count, null);
    }

    private static void sendSmoke(ServerLevel level, double x, double y, double z,
                                  float xSpread, float ySpread, float zSpread, float speed, int count,
                                  @Nullable Vector3f color) {
        ParticleOptions particle = color == null
                ? ParticleTypes.CAMPFIRE_COSY_SMOKE
                : ColorParticleOption.create(ModParticles.COLORED_HOOKAH_SMOKE.get(), color.x(), color.y(), color.z());
        var packet = new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(
                particle, true,
                x, y, z, xSpread, ySpread, zSpread, speed, count);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= PARTICLE_RANGE * PARTICLE_RANGE) {
                player.connection.send(packet);
            }
        }
    }

    @Nullable
    private static Vector3f copyColor(@Nullable Vector3f color) {
        return color == null ? null : new Vector3f(color);
    }

    private static final class LingeringSmoke {
        private final ResourceKey<Level> dimension;
        private final Vec3 direction;
        private final float strength;
        @Nullable
        private final Vector3f color;
        private Vec3 position;
        private int remainingTicks;

        private LingeringSmoke(ResourceKey<Level> dimension, Vec3 position, Vec3 direction,
                               float strength, int remainingTicks, @Nullable Vector3f color) {
            this.dimension = dimension;
            this.position = position;
            this.direction = direction.normalize();
            this.strength = strength;
            this.remainingTicks = remainingTicks;
            this.color = copyColor(color);
        }
    }

    private record RoomKey(ResourceKey<Level> dimension, BlockPos min, BlockPos max) {}

    private record RoomProbe(RoomKey key, List<BlockPos> airBlocks, BlockPos start) {}

    private static final class RoomSmoke {
        private final RoomKey key;
        private final int phase;
        private BlockPos origin;
        private List<BlockPos> airBlocks;
        private float density;
        @Nullable
        private Vector3f color;
        private int puffs;
        private int remainingTicks = ROOM_LINGER_TICKS;
        private int ticks;
        private int age;

        private RoomSmoke(RoomKey key, List<BlockPos> airBlocks) {
            this.key = key;
            this.airBlocks = airBlocks;
            this.origin = key.min;
            this.phase = roomPhaseCounter;
            roomPhaseCounter = (roomPhaseCounter + 1) % ROOM_RECHECK_TICKS;
        }
    }
}
