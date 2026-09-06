package com.hookahmod.item;

import com.hookahmod.integration.KingdomsIntegration;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class HookahBlockItem extends BlockItem implements Equipable, TieredHookahItem {

    private final HookahTier tier;

    public HookahBlockItem(Block block, Properties properties) {
        this(block, properties, HookahTier.NORMAL);
    }

    public HookahBlockItem(Block block, Properties properties, HookahTier tier) {
        super(block, properties);
        this.tier = tier;
    }

    @Override
    public HookahTier tier() {
        return tier;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean selected) {
        if (!level.isClientSide) WornHookah.clearStaleSession(stack);
    }

    @Override
    public String getDescriptionId() {
        return "item.hookahmod." + tier.itemId();
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && !KingdomsIntegration.canEquipHookah(serverPlayer)) {
            player.displayClientMessage(Component.translatable("message.hookahmod.protected_area"), true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return swapWithEquipmentSlot(this, level, player, hand);
    }
}
