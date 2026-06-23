package com.hookahmod.integration;

import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

public final class KingdomsIntegration {
    private static final String KINGDOMS_MOD_ID = "kingdoms";
    private static final String HOOKAH_INTEGRATION_CLASS =
            "com.geydev.kalfactions.integration.curios.CuriosHookahIntegration";
    private static volatile boolean resolved;
    private static volatile Method combatMultiplierMethod;

    public static float hookahCombatMultiplier(ServerPlayer player) {
        Method method = resolveCombatMultiplier();
        if (method == null || player == null) {
            return 1.0F;
        }
        try {
            Object value = method.invoke(null, player);
            return value instanceof Number number ? Math.max(1.0F, number.floatValue()) : 1.0F;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            combatMultiplierMethod = null;
            resolved = true;
            return 1.0F;
        }
    }

    public static float hookahCombatMultiplier(ServerPlayer player, ServerPlayer wearer) {
        return Math.max(hookahCombatMultiplier(player), hookahCombatMultiplier(wearer));
    }

    private static Method resolveCombatMultiplier() {
        if (resolved) {
            return combatMultiplierMethod;
        }
        synchronized (KingdomsIntegration.class) {
            if (resolved) {
                return combatMultiplierMethod;
            }
            if (ModList.get().isLoaded(KINGDOMS_MOD_ID)) {
                try {
                    Class<?> integration = Class.forName(
                            HOOKAH_INTEGRATION_CLASS,
                            false,
                            KingdomsIntegration.class.getClassLoader()
                    );
                    combatMultiplierMethod = integration.getMethod("combatMultiplier", ServerPlayer.class);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    combatMultiplierMethod = null;
                }
            }
            resolved = true;
            return combatMultiplierMethod;
        }
    }

    private KingdomsIntegration() {
    }
}
