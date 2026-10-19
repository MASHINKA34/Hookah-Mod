# Hookah Mod

Craftable hookahs for Minecraft. Set one up, fill it with tobacco, charcoal and water, and smoke it with your friends.
Sixteen tobacco blends give you buffs, turn your exhale into a combat cone or send you on a trip,
and an intoxication meter keeps track of how much you have smoked.

## Getting started

1. Craft a bowl, a shaft and a flask, combine them into a hookah and place it.
2. Open the hookah and attach a hose: short (5 blocks) or long (10 blocks).
3. Add tobacco, Hookah Charcoal and a Hookah Water Bottle.
4. With a mouthpiece in your inventory, press **Take** in the menu. Then hold right-click with the mouthpiece in hand to draw and release to exhale.

A longer draw makes a bigger cloud. Tobacco and charcoal last 20 puffs, water lasts 200.
One hose serves one player at a time.

The **Hookah Guide** (a book plus tobacco) explains everything in game.
With JEI installed, every recipe is shown there.

## Features

- **Six hookah tiers** - normal, leather, gold, iron, diamond and netherite. Upgrading a hookah keeps everything inside it.
- **Wear your hookah** - shift-right-click a placed hookah with an empty hand to put it in your chest slot.
  It gives armor by tier, **Z** opens its menu, and other players can connect to it.
  Shift-right-click a block with both hands empty to put it back down.
- **Useful blends** - each puff adds to the effect time instead of resetting it:
  - Apple - Regeneration
  - Honey - Saturation
  - Citrus - Haste
  - Coffee - Speed and Haste
  - Mint - Speed
  - Lavender - Night Vision
  - Miner's - Night Vision and Haste
  - Traveler's - Speed II
  - Fisher's - Sea Luck II, a new effect that helps with fishing
- **Combat blends** - poison, fire and ice smoke hit enemies in front of you, healing smoke restores nearby players.
  Fire can set things alight and ice can freeze water. Both respect claims, PvP rules and adventure mode,
  and can be limited or turned off in the server config.
- **Special blends** - Abyss (visions in the world), Hashish (a spiral screen effect with music)
  and Pal Palych (a full-screen video trip).
- **Intoxication** - Sober, Relaxed (Regeneration), High (Slowness), Trip (Nausea and trip visuals)
  and Overdose (Nausea, Weakness, Hunger and short blindness). The **Tonometer** shows your level, and it goes down over time.
- **Smoke** - exhaled smoke takes the color of the blend, lingers, drifts and fills closed rooms.
  Everyone nearby hears the hookah bubbling.
- **Crops** - tobacco, mint and lavender. Their seeds drop from grass.
- **Extras** - White Monster and Sweet Water drinks from the brewing stand, and chickens that sometimes leave Chicken Poop.

## Compatibility

- **Required:** Minecraft 1.21.1, NeoForge 21.1, GeckoLib 4.7 or newer. Install the mod on both the client and the server.
- **Optional:**
  - JEI - shows all recipes.
  - Create - filling, emptying and mixing recipes for Sweet Water and White Monster.
  - Croptopia - citrus and coffee blends use its fruit and coffee beans.

## Configuration

- `hookahmod-server.toml` - per world, synced to players: hose reach, puff timing, consumption,
  intoxication bands, combat smoke and room smoke.
- `hookahmod-client.toml` - per player: trip visuals.

## Photosensitivity warning

Trips include flashing images, a jump scare, a full-screen video, a spiral shader and camera sway.
Each of them can be turned off in `hookahmod-client.toml`. `trips.enabled = false` turns off all of them at once.

## Notes

