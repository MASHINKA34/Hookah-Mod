package com.hookahmod.integration;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

public final class KingdomsIntegration {
    private static final String KINGDOMS_MOD_ID = "kingdoms";
    private static final String HOOKAH_INTEGRATION_CLASS =
            "com.geydev.kalfactions.integration.curios.CuriosHookahIntegration";
    private static volatile Class<?> integrationClass;
    private static volatile boolean classResolved;
    private static volatile Method combatMultiplierMethod;
    private static volatile Method hasHookahBonusMethod;
    private static volatile Method canEquipHookahMethod;
    private static volatile Method canMoveHookahBlockMethod;
    private static volatile boolean combatMultiplierResolved;
    private static volatile boolean hasHookahBonusResolved;
    private static volatile boolean canEquipHookahResolved;
    private static volatile boolean canMoveHookahBlockResolved;

    public static float hookahCombatMultiplier(ServerPlayer player) {
        Method method = resolveMethod("combatMultiplier", ServerPlayer.class);
        if (method == null || player == null) {
            return 1.0F;
        }
        try {
            Object value = method.invoke(null, player);
            return value instanceof Number number ? Math.max(1.0F, number.floatValue()) : 1.0F;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            combatMultiplierMethod = null;
            return 1.0F;
        }
    }

    public static float hookahCombatMultiplier(ServerPlayer player, ServerPlayer wearer) {
        return Math.max(hookahCombatMultiplier(player), hookahCombatMultiplier(wearer));
    }

    public static boolean hasHookahBonus(ServerPlayer player) {
        return invokeBoolean(resolveMethod("hasHookahBonus", ServerPlayer.class), false, player);
    }

    public static boolean canEquipHookah(ServerPlayer player) {
        return invokeBoolean(resolveMethod("canEquipHookah", ServerPlayer.class), true, player);
    }

    public static boolean canMoveHookahBlock(ServerPlayer player, BlockPos pos) {
        return invokeBoolean(resolveMethod("canMoveHookahBlock", ServerPlayer.class, BlockPos.class), true, player, pos);
    }

    private static boolean invokeBoolean(Method method, boolean fallback, Object... args) {
        if (method == null) {
            return fallback;
        }
        for (Object arg : args) {
            if (arg == null) {
                return fallback;
            }
        }
        try {
            Object value = method.invoke(null, args);
            return value instanceof Boolean bool ? bool : fallback;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (method == hasHookahBonusMethod) {
                hasHookahBonusMethod = null;
            } else if (method == canEquipHookahMethod) {
                canEquipHookahMethod = null;
            } else if (method == canMoveHookahBlockMethod) {
                canMoveHookahBlockMethod = null;
            }
            return fallback;
        }
    }

    private static Method resolveMethod(String name, Class<?>... parameters) {
        if (methodResolved(name)) {
            return cachedMethod(name);
        }
        Class<?> integration = resolveIntegrationClass();
        if (integration == null) {
            cacheMethod(name, null);
            return null;
        }
        try {
            Method method = integration.getMethod(name, parameters);
            cacheMethod(name, method);
            return method;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            cacheMethod(name, null);
            return null;
        }
    }

    private static Method cachedMethod(String name) {
        return switch (name) {
            case "combatMultiplier" -> combatMultiplierMethod;
            case "hasHookahBonus" -> hasHookahBonusMethod;
            case "canEquipHookah" -> canEquipHookahMethod;
            case "canMoveHookahBlock" -> canMoveHookahBlockMethod;
            default -> null;
        };
    }

    private static boolean methodResolved(String name) {
        return switch (name) {
            case "combatMultiplier" -> combatMultiplierResolved;
            case "hasHookahBonus" -> hasHookahBonusResolved;
            case "canEquipHookah" -> canEquipHookahResolved;
            case "canMoveHookahBlock" -> canMoveHookahBlockResolved;
            default -> true;
        };
    }

    private static void cacheMethod(String name, Method method) {
        switch (name) {
            case "combatMultiplier" -> {
                combatMultiplierMethod = method;
                combatMultiplierResolved = true;
            }
            case "hasHookahBonus" -> {
                hasHookahBonusMethod = method;
                hasHookahBonusResolved = true;
            }
            case "canEquipHookah" -> {
                canEquipHookahMethod = method;
                canEquipHookahResolved = true;
            }
            case "canMoveHookahBlock" -> {
                canMoveHookahBlockMethod = method;
                canMoveHookahBlockResolved = true;
            }
            default -> {
            }
        }
    }

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

    private KingdomsIntegration() {
    }
}
