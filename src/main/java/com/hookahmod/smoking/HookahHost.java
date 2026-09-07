package com.hookahmod.smoking;

import com.hookahmod.block.HookahBlockEntity;
import com.hookahmod.item.HookahHoseType;
import com.hookahmod.item.HookahTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.IntFunction;

/**
 * A hookah a player can attach to, either standing in the world or worn on
 * someone's back. {@link HookahSessions} drives claiming and exhaling through
 * this interface so both carriers share one implementation of the rules.
 */
public interface HookahHost {

    /** Identity {@code ActiveSessions} keys ownership on. */
    Object sessionKey();

    HookahHoseType hoseType();

    HookahTier tier();

    @Nullable
    UUID activePlayer();

    ItemStack consumable(int slot);

    boolean hasAllConsumables();

    /** Whether this hookah still exists and both sides are in a usable state. */
    boolean canBeUsedBy(ServerPlayer player);

    boolean inRange(Player player);

    /** Where the hookah itself puffs from. */
    Vec3 exhaleOrigin();

    BlockPos soundPosition();

    float combatMultiplier(ServerPlayer smoker);

    /** Kingdoms mastery lets a smoker draw without burning the charge. */
    boolean keepsCharge(ServerPlayer smoker);

    void consumeCharge(ServerPlayer smoker);

    void claim(ServerPlayer player);

    void release();

    static boolean hasAllConsumables(IntFunction<ItemStack> slots) {
        return !slots.apply(HookahBlockEntity.SLOT_TOBACCO).isEmpty()
                && !slots.apply(HookahBlockEntity.SLOT_COAL).isEmpty()
                && !slots.apply(HookahBlockEntity.SLOT_WATER).isEmpty();
    }
}
