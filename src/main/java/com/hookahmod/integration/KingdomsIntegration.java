package com.hookahmod.integration;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class KingdomsIntegration {

    private static final String KINGDOMS_MOD_ID = "kingdoms";
    private static final String HOOKAH_INTEGRATION_CLASS =
            "com.geydev.kalfactions.integration.curios.CuriosHookahIntegration";

    private static final Class<?>[] PLAYER = { ServerPlayer.class };
    private static final Class<?>[] PLAYER_CHARGE = { ServerPlayer.class, float.class };
    private static final Class<?>[] PLAYER_POS = { ServerPlayer.class, BlockPos.class };

    private static final Map<String, Optional<Method>> METHODS = new ConcurrentHashMap<>();

    private static volatile Class<?> integrationClass;
    private static volatile boolean classResolved;

    private KingdomsIntegration() {}

    public static float hookahCombatMultiplier(ServerPlayer player) {
        Object value = invoke("combatMultiplier", PLAYER, player);
        return value instanceof Number number ? Math.max(1.0F, number.floatValue()) : 1.0F;
    }

    public static float hookahCombatMultiplier(ServerPlayer player, ServerPlayer wearer) {
        return Math.max(hookahCombatMultiplier(player), hookahCombatMultiplier(wearer));
    }

    public static boolean hasHookahBonus(ServerPlayer player) {
        return invokeBoolean("hasHookahBonus", false, PLAYER, player);
    }

    public static boolean hasHookahMastery(ServerPlayer player) {
        return invokeBoolean("hasHookahMastery", false, PLAYER, player);
    }

    public static boolean hasHookahMastery(ServerPlayer player, ServerPlayer wearer) {
        return hasHookahMastery(player) || hasHookahMastery(wearer);
    }

    public static void onHookahPuff(ServerPlayer player, float charge) {
        invoke("onHookahPuff", PLAYER_CHARGE, player, charge);
    }

    public static boolean canEquipHookah(ServerPlayer player) {
        return invokeBoolean("canEquipHookah", true, PLAYER, player);
    }

    public static boolean canMoveHookahBlock(ServerPlayer player, BlockPos pos) {
        return invokeBoolean("canMoveHookahBlock", true, PLAYER_POS, player, pos);
    }

    private static boolean invokeBoolean(String name, boolean fallback, Class<?>[] parameters, Object... args) {
        Object value = invoke(name, parameters, args);
        return value instanceof Boolean bool ? bool : fallback;
    }

    @Nullable
    private static Object invoke(String name, Class<?>[] parameters, Object... args) {
        for (Object arg : args) {
            if (arg == null) return null;
        }
        Method method = resolveMethod(name, parameters);
        if (method == null) return null;
        try {
            return method.invoke(null, args);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            METHODS.put(name, Optional.empty());
            return null;
        }
    }

    @Nullable
    private static Method resolveMethod(String name, Class<?>[] parameters) {
        return METHODS.computeIfAbsent(name, key -> {
            Class<?> integration = resolveIntegrationClass();
            if (integration == null) return Optional.empty();
            try {
                return Optional.of(integration.getMethod(key, parameters));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    @Nullable
    private static Class<?> resolveIntegrationClass() {
        if (classResolved) {
            return integrationClass;
        }
        synchronized (KingdomsIntegration.class) {
            if (classResolved) {
                return integrationClass;
            }
            if (ModList.get().isLoaded(KINGDOMS_MOD_ID)) {
                try {
                    integrationClass = Class.forName(
                            HOOKAH_INTEGRATION_CLASS,
                            false,
                            KingdomsIntegration.class.getClassLoader()
                    );
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    integrationClass = null;
                }
            }
            classResolved = true;
            return integrationClass;
        }
    }
}
