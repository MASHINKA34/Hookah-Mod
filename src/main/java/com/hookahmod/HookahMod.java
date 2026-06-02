package com.hookahmod;

import com.hookahmod.effect.ModMobEffects;
import com.hookahmod.event.ServerEvents;
import com.hookahmod.network.NetworkHandler;
import com.hookahmod.registry.ModBlockEntities;
import com.hookahmod.registry.ModBlocks;
import com.hookahmod.registry.ModCreativeTabs;
import com.hookahmod.registry.ModItems;
import com.hookahmod.registry.ModLootModifiers;
import com.hookahmod.registry.ModMenuTypes;
import com.hookahmod.registry.ModParticles;
import com.hookahmod.registry.ModSounds;
import com.hookahmod.smoking.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(HookahMod.MOD_ID)
public final class HookahMod {

    public static final String MOD_ID = "hookahmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public HookahMod(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenuTypes.MENUS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModMobEffects.MOB_EFFECTS.register(modBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modBus);

        modBus.addListener(NetworkHandler::register);

        NeoForge.EVENT_BUS.register(ServerEvents.class);

        LOGGER.info("Hookah Mod loaded");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
