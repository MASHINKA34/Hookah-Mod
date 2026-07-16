package com.hookahmod.item;

import com.hookahmod.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

public class WhiteMonsterItem extends Item {

    public WhiteMonsterItem(Properties properties) {
        super(properties);
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
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer player) {
            Vec3 look = player.getLookAngle();
            Vec3 mouth = player.getEyePosition().add(look.scale(0.35D)).add(0.0D, -0.18D, 0.0D);
            level.playSound(null, player.blockPosition(), ModSounds.WHITE_MONSTER_BURP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            for (int i = 0; i < 20; i++) {
                double speed = 0.04D + player.getRandom().nextDouble() * 0.06D;
                server.sendParticles(
                        ParticleTypes.BUBBLE_POP,
                        mouth.x,
                        mouth.y,
                        mouth.z,
                        0,
                        look.x * speed + player.getRandom().nextGaussian() * 0.025D,
                        look.y * speed + 0.025D + player.getRandom().nextDouble() * 0.035D,
                        look.z * speed + player.getRandom().nextGaussian() * 0.025D,
                        1.0D
                );
            }
        }
        return stack;
    }
}
