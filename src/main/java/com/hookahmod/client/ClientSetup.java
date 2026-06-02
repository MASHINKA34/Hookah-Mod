package com.hookahmod.client;

import com.hookahmod.HookahMod;
import com.hookahmod.registry.ModBlockEntities;
import com.hookahmod.registry.ModItems;
import com.hookahmod.registry.ModMenuTypes;
import com.hookahmod.registry.ModParticles;
import com.hookahmod.network.OpenWornHookahPayload;
import com.hookahmod.item.WornHookah;
import com.hookahmod.client.particle.ColoredHookahSmokeParticle;
import com.hookahmod.client.trip.HashishTripManager;
import com.hookahmod.client.trip.TripManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = HookahMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {

    private static final KeyMapping OPEN_WORN_HOOKAH = new KeyMapping(
            "key.hookahmod.open_worn_hookah",
            GLFW.GLFW_KEY_Z,
            "key.categories.hookahmod"
    );

    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(ClientSetup::onKeyInput);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
        NeoForge.EVENT_BUS.addListener(TripManager::render);
        NeoForge.EVENT_BUS.addListener(TripManager::renderScreamer);
        NeoForge.EVENT_BUS.addListener(TripManager::computeFov);
        NeoForge.EVENT_BUS.addListener(TripManager::cameraAngles);
        NeoForge.EVENT_BUS.addListener(TripManager::fogColor);
        NeoForge.EVENT_BUS.addListener(HashishTripManager::computeFov);
        NeoForge.EVENT_BUS.addListener(HashishTripManager::cameraAngles);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_WORN_HOOKAH);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.HOOKAH.get(), HookahBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new HookahBackLayer(renderer));
        }
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.HOOKAH.get(), HookahScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.COLORED_HOOKAH_SMOKE.get(), ColoredHookahSmokeParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new HookahMouthpieceRenderer();
                return renderer;
            }
        }, ModItems.HOOKAH_MOUTHPIECE.get());
    }

    private static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (OPEN_WORN_HOOKAH.consumeClick()) {
            if (WornHookah.isHookahStack(mc.player.getItemBySlot(EquipmentSlot.CHEST))) {
                PacketDistributor.sendToServer(new OpenWornHookahPayload());
            }
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        HookahSmokingSound.tickLocal();
        TripManager.tick(event);
        HashishTripManager.tick(event);
    }
}
