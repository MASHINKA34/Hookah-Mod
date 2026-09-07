package com.hookahmod.client;

import com.hookahmod.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

public class GuideScreen extends Screen {

    private static final int BOOK_WIDTH = 360;
    private static final int BOOK_HEIGHT = 214;
    private static final int PAGE_WIDTH = 156;
    private static final int PAGE_HEIGHT = 178;
    private static final int LEFT_PAGE_OFFSET = 16;
    private static final int RIGHT_PAGE_OFFSET = 188;
    private static final int PAGE_TOP_OFFSET = 17;

    private static final int COVER_DARK = 0xFF2A1005;
    private static final int COVER = 0xFF5A2B12;
    private static final int COVER_LIGHT = 0xFF8A4A22;
    private static final int PAGE_SHADOW = 0xFFC7B77D;
    private static final int PAGE = 0xFFF3E7B5;
    private static final int PAGE_LIGHT = 0xFFFFF3C8;
    private static final int INK = 0xFF32190B;
    private static final int INK_MUTED = 0xFF76542F;
    private static final int GOLD = 0xFFD69A3A;
    private static final int SLOT = 0xFFE1D39D;

    private static final List<Chapter> CHAPTERS = List.of(
            new Chapter("guide.hookahmod.chapter.overview", 0, 0xFF9B5B28),
            new Chapter("guide.hookahmod.chapter.hookah", 1, 0xFFB56B2D),
            new Chapter("guide.hookahmod.chapter.tobacco", 4, 0xFF557D3B),
            new Chapter("guide.hookahmod.chapter.effects", 6, 0xFF76518F),
            new Chapter("guide.hookahmod.chapter.world", 7, 0xFF477D83)
    );

