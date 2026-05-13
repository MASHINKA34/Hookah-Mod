package com.hookahmod.registry;

import com.hookahmod.HookahMod;
import com.hookahmod.item.HookahMouthpieceItem;
import com.hookahmod.item.LongHookahHoseItem;
import com.hookahmod.item.ShortHookahHoseItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HookahMod.MOD_ID);

    public static final DeferredItem<BlockItem> HOOKAH = ITEMS.registerSimpleBlockItem(ModBlocks.HOOKAH);

    public static final DeferredItem<ShortHookahHoseItem> SHORT_HOOKAH_HOSE = ITEMS.register(
            "short_hookah_hose",
            () -> new ShortHookahHoseItem(new Item.Properties().stacksTo(16))
    );

    public static final DeferredItem<LongHookahHoseItem> LONG_HOOKAH_HOSE = ITEMS.register(
            "long_hookah_hose",
            () -> new LongHookahHoseItem(new Item.Properties().stacksTo(16))
    );

    public static final DeferredItem<HookahMouthpieceItem> HOOKAH_MOUTHPIECE = ITEMS.register(
            "hookah_mouthpiece",
            () -> new HookahMouthpieceItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {}
}
