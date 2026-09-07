package com.hookahmod.smoking;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.event.ActiveSessions;
import com.hookahmod.integration.KingdomsIntegration;
import com.hookahmod.item.AbstractTobaccoItem;
import com.hookahmod.item.HookahTier;
import com.hookahmod.item.WornHookah;
import com.hookahmod.smoke.HookahSmoke;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * The rules a smoking session follows, shared by the placed hookah block and
 * the worn one. Everything side-specific lives behind {@link HookahHost}.
 */
public final class HookahSessions {

    private HookahSessions() {}

    /**
     * Claims the mouthpiece for the player, or hands it back when they already
     * hold it. Returns whether the state changed.
     */
    public static boolean toggle(ServerPlayer player, HookahHost host) {
        if (!host.canBeUsedBy(player)) return false;

        UUID active = host.activePlayer();
        if (active != null && !ActiveSessions.server().owns(active, host.sessionKey())) {
            host.release();
            active = null;
        }

        if (!host.hoseType().isPresent()) {
            return refuse(player, "message.hookahmod.install_hose");
        }
        if (!host.inRange(player)) return false;
        if (active != null && !active.equals(player.getUUID())) {
            return refuse(player, "message.hookahmod.busy");
        }
        if (active != null) {
            host.release();
            return true;
        }
        if (!WornHookah.playerHasMouthpiece(player)) {
            return refuse(player, "message.hookahmod.no_mouthpiece");
        }

        host.claim(player);
        return true;
    }

    public static void exhale(ServerPlayer player, HookahHost host, float charge) {
        if (!(player.level() instanceof ServerLevel server)) return;
        if (!ActiveSessions.server().owns(player.getUUID(), host.sessionKey())
                || !host.canBeUsedBy(player)
                || !host.inRange(player)
                || !host.hasAllConsumables()) {
            return;
        }

        ItemStack tobaccoStack = host.consumable(HookahBlockEntity.SLOT_TOBACCO);
        Vector3f smokeColor = tobaccoStack.getItem() instanceof AbstractTobaccoItem tobaccoForSmoke
                ? tobaccoForSmoke.smokeColor()
                : null;
        HookahSmoke.spawnExhaleSmoke(server, host.exhaleOrigin(), player, charge, smokeColor);

        server.playSound(null, host.soundPosition(), SoundEvents.GENERIC_DRINK,
                SoundSource.PLAYERS, 0.15f + charge * 0.4f, 1.6f);

        if (tobaccoStack.getItem() instanceof AbstractTobaccoItem tobacco) {
            HookahTier tier = host.tier();
            IntoxicationState.add(player, IntoxicationState.gain(tobacco.intoxication(), charge));
            tobacco.onExhale(
                    server,
                    player,
                    charge,
                    tier.effectMult(),
                    tier.combatMult() * host.combatMultiplier(player)
            );
        } else {
            IntoxicationState.add(player, IntoxicationState.gain(IntoxicationState.plainTobaccoIntoxication(), charge));
        }

        KingdomsIntegration.onHookahPuff(player, charge);
        if (host.keepsCharge(player)) return;
        host.consumeCharge(player);
    }

    private static boolean refuse(ServerPlayer player, String messageKey) {
        player.displayClientMessage(Component.translatable(messageKey), true);
        return false;
    }
}
