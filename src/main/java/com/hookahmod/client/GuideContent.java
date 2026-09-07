package com.hookahmod.client;

import com.hookahmod.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/**
 * The pages of the in-game guidebook. Split from {@link GuideScreen} so the
 * screen only deals with laying the book out and this file only with what it
 * says; every string here is a translation key.
 */
public final class GuideContent {

    private GuideContent() {}

    public static final List<Chapter> CHAPTERS = List.of(
            new Chapter("guide.hookahmod.chapter.overview", 0, 0xFF9B5B28),
            new Chapter("guide.hookahmod.chapter.hookah", 1, 0xFFB56B2D),
            new Chapter("guide.hookahmod.chapter.tobacco", 4, 0xFF557D3B),
            new Chapter("guide.hookahmod.chapter.effects", 6, 0xFF76518F),
            new Chapter("guide.hookahmod.chapter.world", 7, 0xFF477D83)
    );

    public static final List<GuidePage> PAGES = List.of(
            textPage(
                    "guide.hookahmod.intro.title",
                    stack(ModItems.GUIDEBOOK.get()),
                    "guide.hookahmod.intro.line1",
                    "",
                    "guide.hookahmod.intro.line2",
                    "",
                    "guide.hookahmod.intro.line3"
            ),
            indexPage(
                    "guide.hookahmod.contents.title",
                    new String[]{
                            "guide.hookahmod.contents.caption1",
                            "guide.hookahmod.contents.caption2",
                            "guide.hookahmod.contents.caption3",
                            "guide.hookahmod.contents.caption4"
                    },
                    stack(ModItems.HOOKAH.get()),
                    stack(ModItems.HOOKAH_TOBACCO.get()),
                    stack(ModItems.TONOMETER.get()),
                    stack(ModItems.TOBACCO_SEED.get())
            ),
            flowPage(
                    "guide.hookahmod.quickstart.title",
                    new String[]{
                            "guide.hookahmod.quickstart.line1",
                            "guide.hookahmod.quickstart.line2",
                            "guide.hookahmod.quickstart.line3"
                    },
                    stack(ModItems.HOOKAH.get()),
                    stack(ModItems.SHORT_HOOKAH_HOSE.get()),
                    stack(ModItems.HOOKAH_TOBACCO.get()),
                    stack(ModItems.HOOKAH_CHARCOAL.get()),
                    stack(ModItems.HOOKAH_WATER_BOTTLE.get()),
                    stack(ModItems.HOOKAH_MOUTHPIECE.get())
            ),
            gridPage(
                    "guide.hookahmod.hoses.title",
                    new String[]{
                            "guide.hookahmod.hoses.caption1",
                            "guide.hookahmod.hoses.caption2",
                            "guide.hookahmod.hoses.caption3",
                            "guide.hookahmod.hoses.caption4"
                    },
                    new String[]{
                            "guide.hookahmod.hoses.line1",
                            "guide.hookahmod.hoses.line2",
                            "guide.hookahmod.hoses.line3"
                    },
                    stack(ModItems.SHORT_HOOKAH_HOSE.get()),
                    stack(ModItems.LONG_HOOKAH_HOSE.get()),
                    stack(ModItems.HOOKAH_MOUTHPIECE.get()),
                    stack(ModItems.TONOMETER.get())
            ),
            gridPage(
                    "guide.hookahmod.tiers.title",
                    new String[]{
                            "guide.hookahmod.tiers.caption1",
                            "guide.hookahmod.tiers.caption2",
                            "guide.hookahmod.tiers.caption3",
                            "guide.hookahmod.tiers.caption4",
                            "guide.hookahmod.tiers.caption5",
                            "guide.hookahmod.tiers.caption6"
                    },
                    new String[]{
                            "guide.hookahmod.tiers.line1",
                            "guide.hookahmod.tiers.line2"
                    },
                    stack(ModItems.HOOKAH.get()),
                    stack(ModItems.HOOKAH_LEATHER.get()),
                    stack(ModItems.HOOKAH_GOLD.get()),
                    stack(ModItems.HOOKAH_IRON.get()),
                    stack(ModItems.HOOKAH_DIAMOND.get()),
                    stack(ModItems.HOOKAH_NETHERITE.get())
            ),
            flowPage(
                    "guide.hookahmod.parts.title",
                    new String[]{
                            "guide.hookahmod.parts.line1",
                            "guide.hookahmod.parts.line2"
                    },
                    stack(ModItems.HOOKAH_FLASK.get()),
                    stack(ModItems.HOOKAH_SHAFT.get()),
                    stack(ModItems.HOOKAH_BOWL.get()),
                    stack(ModItems.HOOKAH.get())
            ),
            textPage(
                    "guide.hookahmod.worn.title",
                    stack(ModItems.HOOKAH_NETHERITE.get()),
                    "guide.hookahmod.worn.line1",
                    "",
                    "guide.hookahmod.worn.line2",
                    "",
                    "guide.hookahmod.worn.line3"
            ),
            gridPage(
                    "guide.hookahmod.supplies.title",
                    new String[]{
                            "guide.hookahmod.supplies.caption1",
                            "guide.hookahmod.supplies.caption2",
                            "guide.hookahmod.supplies.caption3",
                            "guide.hookahmod.supplies.caption4",
                            "guide.hookahmod.supplies.caption5",
                            "guide.hookahmod.supplies.caption6"
                    },
                    new String[]{
                            "guide.hookahmod.supplies.line1",
                            "guide.hookahmod.supplies.line2"
                    },
                    stack(ModItems.HOOKAH_CHARCOAL.get()),
                    stack(ModItems.HOOKAH_WATER_BOTTLE.get()),
                    stack(ModItems.HOOKAH_MOUTHPIECE.get()),
                    stack(ModItems.SHORT_HOOKAH_HOSE.get()),
                    stack(ModItems.LONG_HOOKAH_HOSE.get()),
                    stack(ModItems.TONOMETER.get())
            ),
            textPage(
                    "guide.hookahmod.tobacco_base.title",
                    stack(ModItems.HOOKAH_TOBACCO.get()),
                    "guide.hookahmod.tobacco_base.line1",
                    "",
                    "guide.hookahmod.tobacco_base.line2",
                    "",
                    "guide.hookahmod.tobacco_base.line3"
            ),
            gridPage(
                    "guide.hookahmod.blends1.title",
                    new String[]{
                            "guide.hookahmod.blends1.caption1",
                            "guide.hookahmod.blends1.caption2",
                            "guide.hookahmod.blends1.caption3",
                            "guide.hookahmod.blends1.caption4",
                            "guide.hookahmod.blends1.caption5",
                            "guide.hookahmod.blends1.caption6"
                    },
                    new String[]{
                            "guide.hookahmod.blends1.line1",
                            "guide.hookahmod.blends1.line2",
                            "guide.hookahmod.blends1.line3"
                    },
                    stack(ModItems.TOBACCO_APPLE.get()),
                    stack(ModItems.TOBACCO_HONEY.get()),
                    stack(ModItems.TOBACCO_CITRUS.get()),
                    stack(ModItems.TOBACCO_COFFEE.get()),
                    stack(ModItems.TOBACCO_MINT.get()),
                    stack(ModItems.TOBACCO_LAVENDER.get())
            ),
            gridPage(
                    "guide.hookahmod.blends2.title",
                    new String[]{
                            "guide.hookahmod.blends2.caption1",
                            "guide.hookahmod.blends2.caption2",
                            "guide.hookahmod.blends2.caption3"
                    },
                    new String[]{
                            "guide.hookahmod.blends2.line1",
                            "guide.hookahmod.blends2.line2",
                            "guide.hookahmod.blends2.line3"
                    },
                    stack(ModItems.TOBACCO_MINER.get()),
                    stack(ModItems.TOBACCO_TRAVELER.get()),
                    stack(ModItems.TOBACCO_FISHER.get())
            ),
            gridPage(
                    "guide.hookahmod.combat.title",
                    new String[]{
                            "guide.hookahmod.combat.caption1",
                            "guide.hookahmod.combat.caption2",
                            "guide.hookahmod.combat.caption3",
                            "guide.hookahmod.combat.caption4"
                    },
                    new String[]{
                            "guide.hookahmod.combat.line1",
                            "guide.hookahmod.combat.line2",
                            "guide.hookahmod.combat.line3"
                    },
                    stack(ModItems.TOBACCO_POISON.get()),
                    stack(ModItems.TOBACCO_FIRE.get()),
                    stack(ModItems.TOBACCO_ICE.get()),
                    stack(ModItems.TOBACCO_HEAL.get())
            ),
            gridPage(
                    "guide.hookahmod.special.title",
                    new String[]{
                            "guide.hookahmod.special.caption1",
                            "guide.hookahmod.special.caption2",
                            "guide.hookahmod.special.caption3"
                    },
                    new String[]{
                            "guide.hookahmod.special.line1",
                            "guide.hookahmod.special.line2",
                            "guide.hookahmod.special.line3"
                    },
                    stack(ModItems.TOBACCO_ABYSS.get()),
                    stack(ModItems.TOBACCO_HASHISH.get()),
                    stack(ModItems.TOBACCO_PALPALYCH.get())
            ),
            stagesPage(
                    "guide.hookahmod.stages.title",
                    "guide.hookahmod.stages.line1",
                    "guide.hookahmod.stages.line2",
                    "guide.hookahmod.stages.line3",
                    "guide.hookahmod.stages.line4",
                    "guide.hookahmod.stages.line5"
            ),
            gridPage(
                    "guide.hookahmod.crops.title",
                    new String[]{
                            "guide.hookahmod.crops.caption1",
                            "guide.hookahmod.crops.caption2",
                            "guide.hookahmod.crops.caption3",
                            "guide.hookahmod.crops.caption4",
                            "guide.hookahmod.crops.caption5"
                    },
                    new String[]{
                            "guide.hookahmod.crops.line1",
                            "guide.hookahmod.crops.line2",
                            "guide.hookahmod.crops.line3"
                    },
                    stack(ModItems.TOBACCO_SEED.get()),
                    stack(ModItems.MINT_SEED.get()),
                    stack(ModItems.LAVENDER_SEED.get()),
                    stack(ModItems.MINT.get()),
                    stack(ModItems.LAVENDER.get())
            ),
            gridPage(
                    "guide.hookahmod.croptopia.title",
                    new String[]{
                            "guide.hookahmod.croptopia.caption1",
                            "guide.hookahmod.croptopia.caption2",
                            "guide.hookahmod.croptopia.caption3",
                            "guide.hookahmod.croptopia.caption4"
                    },
                    new String[]{
                            "guide.hookahmod.croptopia.line1",
                            "guide.hookahmod.croptopia.line2"
                    },
                    stack(ModItems.TOBACCO_CITRUS.get()),
                    stack(ModItems.TOBACCO_COFFEE.get()),
                    stack(Items.CARROT),
                    stack(Items.COCOA_BEANS)
            ),
            flowPage(
                    "guide.hookahmod.recipes.title",
                    new String[]{
                            "guide.hookahmod.recipes.line1",
                            "guide.hookahmod.recipes.line2"
                    },
                    stack(Items.BOOK),
                    stack(ModItems.HOOKAH_TOBACCO.get()),
                    stack(ModItems.GUIDEBOOK.get())
            ),
            textPage(
                    "guide.hookahmod.outro.title",
                    stack(ModItems.GUIDEBOOK.get()),
                    "guide.hookahmod.outro.line1",
                    "",
                    "guide.hookahmod.outro.line2",
                    "",
                    "guide.hookahmod.outro.line3"
            )
    );

