package com.hookahmod.client;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.HookahMouthpieceItem;
import com.hookahmod.item.WornHookah;
import com.hookahmod.registry.ModItems;
import com.hookahmod.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class HookahSmokingSound extends AbstractTickableSoundInstance {

    private final LocalPlayer player;

    public HookahSmokingSound(LocalPlayer player) {
        super(ModSounds.SMOKING.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.9F;
        this.pitch = 1.0F;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        updatePosition();
    }

    @Override
    public void tick() {
        if (!shouldKeepPlaying()) {
            stop();
            return;
        }
        updatePosition();
    }

    public void halt() {
        stop();
    }

    private boolean shouldKeepPlaying() {
        return !player.isRemoved()
                && player.isUsingItem()
                && player.getUseItem().is(ModItems.HOOKAH_MOUTHPIECE.get());
    }

    private void updatePosition() {
        HookahBlockEntity be = HookahMouthpieceItem.findClaimedHookah(player, player.level());
        if (be != null) {
            Vec3 pos = Vec3.atCenterOf(be.getBlockPos());
            x = pos.x;
            y = pos.y;
            z = pos.z;
            return;
        }

        Player wearer = WornHookah.findClaimedWearer(player, player.level());
        if (wearer != null) {
            x = wearer.getX();
            y = wearer.getY() + wearer.getBbHeight() * 0.72D;
            z = wearer.getZ();
            return;
        }

        x = player.getX();
        y = player.getY() + player.getEyeHeight();
        z = player.getZ();
    }

    public static boolean shouldPlayFor(LocalPlayer player) {
        if (player == null || !player.isUsingItem()) return false;
        ItemStack stack = player.getUseItem();
        return stack.is(ModItems.HOOKAH_MOUTHPIECE.get());
    }

    public static void tickLocal() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            HookahSmokingSoundController.stop(mc);
            return;
        }
        HookahSmokingSoundController.tick(mc, mc.player);
    }
}

final class HookahSmokingSoundController {

    private static HookahSmokingSound current;

    private HookahSmokingSoundController() {}

    static void tick(Minecraft mc, LocalPlayer player) {
        if (HookahSmokingSound.shouldPlayFor(player)) {
            if (current == null || current.isStopped()) {
                current = new HookahSmokingSound(player);
                mc.getSoundManager().play(current);
            }
        } else {
            stop(mc);
        }
    }

    static void stop(Minecraft mc) {
        if (current == null) return;
        current.halt();
        mc.getSoundManager().stop(current);
        current = null;
    }
}
