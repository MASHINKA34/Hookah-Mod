package com.hookahmod.gametest;

import com.hookahmod.config.HookahConfig;
import com.hookahmod.effect.ModMobEffects;
import com.hookahmod.smoking.IntoxicationState;
import com.hookahmod.smoking.ModAttachments;
import com.hookahmod.smoking.MouthpiecePosition;
import com.hookahmod.registry.ModItems;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

@GameTestHolder("hookahmod_tests")
@PrefixGameTestTemplate(false)
public class IntoxicationGameTests {
    @GameTest(template = "empty")
    public static void relaxedAndHighHealAtTheVanillaRegenerationCadence(GameTestHelper helper) {
        for (float value : new float[]{HookahConfig.relaxedThreshold, HookahConfig.highThreshold}) {
            FakePlayer player = player(helper);
            player.setHealth(5.0f);
            player.setData(ModAttachments.INTOXICATION, value);
            for (int tick = 0; tick < 120; tick++) {
                if (tick % 20 == 0) IntoxicationState.applyBandEffects(player);
                MobEffectInstance regeneration = player.getEffect(MobEffects.REGENERATION);
                if (regeneration != null && !regeneration.tick(player, () -> {})) {
                    player.removeEffect(MobEffects.REGENERATION);
                }
            }
            helper.assertTrue(player.getHealth() == 8.0f, "Regeneration I must heal every 50 ticks in either band");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hookahDoesNotRefreshOrReplaceAnExistingStrongerRegeneration(GameTestHelper helper) {
        FakePlayer player = player(helper);
        player.setData(ModAttachments.INTOXICATION, HookahConfig.highThreshold);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 1));
        IntoxicationState.applyBandEffects(player);
        helper.assertTrue(player.getEffect(MobEffects.REGENERATION).getDuration() == 30, "Existing duration must be preserved");
        helper.assertTrue(player.getEffect(MobEffects.REGENERATION).getAmplifier() == 1, "Existing amplifier must be preserved");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void abyssTripSurvivesPlayerSaveAndLoad(GameTestHelper helper) {
        FakePlayer original = player(helper);
        original.setData(ModAttachments.INTOXICATION, HookahConfig.tripThreshold);
        original.addEffect(new MobEffectInstance(ModMobEffects.ABYSS_TRIP, 400));
        original.setData(ModAttachments.ABYSS_TRIP_TICKS, 20);
        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        FakePlayer restored = player(helper);
        restored.load(saved);
        helper.assertTrue(restored.getData(ModAttachments.ABYSS_TRIP_TICKS) == 20, "Saved timer must survive loading");
        helper.assertTrue(restored.hasEffect(ModMobEffects.ABYSS_TRIP), "Saved effect must survive loading");
        IntoxicationState.applyBandEffects(restored);
        helper.assertTrue(restored.getData(ModAttachments.ABYSS_TRIP_TICKS) == 19, "Loaded timer must continue");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyAbyssEffectRecoversItsTimerAndClearsWhenSober(GameTestHelper helper) {
        FakePlayer player = player(helper);
        player.setData(ModAttachments.INTOXICATION, HookahConfig.tripThreshold);
        player.addEffect(new MobEffectInstance(ModMobEffects.ABYSS_TRIP, 200));
        IntoxicationState.applyBandEffects(player);
        helper.assertTrue(player.getData(ModAttachments.ABYSS_TRIP_TICKS) == 9, "Legacy timer must recover from effect duration");
        player.setData(ModAttachments.INTOXICATION, 0.0f);
        IntoxicationState.applyBandEffects(player);
        helper.assertTrue(!player.hasEffect(ModMobEffects.ABYSS_TRIP), "Sober players must lose the trip effect");
        helper.assertTrue(player.getData(ModAttachments.ABYSS_TRIP_TICKS) == 0, "Sober players must clear the timer");
        helper.succeed();
    }

    private static FakePlayer player(GameTestHelper helper) {
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "IntoxicationTest"));
    }

    @GameTest(template = "empty")
    public static void hoseFollowsMainArmOffhandAndYawAcrossZero(GameTestHelper helper) {
        FakePlayer player = player(helper);
        player.setPos(0, 0, 0);
        player.setOldPosAndRot();
        player.yBodyRotO = 0;
        player.yBodyRot = 0;
        for (HumanoidArm arm : HumanoidArm.values()) {
            player.setMainArm(arm);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HOOKAH_MOUTHPIECE.get()));
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            double mainX = MouthpiecePosition.hand(player, 1.0f).x;
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.HOOKAH_MOUTHPIECE.get()));
            double offX = MouthpiecePosition.hand(player, 1.0f).x;
            helper.assertTrue(Math.abs(mainX + offX) < 0.0001, "Offhand must mirror the main hand");
            helper.assertTrue(arm == HumanoidArm.RIGHT ? mainX < 0 : mainX > 0, "Hose must follow the configured main arm");
        }
        player.yBodyRotO = 350;
        player.yBodyRot = 10;
        helper.assertTrue(MouthpiecePosition.hand(player, 0.5f).z > 0, "Yaw interpolation must take the short path");
        helper.succeed();
    }
}
