package com.hookahmod.registry;

import com.hookahmod.HookahMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HookahMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HOOKAH_TAB = TABS.register(
            "hookah_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hookahmod"))
                    .icon(() -> ModItems.HOOKAH.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.HOOKAH.get());
                        output.accept(ModItems.HOOKAH_LEATHER.get());
                        output.accept(ModItems.HOOKAH_GOLD.get());
                        output.accept(ModItems.HOOKAH_IRON.get());
                        output.accept(ModItems.HOOKAH_DIAMOND.get());
                        output.accept(ModItems.HOOKAH_NETHERITE.get());
                        output.accept(ModItems.HOOKAH_FLASK.get());
                        output.accept(ModItems.HOOKAH_SHAFT.get());
                        output.accept(ModItems.HOOKAH_BOWL.get());
                        output.accept(ModItems.HOOKAH_MOUTHPIECE.get());
                        output.accept(ModItems.SHORT_HOOKAH_HOSE.get());
                        output.accept(ModItems.LONG_HOOKAH_HOSE.get());
                        output.accept(ModItems.HOOKAH_TOBACCO.get());
                        output.accept(ModItems.TOBACCO_POISON.get());
                        output.accept(ModItems.TOBACCO_FIRE.get());
                        output.accept(ModItems.TOBACCO_ICE.get());
                        output.accept(ModItems.TOBACCO_HEAL.get());
                        output.accept(ModItems.TOBACCO_ABYSS.get());
                        output.accept(ModItems.TOBACCO_HASHISH.get());
                        output.accept(ModItems.TOBACCO_APPLE.get());
                        output.accept(ModItems.TOBACCO_HONEY.get());
                        output.accept(ModItems.TOBACCO_CITRUS.get());
                        output.accept(ModItems.TOBACCO_COFFEE.get());
                        output.accept(ModItems.TOBACCO_MINER.get());
                        output.accept(ModItems.TOBACCO_TRAVELER.get());
                        output.accept(ModItems.TOBACCO_FISHER.get());
                        output.accept(ModItems.TOBACCO_MINT.get());
                        output.accept(ModItems.TOBACCO_LAVENDER.get());
                        output.accept(ModItems.HOOKAH_CHARCOAL.get());
                        output.accept(ModItems.HOOKAH_WATER_BOTTLE.get());
                        output.accept(ModItems.TONOMETER.get());
                        output.accept(ModItems.TOBACCO_SEED.get());
                        output.accept(ModItems.MINT.get());
                        output.accept(ModItems.MINT_SEED.get());
                        output.accept(ModItems.LAVENDER.get());
                        output.accept(ModItems.LAVENDER_SEED.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {}
}