    private static GuidePage textPage(String title, ItemStack icon, String... lines) {
        return new GuidePage(
                Component.translatable(title),
                components(lines),
                List.of(icon),
                List.of(),
                PageStyle.TEXT
        );
    }

    private static GuidePage flowPage(String title, String[] lines, ItemStack... icons) {
        return new GuidePage(
                Component.translatable(title),
                components(lines),
                List.of(icons),
                List.of(),
                PageStyle.FLOW
        );
    }

    private static GuidePage gridPage(
            String title,
            String[] captions,
            String[] lines,
            ItemStack... icons
    ) {
        return new GuidePage(
                Component.translatable(title),
                components(lines),
                List.of(icons),
                components(captions),
                PageStyle.GRID
        );
    }

    private static GuidePage indexPage(String title, String[] captions, ItemStack... icons) {
        return new GuidePage(
                Component.translatable(title),
                List.of(),
                List.of(icons),
                components(captions),
                PageStyle.INDEX
        );
    }

    private static GuidePage stagesPage(String title, String... stages) {
        return new GuidePage(
                Component.translatable(title),
                components(stages),
                List.of(stack(ModItems.TONOMETER.get())),
                List.of(),
                PageStyle.STAGES
        );
    }

    private static List<Component> components(String... keys) {
        List<Component> components = new ArrayList<>(keys.length);
        for (String key : keys) {
            components.add(key.isEmpty() ? Component.empty() : Component.translatable(key));
        }
        return List.copyOf(components);
    }

    private static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }

    public record Chapter(String nameKey, int firstSpread, int color) {
    }

    public record GuidePage(
            Component title,
            List<Component> lines,
            List<ItemStack> icons,
            List<Component> captions,
            PageStyle style
    ) {
    }

    public enum PageStyle {
        TEXT,
        FLOW,
        GRID,
        INDEX,
        STAGES
    }
}
