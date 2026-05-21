package com.hookahmod.registry;

import com.hookahmod.HookahMod;
import com.hookahmod.menu.HookahMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, HookahMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<HookahMenu>> HOOKAH =
            MENUS.register("hookah", () -> new MenuType<>(
                    (IContainerFactory<HookahMenu>) (windowId, inv, data) -> {
                        boolean wearable = data != null && data.readBoolean();
                        if (wearable) {
                            return new HookahMenu(windowId, inv, inv.player.getUUID());
                        }
                        BlockPos pos = data != null ? data.readBlockPos() : BlockPos.ZERO;
                        return new HookahMenu(windowId, inv, pos);
                    },
                    FeatureFlags.VANILLA_SET));

    private ModMenuTypes() {}
}
