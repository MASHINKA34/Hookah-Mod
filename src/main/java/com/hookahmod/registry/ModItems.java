package com.hookahmod.registry;

import com.hookahmod.HookahMod;
import com.hookahmod.item.HookahTier;
import com.hookahmod.item.HookahBlockItem;
import com.hookahmod.item.HookahCharcoalItem;
import com.hookahmod.item.HookahMouthpieceItem;
import com.hookahmod.item.HookahTobaccoItem;
import com.hookahmod.item.HookahWaterBottleItem;
import com.hookahmod.item.LongHookahHoseItem;
import com.hookahmod.item.ShortHookahHoseItem;
import com.hookahmod.item.TonometerItem;
import com.hookahmod.item.CombatTobaccoItem;
import com.hookahmod.item.AbyssTobaccoItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HookahMod.MOD_ID);

    public static final DeferredItem<HookahBlockItem> HOOKAH = ITEMS.register(
            "hookah",
            () -> new HookahBlockItem(ModBlocks.HOOKAH.get(), hookahProperties(HookahTier.LEATHER), HookahTier.LEATHER)
    );

    public static final DeferredItem<HookahBlockItem> HOOKAH_GOLD = ITEMS.register(
            "hookah_gold",
            () -> new HookahBlockItem(ModBlocks.HOOKAH.get(), hookahProperties(HookahTier.GOLD), HookahTier.GOLD)
    );

    public static final DeferredItem<HookahBlockItem> HOOKAH_IRON = ITEMS.register(
            "hookah_iron",
            () -> new HookahBlockItem(ModBlocks.HOOKAH.get(), hookahProperties(HookahTier.IRON), HookahTier.IRON)
    );

    public static final DeferredItem<HookahBlockItem> HOOKAH_DIAMOND = ITEMS.register(
            "hookah_diamond",
            () -> new HookahBlockItem(ModBlocks.HOOKAH.get(), hookahProperties(HookahTier.DIAMOND), HookahTier.DIAMOND)
    );

    public static final DeferredItem<HookahBlockItem> HOOKAH_NETHERITE = ITEMS.register(
            "hookah_netherite",
            () -> new HookahBlockItem(ModBlocks.HOOKAH.get(), hookahProperties(HookahTier.NETHERITE), HookahTier.NETHERITE)
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

    public static final DeferredItem<CombatTobaccoItem> TOBACCO_POISON = ITEMS.register(
            "tobacco_poison",
            () -> new CombatTobaccoItem(new Item.Properties().stacksTo(64), CombatTobaccoItem.CombatType.POISON)
    );

    public static final DeferredItem<CombatTobaccoItem> TOBACCO_FIRE = ITEMS.register(
            "tobacco_fire",
            () -> new CombatTobaccoItem(new Item.Properties().stacksTo(64), CombatTobaccoItem.CombatType.FIRE)
    );

    public static final DeferredItem<CombatTobaccoItem> TOBACCO_ICE = ITEMS.register(
            "tobacco_ice",
            () -> new CombatTobaccoItem(new Item.Properties().stacksTo(64), CombatTobaccoItem.CombatType.ICE)
    );

    public static final DeferredItem<CombatTobaccoItem> TOBACCO_HEAL = ITEMS.register(
            "tobacco_heal",
            () -> new CombatTobaccoItem(new Item.Properties().stacksTo(64), CombatTobaccoItem.CombatType.HEAL)
    );

    public static final DeferredItem<AbyssTobaccoItem> TOBACCO_ABYSS = ITEMS.register(
            "tobacco_abyss",
            () -> new AbyssTobaccoItem(new Item.Properties().stacksTo(64))
    );

    public static final DeferredItem<HookahCharcoalItem> HOOKAH_CHARCOAL = ITEMS.register(
            "hookah_charcoal",
            () -> new HookahCharcoalItem(new Item.Properties().stacksTo(64))
    );

    public static final DeferredItem<HookahWaterBottleItem> HOOKAH_WATER_BOTTLE = ITEMS.register(
            "hookah_water_bottle",
            () -> new HookahWaterBottleItem(new Item.Properties().stacksTo(16))
    );

    public static final DeferredItem<TonometerItem> TONOMETER = ITEMS.register(
            "tonometer",
            () -> new TonometerItem(new Item.Properties().stacksTo(1))
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

    private static Item.Properties hookahProperties(HookahTier tier) {
        Item.Properties properties = new Item.Properties()
                .stacksTo(1)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
                .attributes(ItemAttributeModifiers.builder()
                        .add(
                                Attributes.ARMOR,
                                new AttributeModifier(
                                        HookahMod.id("hookah_" + tier.getSerializedName() + "_armor"),
                                        tier.armor(),
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.CHEST
                        )
                        .build());
        if (tier.fireResistant()) properties.component(DataComponents.FIRE_RESISTANT, Unit.INSTANCE);
        return properties;
    }

    private ModItems() {}
}
