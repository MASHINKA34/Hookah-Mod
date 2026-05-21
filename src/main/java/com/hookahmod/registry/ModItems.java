package com.hookahmod.registry;

import com.hookahmod.HookahMod;
import com.hookahmod.item.HookahBlockItem;
import com.hookahmod.item.HookahCharcoalItem;
import com.hookahmod.item.HookahMouthpieceItem;
import com.hookahmod.item.HookahTobaccoItem;
import com.hookahmod.item.HookahWaterBottleItem;
import com.hookahmod.item.LongHookahHoseItem;
import com.hookahmod.item.ShortHookahHoseItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HookahMod.MOD_ID);

    public static final DeferredItem<HookahBlockItem> HOOKAH = ITEMS.register(
            "hookah",
            () -> new HookahBlockItem(ModBlocks.HOOKAH.get(), new Item.Properties().stacksTo(1))
    );

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

    public static final DeferredItem<HookahTobaccoItem> HOOKAH_TOBACCO = ITEMS.register(
            "hookah_tobacco",
            () -> new HookahTobaccoItem(new Item.Properties().stacksTo(64))
    );

    public static final DeferredItem<HookahCharcoalItem> HOOKAH_CHARCOAL = ITEMS.register(
            "hookah_charcoal",
            () -> new HookahCharcoalItem(new Item.Properties().stacksTo(64))
    );

    public static final DeferredItem<HookahWaterBottleItem> HOOKAH_WATER_BOTTLE = ITEMS.register(
            "hookah_water_bottle",
            () -> new HookahWaterBottleItem(new Item.Properties().stacksTo(16))
    );

    public static final DeferredItem<Item> HOOKAH_FLASK = ITEMS.register(
            "hookah_flask",
            () -> new Item(new Item.Properties().stacksTo(16))
    );

    public static final DeferredItem<Item> HOOKAH_SHAFT = ITEMS.register(
            "hookah_shaft",
            () -> new Item(new Item.Properties().stacksTo(16))
    );

    public static final DeferredItem<Item> HOOKAH_BOWL = ITEMS.register(
            "hookah_bowl",
            () -> new Item(new Item.Properties().stacksTo(16))
    );

    private ModItems() {}
}
