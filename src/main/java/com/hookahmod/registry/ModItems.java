package com.hookahmod.registry;

import com.hookahmod.HookahMod;
import com.hookahmod.item.GuidebookItem;
import com.hookahmod.item.HookahTier;
import com.hookahmod.item.HookahBlockItem;
import com.hookahmod.item.HookahCharcoalItem;
import com.hookahmod.item.HookahMouthpieceItem;
import com.hookahmod.item.HookahTobaccoItem;
import com.hookahmod.item.HookahWaterBottleItem;
import com.hookahmod.item.HashishTobaccoItem;
import com.hookahmod.item.PalPalychTobaccoItem;
import com.hookahmod.item.LongHookahHoseItem;
import com.hookahmod.item.ShortHookahHoseItem;
import com.hookahmod.item.TonometerItem;
import com.hookahmod.item.CombatTobaccoItem;
import com.hookahmod.item.AbyssTobaccoItem;
import com.hookahmod.item.BuffTobaccoItem;
import com.hookahmod.effect.ModMobEffects;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.joml.Vector3f;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HookahMod.MOD_ID);

    public static final DeferredItem<HookahBlockItem> HOOKAH = ITEMS.register(
            "hookah",
            () -> new HookahBlockItem(ModBlocks.HOOKAH.get(), normalHookahProperties(), HookahTier.NORMAL)
    );

    public static final DeferredItem<HookahBlockItem> HOOKAH_LEATHER = ITEMS.register(
            "hookah_leather",
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

    public static final DeferredItem<HashishTobaccoItem> TOBACCO_HASHISH = ITEMS.register(
            "tobacco_hashish",
            () -> new HashishTobaccoItem(new Item.Properties().stacksTo(64))
    );

    public static final DeferredItem<PalPalychTobaccoItem> TOBACCO_PALPALYCH = ITEMS.register(
            "tobacco_palpalych",
            () -> new PalPalychTobaccoItem(new Item.Properties().stacksTo(64))
    );

    public static final DeferredItem<BuffTobaccoItem> TOBACCO_APPLE = ITEMS.register(
            "tobacco_apple",
            () -> new BuffTobaccoItem(new Item.Properties().stacksTo(64), 15,
                    new Vector3f(0.85f, 0.20f, 0.22f),
                    new BuffTobaccoItem.Buff(MobEffects.REGENERATION, 0, 20))
    );

    public static final DeferredItem<BuffTobaccoItem> TOBACCO_HONEY = ITEMS.register(
            "tobacco_honey",
            () -> new BuffTobaccoItem(new Item.Properties().stacksTo(64), 18,
                    new Vector3f(0.95f, 0.72f, 0.20f),
                    new BuffTobaccoItem.Buff(MobEffects.SATURATION, 0, 10))
    );

    public static final DeferredItem<BuffTobaccoItem> TOBACCO_CITRUS = ITEMS.register(
            "tobacco_citrus",
            () -> new BuffTobaccoItem(new Item.Properties().stacksTo(64), 18,
                    new Vector3f(1.0f, 0.55f, 0.10f),
                    new BuffTobaccoItem.Buff(MobEffects.DIG_SPEED, 0, 30))
    );

    public static final DeferredItem<BuffTobaccoItem> TOBACCO_COFFEE = ITEMS.register(
            "tobacco_coffee",
            () -> new BuffTobaccoItem(new Item.Properties().stacksTo(64), 22,
                    new Vector3f(0.40f, 0.26f, 0.15f),
                    new BuffTobaccoItem.Buff(MobEffects.MOVEMENT_SPEED, 0, 30),
                    new BuffTobaccoItem.Buff(MobEffects.DIG_SPEED, 0, 30))
    );

    public static final DeferredItem<BuffTobaccoItem> TOBACCO_MINER = ITEMS.register(
            "tobacco_miner",
            () -> new BuffTobaccoItem(new Item.Properties().stacksTo(64), 27,
                    new Vector3f(0.30f, 0.30f, 0.32f),
                    new BuffTobaccoItem.Buff(MobEffects.NIGHT_VISION, 0, 45),
                    new BuffTobaccoItem.Buff(MobEffects.DIG_SPEED, 0, 45))
    );

    public static final DeferredItem<BuffTobaccoItem> TOBACCO_TRAVELER = ITEMS.register(
            "tobacco_traveler",
            () -> new BuffTobaccoItem(new Item.Properties().stacksTo(64), 27,
                    new Vector3f(0.80f, 0.70f, 0.45f),
                    new BuffTobaccoItem.Buff(MobEffects.MOVEMENT_SPEED, 1, 25))
    );

    public static final DeferredItem<BuffTobaccoItem> TOBACCO_FISHER = ITEMS.register(
            "tobacco_fisher",
            () -> new BuffTobaccoItem(new Item.Properties().stacksTo(64), 22,
                    new Vector3f(0.30f, 0.75f, 0.80f),
                    new BuffTobaccoItem.Buff(ModMobEffects.SEA_LUCK, 1, 30))
    );

    public static final DeferredItem<BuffTobaccoItem> TOBACCO_MINT = ITEMS.register(
            "tobacco_mint",
            () -> new BuffTobaccoItem(new Item.Properties().stacksTo(64), 14,
                    new Vector3f(0.55f, 0.95f, 0.65f),
                    new BuffTobaccoItem.Buff(MobEffects.MOVEMENT_SPEED, 0, 30))
    );

    public static final DeferredItem<BuffTobaccoItem> TOBACCO_LAVENDER = ITEMS.register(
            "tobacco_lavender",
            () -> new BuffTobaccoItem(new Item.Properties().stacksTo(64), 22,
                    new Vector3f(0.70f, 0.55f, 0.90f),
                    new BuffTobaccoItem.Buff(MobEffects.NIGHT_VISION, 0, 45))
    );

    public static final DeferredItem<HookahCharcoalItem> HOOKAH_CHARCOAL = ITEMS.register(
            "hookah_charcoal",
            () -> new HookahCharcoalItem(new Item.Properties().stacksTo(64))
    );

    public static final DeferredItem<HookahWaterBottleItem> HOOKAH_WATER_BOTTLE = ITEMS.register(
            "hookah_water_bottle",
            () -> new HookahWaterBottleItem(new Item.Properties().stacksTo(16))
    );

    public static final DeferredItem<Item> CHICKEN_POOP = ITEMS.register(
            "chicken_poop",
            () -> new Item(new Item.Properties().stacksTo(64))
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

    public static final DeferredItem<ItemNameBlockItem> TOBACCO_SEED = ITEMS.register(
            "tobacco_seed",
            () -> new ItemNameBlockItem(ModBlocks.TOBACCO_CROP.get(), new Item.Properties())
    );

    public static final DeferredItem<ItemNameBlockItem> MINT_SEED = ITEMS.register(
            "mint_seed",
            () -> new ItemNameBlockItem(ModBlocks.MINT_CROP.get(), new Item.Properties())
    );

    public static final DeferredItem<ItemNameBlockItem> LAVENDER_SEED = ITEMS.register(
            "lavender_seed",
            () -> new ItemNameBlockItem(ModBlocks.LAVENDER_CROP.get(), new Item.Properties())
    );

    public static final DeferredItem<Item> MINT = ITEMS.register(
            "mint",
            () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final DeferredItem<Item> LAVENDER = ITEMS.register(
            "lavender",
            () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final DeferredItem<GuidebookItem> GUIDEBOOK = ITEMS.register(
            "guidebook",
            () -> new GuidebookItem(new Item.Properties().stacksTo(1))
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

    private static Item.Properties normalHookahProperties() {
        return new Item.Properties().stacksTo(1);
    }

    private ModItems() {}
}
