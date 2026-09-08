package com.hookahmod.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

public final class KingdomsIntegration {

    private static final String KINGDOMS_MOD_ID = "kingdoms";
    private static final String HOOKAH_INTEGRATION_CLASS =
            "com.geydev.kalfactions.integration.curios.CuriosHookahIntegration";

    private static final Class<?>[] PLAYER = { ServerPlayer.class };
    private static final Class<?>[] PLAYER_CHARGE = { ServerPlayer.class, float.class };
    private static final Class<?>[] PLAYER_POS = { ServerPlayer.class, BlockPos.class };

    private static final KingdomsBridge BRIDGE = new KingdomsBridge(
            () -> ModList.get().isLoaded(KINGDOMS_MOD_ID), HOOKAH_INTEGRATION_CLASS);

    private KingdomsIntegration() {}

    public static float hookahCombatMultiplier(ServerPlayer player) {
        Object value = invoke("combatMultiplier", PLAYER, player);
        return value instanceof Number number && Float.isFinite(number.floatValue())
                ? Math.max(1.0F, number.floatValue()) : 1.0F;
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
        return BRIDGE.permission("canEquipHookah", PLAYER, player);
    }

    public static boolean canMoveHookahBlock(ServerPlayer player, BlockPos pos) {
        return BRIDGE.permission("canMoveHookahBlock", PLAYER_POS, player, pos);
    }

    private static boolean invokeBoolean(String name, boolean fallback, Class<?>[] parameters, Object... args) {
        Object value = invoke(name, parameters, args);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private static Object invoke(String name, Class<?>[] parameters, Object... args) {
        return BRIDGE.invoke(name, parameters, args);
    }
}
