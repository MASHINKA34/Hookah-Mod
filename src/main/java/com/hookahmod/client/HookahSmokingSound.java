package com.hookahmod.client;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.HookahMouthpieceItem;
import com.hookahmod.item.WornHookah;
import com.hookahmod.registry.ModItems;
import com.hookahmod.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class HookahSmokingSound extends AbstractTickableSoundInstance {

    private final Player player;

    public HookahSmokingSound(Player player) {
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
        if (!shouldPlayFor(player)) {
            stop();
            return;
        }
        updatePosition();
    }

    public void halt() {
        stop();
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

    public static boolean shouldPlayFor(Player player) {
        if (player == null || player.isRemoved() || !player.isAlive() || !player.isUsingItem()) return false;
        ItemStack stack = player.getUseItem();
        return stack.is(ModItems.HOOKAH_MOUTHPIECE.get());
    }

    /**
     * Every player draws audibly, not just the local one. The used-item slot is
     * already synchronised for remote players, so bystanders can be served from
     * the client without a packet of our own.
     */
    public static void tickLocal() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            HookahSmokingSoundController.stopAll(mc);
            return;
        }
        HookahSmokingSoundController.tick(mc);
    }
}

final class HookahSmokingSoundController {

    private static final Map<UUID, HookahSmokingSound> ACTIVE = new HashMap<>();

    private HookahSmokingSoundController() {}

    static void tick(Minecraft mc) {
        Iterator<Map.Entry<UUID, HookahSmokingSound>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            HookahSmokingSound sound = iterator.next().getValue();
            if (!sound.isStopped()) continue;
            mc.getSoundManager().stop(sound);
            iterator.remove();
        }

        if (mc.level == null) return;
        for (Player player : mc.level.players()) {
            if (!HookahSmokingSound.shouldPlayFor(player)) continue;
            if (ACTIVE.containsKey(player.getUUID())) continue;
            HookahSmokingSound sound = new HookahSmokingSound(player);
            ACTIVE.put(player.getUUID(), sound);
            mc.getSoundManager().play(sound);
        }
    }

    static void stopAll(Minecraft mc) {
        if (ACTIVE.isEmpty()) return;
        for (HookahSmokingSound sound : ACTIVE.values()) {
            sound.halt();
            mc.getSoundManager().stop(sound);
        }
        ACTIVE.clear();
    }
}