- Some of the textures were made with the help of AI image tools.
- Bugs and suggestions: [GitHub Issues](https://github.com/MASHINKA34/Hookah-Mod/issues)

---

# Hookah Mod (RU)

Кальяны, которые можно скрафтить. Поставь кальян, заправь его табаком, углём и водой и кури вместе с друзьями.
Шестнадцать табаков дают бафы, превращают выдох в боевой конус или отправляют в трип,
а шкала накуренности следит за тем, сколько ты выкурил.

## С чего начать

1. Скрафти чашу, шахту и колбу, собери из них кальян и поставь его.
2. Открой кальян и поставь шланг: короткий (5 блоков) или длинный (10 блоков).
3. Положи табак, кальянный уголь и воду для кальяна.
4. С мундштуком в инвентаре нажми **Взять** в меню. Затем держи ПКМ с мундштуком в руке, чтобы затянуться, и отпусти, чтобы выдохнуть.

Чем дольше затяжка, тем больше облако. Табака и угля хватает на 20 затяжек, воды - на 200.
Один шланг - один игрок.

**Руководство по кальяну** (книга и табак) рассказывает обо всём прямо в игре.
Если установлен JEI, все рецепты видны в нём.

## Возможности

- **Шесть уровней кальяна** - обычный, кожаный, золотой, железный, алмазный и незеритовый. При улучшении всё содержимое сохраняется.
- **Кальян на спине** - Shift+ПКМ пустой рукой по поставленному кальяну надевает его в слот нагрудника.
  Он даёт броню по уровню, **Z** открывает его меню, к нему могут подключаться другие игроки.
  Shift+ПКМ по блоку с пустыми руками ставит его обратно.
- **Полезные табаки** - каждая затяжка добавляет время эффекта, а не сбрасывает его:
  - Яблочный - Регенерация
  - Медовый - Насыщение
  - Цитрусовый - Спешка
  - Кофейный - Скорость и Спешка
  - Мятный - Скорость
  - Лавандовый - Ночное зрение
  - Шахтёрский - Ночное зрение и Спешка
  - Табак путешественника - Скорость II
  - Рыбацкий - Удача моря II, новый эффект для рыбалки
- **Боевые табаки** - ядовитый, огненный и ледяной дым бьют врагов перед тобой, лечебный лечит игроков рядом.
  Огонь может поджигать, лёд - замораживать воду. Оба учитывают приваты, правила PvP и режим приключения,
  их можно ограничить или выключить в серверном конфиге.
- **Особые табаки** - Табак Бездны (видения в мире), Табак с гашишем (спиральный эффект на экране с музыкой)
  и Пал Палыч (полноэкранный видео-трип).
- **Накуренность** - Трезв, Расслаблен (Регенерация), Накурен (Замедление), Трип (Тошнота и визуальные эффекты)
  и Передоз (Тошнота, Слабость, Голод и короткая слепота). **Тонометр** показывает уровень, со временем он снижается.
- **Дым** - выдох окрашивается в цвет табака, висит в воздухе, дрейфует и заполняет закрытые комнаты.
  Бульканье кальяна слышат все рядом.
- **Растения** - табак, мята и лаванда. Семена выпадают из травы.
- **Разное** - напитки Белый Монстр и Сладкая вода из варочной стойки и куры, которые иногда оставляют Куриный помёт.

## Совместимость

- **Обязательно:** Minecraft 1.21.1, NeoForge 21.1, GeckoLib 4.7 или новее. Мод ставится и на клиент, и на сервер.
- **Необязательно:**
  - JEI - показывает все рецепты.
  - Create - рецепты наполнения, опустошения и смешивания для Сладкой воды и Белого Монстра.
  - Croptopia - цитрусовый и кофейный табаки делаются из её фруктов и кофейных зёрен.

## Настройки

- `hookahmod-server.toml` - для каждого мира, синхронизируется с игроками: длина шлангов, темп затяжек, расход,
  пороги накуренности, боевой дым и дым в комнатах.
- `hookahmod-client.toml` - у каждого игрока свой: визуальные эффекты трипов.

## Предупреждение о вспышках

В трипах есть мигающие картинки, скример, полноэкранное видео, спиральный шейдер и качание камеры.
Каждый эффект выключается в `hookahmod-client.toml`, а `trips.enabled = false` выключает все сразу.

## Примечания

- Часть текстур сделана с помощью ИИ-инструментов для изображений.
- Баги и предложения: [GitHub Issues](https://github.com/MASHINKA34/Hookah-Mod/issues)
