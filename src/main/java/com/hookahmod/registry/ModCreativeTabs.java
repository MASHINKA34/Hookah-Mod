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
                        output.accept(ModItems.SHORT_HOOKAH_HOSE.get());
                        output.accept(ModItems.LONG_HOOKAH_HOSE.get());
                        output.accept(ModItems.HOOKAH_TOBACCO.get());
                        output.accept(ModItems.HOOKAH_CHARCOAL.get());
                        output.accept(ModItems.HOOKAH_WATER_BOTTLE.get());
                        output.accept(ModItems.HOOKAH_MOUTHPIECE.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {}
}