    private static final List<GuidePage> PAGES = List.of(
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

    private final List<BookButton> chapterButtons = new ArrayList<>();
    private int spreadIndex;
    private BookButton previousButton;
    private BookButton nextButton;
    private BookButton contentsButton;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public GuideScreen() {
        super(Component.translatable("guide.hookahmod.screen_title"));
    }

    @Override
    protected void init() {
        this.chapterButtons.clear();

        int bookX = (this.width - BOOK_WIDTH) / 2;
        int bookY = (this.height - BOOK_HEIGHT) / 2;
        int tabWidth = 64;
        int tabGap = 3;
        int tabsWidth = CHAPTERS.size() * tabWidth + (CHAPTERS.size() - 1) * tabGap;
        int tabX = bookX + (BOOK_WIDTH - tabsWidth) / 2;

        for (int index = 0; index < CHAPTERS.size(); index++) {
            Chapter chapter = CHAPTERS.get(index);
            BookButton button = new BookButton(
                    tabX + index * (tabWidth + tabGap),
                    bookY - 13,
                    tabWidth,
                    18,
                    Component.translatable(chapter.nameKey()),
                    pressed -> jumpToChapter(chapter),
                    chapter.color()
            );
            this.chapterButtons.add(this.addRenderableWidget(button));
        }

        int navigationY = bookY + BOOK_HEIGHT - 24;
        this.previousButton = this.addRenderableWidget(new BookButton(
                bookX + 18, navigationY, 28, 18,
                Component.literal("←"), button -> changeSpread(-1), COVER
        ));
        this.contentsButton = this.addRenderableWidget(new BookButton(
                bookX + BOOK_WIDTH / 2 - 38, navigationY, 76, 18,
                Component.translatable("guide.hookahmod.contents_button"), button -> setSpread(0), COVER
        ));
        this.nextButton = this.addRenderableWidget(new BookButton(
                bookX + BOOK_WIDTH - 46, navigationY, 28, 18,
                Component.literal("→"), button -> changeSpread(1), COVER
        ));

        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.hoveredStack = ItemStack.EMPTY;
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!this.hoveredStack.isEmpty()) {
            graphics.renderTooltip(this.font, this.hoveredStack, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        int bookX = (this.width - BOOK_WIDTH) / 2;
        int bookY = (this.height - BOOK_HEIGHT) / 2;
        drawBook(graphics, bookX, bookY);

        int leftPageIndex = this.spreadIndex * 2;
        drawPage(
                graphics,
                PAGES.get(leftPageIndex),
                bookX + LEFT_PAGE_OFFSET,
                bookY + PAGE_TOP_OFFSET,
                mouseX,
                mouseY
        );

        int rightPageIndex = leftPageIndex + 1;
        if (rightPageIndex < PAGES.size()) {
            drawPage(
                    graphics,
                    PAGES.get(rightPageIndex),
                    bookX + RIGHT_PAGE_OFFSET,
                    bookY + PAGE_TOP_OFFSET,
                    mouseX,
                    mouseY
            );
        }

        drawPageNumbers(graphics, bookX, bookY, leftPageIndex, rightPageIndex);
    }

    private void drawBook(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 5, y + 8, x + BOOK_WIDTH + 5, y + BOOK_HEIGHT + 4, 0x66000000);

        graphics.fill(x + 4, y + 3, x + BOOK_WIDTH - 4, y + BOOK_HEIGHT - 3, COVER_DARK);
        graphics.fill(x, y + 9, x + BOOK_WIDTH, y + BOOK_HEIGHT - 9, COVER_DARK);
        graphics.fill(x + 4, y + 7, x + BOOK_WIDTH - 4, y + BOOK_HEIGHT - 7, COVER);
        graphics.fill(x + 8, y + 10, x + BOOK_WIDTH - 8, y + 14, COVER_LIGHT);

        drawPaper(graphics, x + LEFT_PAGE_OFFSET, y + PAGE_TOP_OFFSET, false);
        drawPaper(graphics, x + RIGHT_PAGE_OFFSET, y + PAGE_TOP_OFFSET, true);

        int spineX = x + BOOK_WIDTH / 2;
        graphics.fill(spineX - 8, y + 13, spineX + 8, y + BOOK_HEIGHT - 17, PAGE_SHADOW);
        graphics.fill(spineX - 5, y + 15, spineX, y + BOOK_HEIGHT - 18, 0xFFAD9A65);
        graphics.fill(spineX, y + 15, spineX + 5, y + BOOK_HEIGHT - 18, 0xFFE7D9A4);
        graphics.fill(spineX - 1, y + 16, spineX + 1, y + BOOK_HEIGHT - 18, 0x886B4B25);
    }

    private void drawPaper(GuiGraphics graphics, int x, int y, boolean right) {
        graphics.fill(x - 3, y + 3, x + PAGE_WIDTH + 3, y + PAGE_HEIGHT + 3, PAGE_SHADOW);
        graphics.fill(x, y, x + PAGE_WIDTH, y + PAGE_HEIGHT, PAGE);
        graphics.fill(x + 3, y + 2, x + PAGE_WIDTH - 3, y + 4, PAGE_LIGHT);

        int edgeX = right ? x + PAGE_WIDTH - 3 : x;
        graphics.fill(edgeX, y + 5, edgeX + 3, y + PAGE_HEIGHT - 4, 0x55A88A50);
        for (int line = 0; line < 6; line++) {
            int lineY = y + 28 + line * 25;
            graphics.fill(x + 5, lineY, x + PAGE_WIDTH - 5, lineY + 1, 0x0E76542F);
        }
    }

    private void drawPage(
            GuiGraphics graphics,
            GuidePage page,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        graphics.drawCenteredString(
                this.font,
                page.title().copy().withStyle(ChatFormatting.BOLD),
                x + PAGE_WIDTH / 2,
                y + 8,
                INK
        );
        graphics.fill(x + 18, y + 20, x + PAGE_WIDTH - 18, y + 21, GOLD);

        switch (page.style()) {
            case TEXT -> drawTextPage(graphics, page, x, y, mouseX, mouseY);
            case FLOW -> drawFlowPage(graphics, page, x, y, mouseX, mouseY);
            case GRID -> drawGridPage(graphics, page, x, y, mouseX, mouseY);
            case INDEX -> drawIndexPage(graphics, page, x, y, mouseX, mouseY);
            case STAGES -> drawStagesPage(graphics, page, x, y);
        }
    }

    private void drawTextPage(
            GuiGraphics graphics,
            GuidePage page,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        if (!page.icons().isEmpty()) {
            int iconX = x + PAGE_WIDTH / 2 - 8;
            drawItemSlot(graphics, page.icons().getFirst(), iconX, y + 29, mouseX, mouseY);
        }
        drawWrappedLines(
                graphics,
                page.lines(),
                x + 10,
                y + 57,
                PAGE_WIDTH - 20,
                10,
                y + PAGE_HEIGHT - 24
        );
    }

    private void drawFlowPage(
            GuiGraphics graphics,
            GuidePage page,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        int count = page.icons().size();
        int spacing = count > 5 ? 22 : 28;
        int rowWidth = (count - 1) * spacing + 16;
        int iconX = x + (PAGE_WIDTH - rowWidth) / 2;
        int iconY = y + 31;

        for (int index = 0; index < count; index++) {
            int currentX = iconX + index * spacing;
            drawItemSlot(graphics, page.icons().get(index), currentX, iconY, mouseX, mouseY);
            if (index < count - 1) {
                graphics.drawString(this.font, "›", currentX + 17, iconY + 4, INK_MUTED, false);
            }
        }

        drawWrappedLines(
                graphics,
                page.lines(),
                x + 10,
                y + 61,
                PAGE_WIDTH - 20,
                10,
                y + PAGE_HEIGHT - 24
        );
    }

    private void drawGridPage(
            GuiGraphics graphics,
            GuidePage page,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        int columns = page.icons().size() <= 4 ? 2 : 3;
        int cardWidth = columns == 2 ? 62 : 44;
        int gridWidth = columns * cardWidth;
        int startX = x + (PAGE_WIDTH - gridWidth) / 2;
        int startY = y + 29;
        int rows = (page.icons().size() + columns - 1) / columns;

        for (int index = 0; index < page.icons().size(); index++) {
            int column = index % columns;
            int row = index / columns;
            int cardX = startX + column * cardWidth;
            int cardY = startY + row * 33;
            drawItemSlot(graphics, page.icons().get(index), cardX + (cardWidth - 20) / 2, cardY, mouseX, mouseY);

            if (index < page.captions().size()) {
                Component caption = page.captions().get(index);
                String shortened = this.font.plainSubstrByWidth(caption.getString(), cardWidth - 2);
                graphics.drawCenteredString(
                        this.font,
                        shortened,
                        cardX + cardWidth / 2,
                        cardY + 21,
                        INK_MUTED
                );
            }
        }

        int textY = startY + rows * 33 + 2;
        drawWrappedLines(
                graphics,
                page.lines(),
                x + 10,
                textY,
                PAGE_WIDTH - 20,
                9,
                y + PAGE_HEIGHT - 24
        );
    }

    private void drawIndexPage(
            GuiGraphics graphics,
            GuidePage page,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        int startX = x + 18;
        int startY = y + 34;
        for (int index = 0; index < page.icons().size(); index++) {
            int column = index % 2;
            int row = index / 2;
            int cardX = startX + column * 62;
            int cardY = startY + row * 54;

            graphics.fill(cardX - 3, cardY - 3, cardX + 47, cardY + 41, 0x5576542F);
            graphics.fill(cardX - 2, cardY - 2, cardX + 46, cardY + 40, SLOT);
            drawItemSlot(graphics, page.icons().get(index), cardX + 14, cardY + 2, mouseX, mouseY);
            graphics.drawCenteredString(
                    this.font,
                    page.captions().get(index),
                    cardX + 22,
                    cardY + 25,
                    INK
            );
        }

        graphics.drawCenteredString(
                this.font,
                Component.translatable("guide.hookahmod.index_hint").withStyle(ChatFormatting.ITALIC),
                x + PAGE_WIDTH / 2,
                y + 151,
                INK_MUTED
        );
    }

    private void drawStagesPage(GuiGraphics graphics, GuidePage page, int x, int y) {
        int[] colors = {0xFF7BA36A, 0xFF91B85C, 0xFFD0A443, 0xFF9B5CAA, 0xFF9E3D3D};
        int startY = y + 30;

        for (int index = 0; index < page.lines().size(); index++) {
            int stageY = startY + index * 22;
            graphics.fill(x + 15, stageY, x + 29, stageY + 14, 0x6632190B);
            graphics.fill(x + 17, stageY + 2, x + 27, stageY + 12, colors[index]);
            graphics.drawString(this.font, page.lines().get(index), x + 36, stageY + 3, INK, false);
            if (index < page.lines().size() - 1) {
                graphics.fill(x + 21, stageY + 15, x + 23, stageY + 21, INK_MUTED);
            }
        }

        graphics.drawCenteredString(
                this.font,
                Component.translatable("guide.hookahmod.overdose_warning1").withStyle(ChatFormatting.DARK_RED),
                x + PAGE_WIDTH / 2,
                y + 145,
                0xFF8B2525
        );
        graphics.drawCenteredString(
                this.font,
                Component.translatable("guide.hookahmod.overdose_warning2").withStyle(ChatFormatting.DARK_RED),
                x + PAGE_WIDTH / 2,
                y + 154,
                0xFF8B2525
        );
    }

    private void drawItemSlot(
            GuiGraphics graphics,
            ItemStack stack,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        graphics.fill(x - 2, y - 2, x + 18, y + 18, 0x6676542F);
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT);
        graphics.renderItem(stack, x, y);

        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0x55FFFFFF);
            this.hoveredStack = stack;
        }
    }

    private void drawWrappedLines(
            GuiGraphics graphics,
            List<Component> lines,
            int x,
            int startY,
            int width,
            int lineHeight,
            int maxY
    ) {
        int y = startY;
        for (Component component : lines) {
            if (component.getString().isEmpty()) {
                y += lineHeight / 2;
                if (y >= maxY) {
                    return;
                }
                continue;
            }
            for (FormattedCharSequence line : this.font.split(component, width)) {
                if (y + lineHeight > maxY) {
                    return;
                }
                graphics.drawString(this.font, line, x, y, INK, false);
                y += lineHeight;
            }
        }
    }

    private void drawPageNumbers(
            GuiGraphics graphics,
            int bookX,
            int bookY,
            int leftPageIndex,
            int rightPageIndex
    ) {
        int numberY = bookY + PAGE_TOP_OFFSET + PAGE_HEIGHT - 12;
        graphics.drawCenteredString(
                this.font,
                String.valueOf(leftPageIndex + 1),
                bookX + LEFT_PAGE_OFFSET + PAGE_WIDTH / 2,
                numberY,
                INK_MUTED
        );
        if (rightPageIndex < PAGES.size()) {
            graphics.drawCenteredString(
                    this.font,
                    String.valueOf(rightPageIndex + 1),
                    bookX + RIGHT_PAGE_OFFSET + PAGE_WIDTH / 2,
                    numberY,
                    INK_MUTED
            );
        }
    }

    private void jumpToChapter(Chapter chapter) {
        setSpread(chapter.firstSpread());
    }

    private void changeSpread(int direction) {
        setSpread(this.spreadIndex + direction);
    }

    private void setSpread(int spread) {
        int maxSpread = (PAGES.size() - 1) / 2;
        this.spreadIndex = Math.max(0, Math.min(spread, maxSpread));
        updateButtons();
    }

    private void updateButtons() {
        if (this.previousButton != null) {
            this.previousButton.active = this.spreadIndex > 0;
        }
        if (this.nextButton != null) {
            this.nextButton.active = this.spreadIndex < (PAGES.size() - 1) / 2;
        }
        if (this.contentsButton != null) {
            this.contentsButton.active = this.spreadIndex != 0;
        }

        Chapter current = CHAPTERS.getFirst();
        for (Chapter chapter : CHAPTERS) {
            if (chapter.firstSpread() <= this.spreadIndex) {
                current = chapter;
            }
        }
        for (int index = 0; index < this.chapterButtons.size(); index++) {
            BookButton button = this.chapterButtons.get(index);
            boolean selected = CHAPTERS.get(index) == current;
            button.setSelected(selected);
            button.active = !selected;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D) {
            changeSpread(scrollY < 0.0D ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 263) {
            changeSpread(-1);
            return true;
        }
        if (keyCode == 262) {
            changeSpread(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
    }

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

    private record Chapter(String nameKey, int firstSpread, int color) {
    }

    private record GuidePage(
            Component title,
            List<Component> lines,
            List<ItemStack> icons,
            List<Component> captions,
            PageStyle style
    ) {
    }

    private enum PageStyle {
        TEXT,
        FLOW,
        GRID,
        INDEX,
        STAGES
    }

    private static final class BookButton extends Button {
        private final int accentColor;
        private boolean selected;

        private BookButton(
                int x,
                int y,
                int width,
                int height,
                Component message,
                OnPress onPress,
                int accentColor
        ) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.accentColor = accentColor;
        }

        private void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int background;
            if (this.selected) {
                background = this.accentColor;
            } else if (!this.active) {
                background = 0xFF6A5436;
            } else if (this.isHovered()) {
                background = 0xFFC58B43;
            } else {
                background = 0xFFE0C98D;
            }

            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), COVER_DARK);
            graphics.fill(
                    getX() + 1,
                    getY() + 1,
                    getX() + getWidth() - 1,
                    getY() + getHeight() - 1,
                    background
            );
            graphics.fill(
                    getX() + 2,
                    getY() + 2,
                    getX() + getWidth() - 2,
                    getY() + 3,
                    0x55FFFFFF
            );

            int textColor = this.selected ? 0xFFFFFFFF : (this.active ? INK : 0xFFB8A882);
            graphics.drawCenteredString(
                    MinecraftHolder.font(),
                    getMessage(),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2,
                    textColor
            );
        }
    }

    private static final class MinecraftHolder {
        private MinecraftHolder() {
        }

        private static net.minecraft.client.gui.Font font() {
            return net.minecraft.client.Minecraft.getInstance().font;
        }
    }
}
