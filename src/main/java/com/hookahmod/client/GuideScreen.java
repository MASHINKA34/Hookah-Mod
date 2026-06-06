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
            new Chapter("Обзор", 0, 0xFF9B5B28),
            new Chapter("Кальян", 1, 0xFFB56B2D),
            new Chapter("Табаки", 4, 0xFF557D3B),
            new Chapter("Эффекты", 6, 0xFF76518F),
            new Chapter("Мир", 7, 0xFF477D83)
    );

    private static final List<GuidePage> PAGES = List.of(
            textPage(
                    "Руководство по кальяну",
                    stack(ModItems.GUIDEBOOK.get()),
                    "Практическая энциклопедия Hookah Mod.",
                    "",
                    "Кальяны, табаки, эффекты, культуры и совместимость.",
                    "",
                    "Листайте стрелками, колёсиком мыши или вкладками сверху."
            ),
            indexPage(
                    "Оглавление",
                    new String[]{"Кальяны", "Табаки", "Эффекты", "Культуры"},
                    stack(ModItems.HOOKAH.get()),
                    stack(ModItems.HOOKAH_TOBACCO.get()),
                    stack(ModItems.TONOMETER.get()),
                    stack(ModItems.TOBACCO_SEED.get())
            ),

            flowPage(
                    "Быстрый старт",
                    new String[]{
                            "Поставьте кальян и установите шланг.",
                            "Заполните слоты табаком, углём и водой.",
                            "Возьмите мундштук: первый ПКМ подключает, следующий позволяет курить."
                    },
                    stack(ModItems.HOOKAH.get()),
                    stack(ModItems.SHORT_HOOKAH_HOSE.get()),
                    stack(ModItems.HOOKAH_TOBACCO.get()),
                    stack(ModItems.HOOKAH_CHARCOAL.get()),
                    stack(ModItems.HOOKAH_WATER_BOTTLE.get()),
                    stack(ModItems.HOOKAH_MOUTHPIECE.get())
            ),
            gridPage(
                    "Шланги и игроки",
                    new String[]{"5 блоков", "10 блоков", "Мундштук", "Тонометр"},
                    new String[]{
                            "Один шланг — один игрок.",
                            "Для совместного курения нужны разные шланги.",
                            "Тонометр измеряет накуренность."
                    },
                    stack(ModItems.SHORT_HOOKAH_HOSE.get()),
                    stack(ModItems.LONG_HOOKAH_HOSE.get()),
                    stack(ModItems.HOOKAH_MOUTHPIECE.get()),
                    stack(ModItems.TONOMETER.get())
            ),

            gridPage(
                    "Уровни кальяна",
                    new String[]{"Normal", "Leather", "Gold", "Iron", "Diamond", "Netherite"},
                    new String[]{
                            "Каждый следующий уровень даёт больше брони, когда кальян надет в слот нагрудника.",
                            "Тип сохраняется при установке и переносе."
                    },
                    stack(ModItems.HOOKAH.get()),
                    stack(ModItems.HOOKAH_LEATHER.get()),
                    stack(ModItems.HOOKAH_GOLD.get()),
                    stack(ModItems.HOOKAH_IRON.get()),
                    stack(ModItems.HOOKAH_DIAMOND.get()),
                    stack(ModItems.HOOKAH_NETHERITE.get())
            ),
            flowPage(
                    "Устройство кальяна",
                    new String[]{
                            "Колба, шахта и чаша образуют основной кальян.",
                            "Для работы дополнительно нужны шланг, табак, уголь и вода."
                    },
                    stack(ModItems.HOOKAH_FLASK.get()),
                    stack(ModItems.HOOKAH_SHAFT.get()),
                    stack(ModItems.HOOKAH_BOWL.get()),
                    stack(ModItems.HOOKAH.get())
            ),

            textPage(
                    "Кальян на спине",
                    stack(ModItems.HOOKAH_NETHERITE.get()),
                    "Кальян можно надеть в слот нагрудника.",
                    "",
                    "Назначенная клавиша открывает его интерфейс. Другие игроки могут подключаться к кальяну владельца.",
                    "",
                    "Сила брони зависит от уровня кальяна."
            ),
            gridPage(
                    "Расходники",
                    new String[]{"Уголь", "Вода", "Мундштук", "Короткий", "Длинный", "Контроль"},
                    new String[]{
                            "Переплавьте уголь: получите 4 кальянных угля.",
                            "Бутылка воды + 3 стекла = 2 воды для кальяна."
                    },
                    stack(ModItems.HOOKAH_CHARCOAL.get()),
                    stack(ModItems.HOOKAH_WATER_BOTTLE.get()),
                    stack(ModItems.HOOKAH_MOUTHPIECE.get()),
                    stack(ModItems.SHORT_HOOKAH_HOSE.get()),
                    stack(ModItems.LONG_HOOKAH_HOSE.get()),
                    stack(ModItems.TONOMETER.get())
            ),

            textPage(
                    "Основа табаков",
                    stack(ModItems.HOOKAH_TOBACCO.get()),
                    "Базовый табак выращивается из семян табака.",
                    "",
                    "Все смеси создаются на его основе и складываются по 64.",
                    "",
                    "Обычный табак не даёт дополнительного эффекта."
            ),
            gridPage(
                    "Полезные смеси I",
                    new String[]{"Яблоко", "Мёд", "Цитрус", "Кофе", "Мята", "Лаванда"},
                    new String[]{
                            "Регенерация 20с • Насыщение 10с",
                            "Спешка 30с • Скорость + Спешка 30с",
                            "Скорость 30с • Ночное зрение 45с"
                    },
                    stack(ModItems.TOBACCO_APPLE.get()),
                    stack(ModItems.TOBACCO_HONEY.get()),
                    stack(ModItems.TOBACCO_CITRUS.get()),
                    stack(ModItems.TOBACCO_COFFEE.get()),
                    stack(ModItems.TOBACCO_MINT.get()),
                    stack(ModItems.TOBACCO_LAVENDER.get())
            ),

            gridPage(
                    "Полезные смеси II",
                    new String[]{"Шахтёр", "Путник", "Рыбак"},
                    new String[]{
                            "Шахтёр: Ночное зрение + Спешка 45с.",
                            "Путешественник: Скорость II 25с.",
                            "Рыбак: Удача моря II 30с."
                    },
                    stack(ModItems.TOBACCO_MINER.get()),
                    stack(ModItems.TOBACCO_TRAVELER.get()),
                    stack(ModItems.TOBACCO_FISHER.get())
            ),
            gridPage(
                    "Боевые смеси",
                    new String[]{"Яд", "Огонь", "Лёд", "Лечение"},
                    new String[]{
                            "Яд отравляет ближайших врагов.",
                            "Огонь поджигает, лёд замедляет.",
                            "Лечебный табак восстанавливает здоровье ближайших игроков."
                    },
                    stack(ModItems.TOBACCO_POISON.get()),
                    stack(ModItems.TOBACCO_FIRE.get()),
                    stack(ModItems.TOBACCO_ICE.get()),
                    stack(ModItems.TOBACCO_HEAL.get())
            ),

            gridPage(
                    "Особые смеси",
                    new String[]{"Бездна", "Гашиш", "Пал Палыч"},
                    new String[]{
                            "Бездна: собственный эффект.",
                            "Гашиш: визуальный шейдер.",
                            "Пал Палыч: психоделический трип."
                    },
                    stack(ModItems.TOBACCO_ABYSS.get()),
                    stack(ModItems.TOBACCO_HASHISH.get()),
                    stack(ModItems.TOBACCO_PALPALYCH.get())
            ),
            stagesPage(
                    "Накуренность",
                    "Трезв",
                    "Расслаблен",
                    "Накурен",
                    "Трип",
                    "Передоз"
            ),

            gridPage(
                    "Культуры",
                    new String[]{"Табак", "Мята", "Лаванда", "Лист мяты", "Лаванда"},
                    new String[]{
                            "Семена сажают на грядку.",
                            "У всех культур 4 стадии роста.",
                            "Урожай нужен для табаков."
                    },
                    stack(ModItems.TOBACCO_SEED.get()),
                    stack(ModItems.MINT_SEED.get()),
                    stack(ModItems.LAVENDER_SEED.get()),
                    stack(ModItems.MINT.get()),
                    stack(ModItems.LAVENDER.get())
            ),
            gridPage(
                    "Croptopia",
                    new String[]{"Цитрус", "Кофе", "Морковь", "Какао"},
                    new String[]{
                            "С Croptopia: апельсин, лимон, лайм и coffee_beans.",
                            "Без неё: морковь и cocoa_beans."
                    },
                    stack(ModItems.TOBACCO_CITRUS.get()),
                    stack(ModItems.TOBACCO_COFFEE.get()),
                    stack(Items.CARROT),
                    stack(Items.COCOA_BEANS)
            ),

            flowPage(
                    "Полезные рецепты",
                    new String[]{
                            "Книга создаётся из обычной книги и базового табака.",
                            "Рецепты можно просмотреть через книгу рецептов верстака."
                    },
                    stack(Items.BOOK),
                    stack(ModItems.HOOKAH_TOBACCO.get()),
                    stack(ModItems.GUIDEBOOK.get())
            ),
            textPage(
                    "Добрый дым",
                    stack(ModItems.GUIDEBOOK.get()),
                    "Следите за стадией накуренности тонометром.",
                    "",
                    "Передозировка даёт отрицательные эффекты, поэтому делайте перерывы.",
                    "",
                    "Наведение на любую иконку в книге показывает название предмета."
            )
    );

    private final List<BookButton> chapterButtons = new ArrayList<>();
    private int spreadIndex;
    private BookButton previousButton;
    private BookButton nextButton;
    private BookButton contentsButton;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public GuideScreen() {
        super(Component.literal("Руководство по кальяну"));
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
                    Component.literal(chapter.name()),
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
                Component.literal("Оглавление"), button -> setSpread(0), COVER
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
                Component.literal("Выберите вкладку сверху").withStyle(ChatFormatting.ITALIC),
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
                Component.literal("Передозировка даёт").withStyle(ChatFormatting.DARK_RED),
                x + PAGE_WIDTH / 2,
                y + 145,
                0xFF8B2525
        );
        graphics.drawCenteredString(
                this.font,
                Component.literal("негативные эффекты").withStyle(ChatFormatting.DARK_RED),
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
                Component.literal(title),
                components(lines),
                List.of(icon),
                List.of(),
                PageStyle.TEXT
        );
    }

    private static GuidePage flowPage(String title, String[] lines, ItemStack... icons) {
        return new GuidePage(
                Component.literal(title),
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
                Component.literal(title),
                components(lines),
                List.of(icons),
                components(captions),
                PageStyle.GRID
        );
    }

    private static GuidePage indexPage(String title, String[] captions, ItemStack... icons) {
        return new GuidePage(
                Component.literal(title),
                List.of(),
                List.of(icons),
                components(captions),
                PageStyle.INDEX
        );
    }

    private static GuidePage stagesPage(String title, String... stages) {
        return new GuidePage(
                Component.literal(title),
                components(stages),
                List.of(stack(ModItems.TONOMETER.get())),
                List.of(),
                PageStyle.STAGES
        );
    }

    private static List<Component> components(String... lines) {
        List<Component> components = new ArrayList<>(lines.length);
        for (String line : lines) {
            components.add(Component.literal(line));
        }
        return List.copyOf(components);
    }

    private static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }

    private record Chapter(String name, int firstSpread, int color) {
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
