package com.hookahmod.item;

import com.hookahmod.registry.ModSounds;
import com.hookahmod.registry.ModParticles;
import com.hookahmod.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class WhiteMonsterItem extends BlockItem {

    public WhiteMonsterItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public String getDescriptionId() {
        return "item.hookahmod.white_monster";
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = stack;
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack emptyCan = new ItemStack(ModItems.EMPTY_WHITE_MONSTER.get());
            if (stack.isEmpty()) {
                result = emptyCan;
            } else if (!player.getInventory().add(emptyCan)) {
                player.drop(emptyCan, false);
            }
        }
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer player) {
            Vec3 look = player.getLookAngle();
            Vec3 mouth = player.getEyePosition().add(look.scale(0.35D)).add(0.0D, -0.18D, 0.0D);
            for (ServerPlayer listener : server.players()) {
                listener.playNotifySound(ModSounds.WHITE_MONSTER_BURP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            Vec3 referenceUp = Math.abs(look.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 right = look.cross(referenceUp).normalize();
            Vec3 coneUp = right.cross(look).normalize();
            for (int i = 0; i < 64; i++) {
                double radius = Math.sqrt(player.getRandom().nextDouble()) * 0.32D;
                double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
                Vec3 direction = look
                        .add(right.scale(Math.cos(angle) * radius))
                        .add(coneUp.scale(Math.sin(angle) * radius))
                        .normalize();
                double speed = 0.035D + player.getRandom().nextDouble() * 0.045D;
                server.sendParticles(
                        ModParticles.SOAP_BUBBLE.get(),
                        mouth.x,
                        mouth.y,
                        mouth.z,
                        0,
                        direction.x * speed,
                        direction.y * speed + 0.006D,
                        direction.z * speed,
                        1.0D
                );
            }
        }
        return result;
    }

    public static void giveEmptyCan(Player player) {
        ItemStack emptyCan = new ItemStack(ModItems.EMPTY_WHITE_MONSTER.get());
        if (!player.getInventory().add(emptyCan)) {
            player.drop(emptyCan, false);
        }
    }
}
