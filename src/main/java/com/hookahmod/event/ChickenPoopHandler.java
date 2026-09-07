package com.hookahmod.event;

import com.hookahmod.config.HookahConfig;
import com.hookahmod.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Chicken;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.function.Consumer;

public final class ChickenPoopHandler {

    private static final Consumer<EntityTickEvent.Pre> LISTENER = ChickenPoopHandler::onEntityTick;

    private static boolean registered;

    private ChickenPoopHandler() {}

    public static synchronized void syncRegistration() {
        boolean wanted = HookahConfig.chickenPoopChance > 0.0f;
        if (wanted == registered) return;
        if (wanted) {
            NeoForge.EVENT_BUS.addListener(LISTENER);
        } else {
            NeoForge.EVENT_BUS.unregister(LISTENER);
        }
        registered = wanted;
    }

    private static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Chicken chicken)) return;
        if (chicken.eggTime != 1 || !(chicken.level() instanceof ServerLevel)) return;
        if (!chicken.isAlive() || chicken.isBaby() || chicken.isChickenJockey()) return;
        if (chicken.getRandom().nextFloat() >= HookahConfig.chickenPoopChance) return;

        chicken.spawnAtLocation(ModItems.CHICKEN_POOP.get());
    }
}
