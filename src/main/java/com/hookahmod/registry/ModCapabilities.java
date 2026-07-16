package com.hookahmod.registry;

import com.hookahmod.integration.WhiteMonsterFluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new WhiteMonsterFluidHandler(stack),
                ModItems.WHITE_MONSTER.get(),
                ModItems.EMPTY_WHITE_MONSTER.get()
        );
    }

    private ModCapabilities() {}
}
