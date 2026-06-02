package com.hookahmod.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class AddItemLootModifier extends LootModifier {

    public static final MapCodec<AddItemLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).and(inst.group(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(m -> m.item),
                    Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance)
            )).apply(inst, AddItemLootModifier::new));

    private final Item item;
    private final float chance;

    public AddItemLootModifier(LootItemCondition[] conditions, Item item, float chance) {
        super(conditions);
        this.item = item;
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < chance) {
            generatedLoot.add(new ItemStack(item));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
