# ТЗ: расширение Hookah Mod (накуренность, тиры, боевые табаки, трипы)

> **Как пользоваться:** В новом чате (Claude Code в этом же репозитории) скажи: «Прочитай `HOOKAH_EXPANSION_TZ.md` и реализуй Фазу 1». Реализовывать строго по фазам: Фаза 1 → `gradlew build` → тест в `runClient` → правки → Фаза 2 и т.д. Числа в таблицах — настраиваемые дефолты, их подкрутим по ощущениям.

---

## 0. Контекст проекта (обязательно к прочтению)

- **Стек:** NeoForge 21.1.228, MC 1.21.1, Java 21, ModDevGradle 2.0.78, Gradle 8.10.2. Mod ID `hookahmod`, пакет `com.hookahmod`. Сборка: `gradlew build` (без демона). Тест: IntelliJ `runClient`.
- **Правила оформления (СТРОГО):**
  - **Без комментариев в коде** (если явно не просят).
  - **Без git-коммитов** (если явно не просят).
  - Идентификаторы/код — английский. UI-строки — русский первичный (`ru_ru.json`), английский зеркальный (`en_us.json`).
  - Предметы добываются крафтом/в творческой вкладке/через JEI — **не через `/give`**.
  - NeoForge best practices: `DeferredRegister`, `DataComponents` вместо сырого NBT где возможно, `AttachmentType` для данных игрока, payload + `StreamCodec` для сети, чёткое разделение клиент/сервер.

### Текущая архитектура и ключевые точки расширения

- **Курение** работает в двух режимах, логика выдоха в двух местах (это главные хуки для эффектов/накуренности):
  - Стационарный кальян: `HookahBlockEntity.applyExhale(ServerPlayer player, float charge)` — строки с комментарием `// Smoking buffs intentionally disabled`.
  - Носимый кальян: `WornHookah.applyExhale(ServerPlayer player, ServerPlayer wearer, ItemStack stack, float charge)`.
  - `charge` ∈ [0..1] — сила затяжки (0 = короткая, 1 = полная 5-сек).
- **Слоты кальяна** (и у блока, и у носимого): `SLOT_HOSE=0, SLOT_TOBACCO=1, SLOT_COAL=2, SLOT_WATER=3` (`HookahBlockEntity`). Табак лежит в `SLOT_TOBACCO` — отсюда определяется тип/категория табака.
- **Носимый кальян:** `HookahBlockItem implements Equipable` → `EquipmentSlot.CHEST` (слот нагрудника, решено — **остаётся слот нагрудника, без Curios**). Логика носимого — `WornHookah` (хранит предметы в `DataComponents.CONTAINER`, сессии в `ActiveSessions`). Рендер на спине — `client/HookahBackLayer`.
- **Мундштук:** `HookahMouthpieceItem` — держать ПКМ для затяжки (`use`/`onUseTick`/`releaseUsing`/`finishUsingItem` → `exhale` → `applyExhale`). GeckoLib-анимация.
- **Дым (косметика):** `smoke/HookahSmoke` — частицы выдоха + заполнение комнаты. Это атмосфера, геймплейного эффекта нет.
- **Реестры:** `registry/ModItems`, `ModBlocks`, `ModBlockEntities`, `ModCreativeTabs`, `ModMenuTypes`, `ModSounds`.
- **Сеть:** `network/NetworkHandler.register(RegisterPayloadHandlersEvent)` — паттерн payload: `TYPE` (CustomPacketPayload.Type) + `STREAM_CODEC` + `handle`. Регистрация `playToServer`/`playToClient`. Существуют `ToggleMouthpiecePayload`, `OpenWornHookahPayload`, `HookahSyncPayload`, `WornHookahSyncPayload`.
- **Клиент-сетап:** `client/ClientSetup` (`@EventBusSubscriber(Dist.CLIENT)`) — здесь регистрируются renderer'ы, слои, экраны, key-mappings, клиентский тик (`ClientTickEvent.Post` → `HookahSmokingSound.tickLocal()`).
- **Сервер-события:** `event/ServerEvents` (`@SubscribeEvent` на `NeoForge.EVENT_BUS`) — server tick, смерть/логаут/смена измерения.

---

## 1. Числовые дефолты (всё tunable)

### 1.1 Тиры кальяна (носимый, слот CHEST = аналог нагрудника)

Решено: **кожа → золото(«цыганский») → железо → алмаз → незерит**. Существующий предмет `hookah` остаётся **кожаным (базовым)** тиром — id не менять, чтобы не ломать миры/рецепты; добавить 4 новых.

| Тир | Item id | Защита (брони) | Множитель эффектов | Множитель боевого | Особое |
|---|---|---|---|---|---|
| Кожаный | `hookah` (есть) | 1 | 1.0 | 1.0 | базовый |
| Золотой (цыганский) | `hookah_gold` | 2 | **1.35** | 1.1 | низкая защита, но макс. бонус к длительности эффектов (флавор «богатый/цыганский»), блестящий |
| Железный | `hookah_iron` | 3 | 1.25 | 1.25 | — |
| Алмазный | `hookah_diamond` | 5 | 1.5 | 1.5 | — |
| Незеритовый | `hookah_netherite` | 7 | 1.75 | 1.75 | предмет огнестойкий (`DataComponents.FIRE_RESISTANT`) |

**Все кальяны НЕломаемые** — без прочности/durability (не задавать `.durability()`; можно `DataComponents.UNBREAKABLE`). Armor toughness не задаём (0). Значение брони (`Attributes.ARMOR`) — строго по таблице (низкое, т.к. это аксессуар на спине). Кальян **сам урона не наносит** — только защита + множители.

### 1.2 Накуренность (счётчик игрока)

| Диапазон | Состояние (ru) | Эффекты (дефолт, tunable) |
|---|---|---|
| 0–30 | Трезв | нет |
| 30–60 | Лёгкое расслабление | Regeneration I (короткими подкачками), лёгкое покачивание FOV |
| 60–100 | Накурен | Regeneration I + Slowness I, заметное покачивание, приглушение звука |
| 100–150 | Трип | Nausea (искажение экрана) + клиентские визэффекты; **сущности-видения только если курится редкий табак** |
| 150+ | Передоз | Nausea II + Weakness + Hunger, вспышки Blindness, сильное искажение; без мгновенной смерти |

- **Прирост за выдох:** `gain = tobacco.intoxication * (0.4 + 0.6*charge)`. Обычный табак ~16, боевой ~4, редкий «Табак Бездны» ~28 (см. §5).
- **Спад (decay):** `-1` каждые `20` тиков (≈ -1/сек) в простое; не курит → плавно трезвеет.
- **Кап:** мягкий максимум ~220.
- Значение хранится на игроке через **`AttachmentType<Float>`** (см. §2), сохраняется при выходе, обнуляется/уменьшается при смерти (`copyOnDeath` = частично или сброс — на выбор, дефолт сброс в 0).

### 1.3 Боевой дым (конус по взгляду — решено)

- **Дальность:** `4.0 + tierCombatMult*1.5 + charge*2.0` блоков.
- **Полу-угол конуса:** ~25°. Цель в конусе: `lookVec · normalize(targetEyes - smokerEyes) >= cos(halfAngle)` и в пределах дальности, есть линия видимости.
- **Расход:** боевой табак тратится быстрее — 1 шт. за 10 выдохов (tunable; обычный — текущие 20).
- Эффекты по типам — см. §5.3. Сила/длительность × `tierCombatMult`.

---

## 2. ФАЗА 1 — Накуренность + Тонометр

**Цель:** ядро механики. Самодостаточно, тестируется первым.

### 2.1 Данные игрока (накуренность)
- Новый пакет `com.hookahmod.smoking`.
- `ModAttachments`: `DeferredRegister<AttachmentType<?>>` на `NeoForgeRegistries.Keys.ATTACHMENT_TYPES`. Зарегистрировать в `HookahMod` конструкторе.
  - `INTOXICATION = AttachmentType.builder(() -> 0.0f).serialize(Codec.FLOAT /* или встроенный */).copyOnDeath(false?).build()`.
- `IntoxicationState` (util): статические методы `get(Player)`, `add(ServerPlayer, float)`, `setAndSync(ServerPlayer, float)`, `decayTick(ServerPlayer)`, `band(float) -> IntoxicationBand`.
- `enum IntoxicationBand { SOBER, RELAXED, HIGH, TRIP, OVERDOSE }` с границами из §1.2 и ключом локализации состояния.

### 2.2 Тик и эффекты
- В `ServerEvents` добавить серверный тик игроков: каждые N тиков `IntoxicationState.decayTick` + применение эффектов диапазона (повесить vanilla `MobEffect` согласно §1.2 с коротким сроком, обновлять).
- Прирост накуренности — в `applyExhale` (оба места, см. §0): после определения табака `IntoxicationState.add(player, gain)` и `setAndSync`.

### 2.3 Синхронизация на клиент
- Payload S→C `IntoxicationSyncPayload(float value)` (паттерн как существующие, регистрировать `playToClient` в `NetworkHandler`). Слать только владельцу при изменении.
- На клиенте — зеркало значения (статика в `client`), читается тонометром/HUD/трип-системой.
- (Опц.) если версия NeoForge поддерживает авто-sync attachment'ов — можно вместо payload, но manual payload надёжнее; оставить payload.

### 2.4 Тонометр
- Предмет `TonometerItem` (id `tonometer`), `stacksTo(1)`, в `ModItems` + творческую вкладку + рецепт (напр. железо + редстоун + стекло; добавить в `data/hookahmod/recipes/`).
- ПКМ (`use`): показать в action-bar `Component.translatable("message.hookahmod.tonometer", value, stateName)` — **только уровень накуренности и состояние**, ничего больше.
- (Опц.) маленький HUD-индикатор, пока тонометр в руке: `RegisterGuiLayersEvent` (1.21.1) → отрисовать число/полоску. По умолчанию реализовать action-bar; HUD — если останется время.
- Текстура: см. §8 (ставится как есть, без ресайза).

### 2.5 Критерий готовности Фазы 1
Затяжки повышают накуренность; в простое падает; пороги дают эффекты; тонометр в action-bar корректно показывает значение и состояние на русском. Сборка зелёная.

---

## 3. ФАЗА 2 — Прогрессия кальянов (тиры + защита)

### 3.1 Тир как данные
- `enum HookahTier { LEATHER, GOLD, IRON, DIAMOND, NETHERITE }` (пакет `item`): поля `armor, effectMult, combatMult, fireResistant`, id-суффикс, ключ локализации.
- Интерфейс-маркер `TieredHookahItem { HookahTier tier(); }`. `HookahBlockItem` реализует, базовый возвращает `LEATHER`. Для новых тиров — конструктор с тиром.

### 3.2 Регистрация предметов
- В `ModItems` добавить `HOOKAH_GOLD/IRON/DIAMOND/NETHERITE` (каждый — `HookahBlockItem` с нужным `HookahTier`).
- Атрибуты брони задать через `DataComponents.ATTRIBUTE_MODIFIERS` (`Attributes.ARMOR`, scope `EquipmentSlotGroup.CHEST`) по таблице §1.1; **прочность не задавать** — кальяны неломаемые (`DataComponents.UNBREAKABLE` или просто без `.durability()`), toughness не используем; для незерита — `.fireResistant()`.
- Все тиры по-прежнему `Equipable` → CHEST, ставят блок кальяна (визуал блока можно общий или по тиру — см. §8). Добавить в творческую вкладку и рецепты (апгрейд-крафт: предыдущий тир + слитки материала; незерит — через smithing с незерит-слитком, как ванила).

### 3.3 Обобщить `WornHookah`
- Сейчас `isHookahStack` завязан на `ModItems.HOOKAH`. Сделать: `isHookahStack` = `stack.getItem() instanceof TieredHookahItem`. Везде, где берётся тир — читать из предмета.
- `HookahBackLayer`: рендерить модель/текстуру по тиру носимого предмета (минимум — тинт/разные текстуры блока).

### 3.4 Связь с эффектами
- В `applyExhale` множители `effectMult`/`combatMult` берутся из тира носимого кальяна. Для **стационарного блока** тир = `LEATHER` (база), множители 1.0.

### 3.5 Критерий готовности
5 кальянов крафтятся по цепочке, дают разную защиту/прочность, на спине выглядят по тиру, множитель усиливает длительность/силу эффектов табака. Золотой = «цыганский» (блестящий, макс. бонус длительности, слабая защита).

---

## 4. ФАЗА 3 — Боевые табаки

### 4.1 Категории табака (рефактор табака под стратегию)
- `enum TobaccoCategory { REGULAR, COMBAT, RARE }`.
- Базовый класс `AbstractTobaccoItem extends Item`: поля `category`, `int intoxication`, метод `onExhale(ServerLevel level, ServerPlayer smoker, float charge, float effectMult, float combatMult)` (стратегия). `appendHoverText` — общий (как сейчас у `HookahTobaccoItem`).
- Существующий `HookahTobaccoItem` → `REGULAR`, базовый (или сделать его `extends AbstractTobaccoItem`).
- В `applyExhale` (оба места): достать `SLOT_TOBACCO`, если `instanceof AbstractTobaccoItem t` → `t.onExhale(...)` (передать множители тира). Это заменяет блок `// Smoking buffs intentionally disabled`.

### 4.2 Конус-хелпер
- `combat/SmokeCone`: `applyCone(ServerLevel, ServerPlayer smoker, double range, double halfAngleDeg, Predicate<LivingEntity> filter, Consumer<LivingEntity> effect)` — собрать `getEntitiesOfClass(LivingEntity, AABB вокруг smoker, filter)`, отфильтровать по углу/дальности/линии видимости, применить эффект. Партиклы выдоха перекрасить под тип (через существующий `HookahSmoke`/`ParticleTypes`).

### 4.3 Боевые табаки (4 шт., id и эффект)
| Item id | Назв. ru | Эффект в конусе (× combatMult) |
|---|---|---|
| `tobacco_poison` | Ядовитый табак | Poison I, (2+tier) сек врагам |
| `tobacco_fire` | Огненный табак | поджиг 3+tier сек + малый огненный урон (огнемёт) |
| `tobacco_ice` | Ледяной табак | Slowness II + Mining Fatigue I, кратко; (опц. freeze-тики) |
| `tobacco_heal` | Лечебный табак | Regeneration II **только союзникам** (игроки/прирученные/жители) + лечит самого себя; враги игнорируются |

- Расход боевого табака — быстрее: 1 шт. за 10 выдохов (обычный — 20).
- Боевые добавляют мало накуренности (intoxication ~4).

### 4.4 Критерий готовности
Зарядив боевой табак, на выдохе летит окрашенный конус, враги в конусе получают эффект, сила/длительность растёт с тиром кальяна; лечебный лечит союзников, не вредит врагам. Кальян сам урона не наносит.

---

## 5. Табаки — полный список (свод)

### 5.1 Обычные (REGULAR) — баффы/атмосфера
- `hookah_tobacco` (есть) — базовый, лёгкий (intoxication ~16, без спец-эффекта или слабый Regen).
- (Расширяемо позже: мятный→Speed, яблочный→Saturation и т.п. — не обязательно в v1.)

### 5.2 Редкие (RARE) — трипы/видения
- `tobacco_abyss` (**Табак Бездны**) — intoxication ~28; при выдохе ставит игроку клиентский флаг «активны видения» (см. Фаза 4). Добывается редко (лут данжей/структур — задел; в v1 — рецепт или творческая вкладка). Тёмно-фиолетовый «пустотный» вид.

### 5.3 Боевые (COMBAT) — см. §4.3.

### 5.4 Как код различает категорию
Через класс предмета (`AbstractTobaccoItem.category()`), не через теги/NBT. Чисто и расширяемо.

---

## 6. ФАЗА 4 — Трипы и галлюцинации (только клиент, только для одного игрока, невидимы другим)

> Самая экспериментальная часть. Всё рендерится **только на клиенте локального игрока**, на сервере сущностей-видений нет, другие игроки их не видят.

### 6.1 Триггеры
- **Экранные искажения** (от ЛЮБОГО табака): когда `band >= TRIP` (накуренность 100+). Реализовать через Nausea-эффект + усиление: дрожание FOV, лёгкая цветовая аберрация/туман, частицы-марево. Передоз (150+) — сильнее + вспышки Blindness.
- **Сущности-видения** (только редкий табак): сервер при выдохе с `tobacco_abyss` в диапазоне трипа шлёт владельцу payload S→C `TripEventPayload(visionType, seed/params)`. Клиент запускает конкретное видение.

### 6.2 Клиентский менеджер
- Пакет `client/trip`. `TripManager` тикается в `ClientTickEvent.Post` (зарегистрировать в `ClientSetup`): хранит активные видения, их кулдауны/таймеры, спавнит/двигает/удаляет.
- Рендер видений — `RenderLevelStageEvent` (`Stage.AFTER_ENTITIES`) через `EntityRenderDispatcher` по dummy-сущности или прямой отрисовкой модели с текстурой. Видения **не добавляются в мир как настоящие Entity** (или добавляются только в `clientLevel` без серверной синхронизации) — без AI, урона, коллизий.

### 6.3 Виды видений (v1 — все выбраны)
1. **Наблюдатель вдали** — высокий тёмный силуэт появляется на расстоянии в линии видимости (предпочт. у деревьев), стоит/смотрит, **исчезает при приближении** (дистанция < ~12 блоков → fade out).
2. **Ложные силуэты мобов** — призрачные мобы (зомби/скелет и т.п.), мерцают и пропадают, без урона.
3. **Призрачные копии игрока** — двойники локального игрока, повторяют/зеркалят его движения, затем тают.
4. **Изменения неба/мира** — сдвиг цвета неба/тумана, доп. частицы, лёгкие визуальные сдвиги (дёшево, через рендер-события/тинт).
5. **Бегущий силуэт (спец-сцена)** — силуэт **бежит к игроку по прямой**, может «открыть дверь» (клиентский визуальный тоггл состояния двери на пути, затем возврат), **добегает вплотную, лицом к лицу** (чтобы игрок точно увидел), стоит миг — затем исчезает. **Текстуру предоставит пользователь** → положить в `assets/hookahmod/textures/entity/vision_runner.png` (имя подтвердить у пользователя). Без урона, без реальной коллизии (визуальное наложение).

### 6.4 Сеть
- `TripEventPayload` (S→C, только владельцу). Поля: тип видения + сид/параметры (позиция-направление вычислит клиент относительно игрока).
- Накуренность уже синхронизирована (Фаза 1) — пороговые искажения клиент включает сам по синку.

### 6.5 Критерий готовности
В трип-диапазоне экран искажается; покурив Табак Бездны, игрок видит Наблюдателя/ложных мобов/копии/бегущий силуэт — **только он сам**, другие игроки ничего не видят, урона нет. Бегущий силуэт доходит вплотную и виден в упор.

---

## 7. Сеть — итог новых payload'ов
- `IntoxicationSyncPayload` (S→C, владельцу) — Фаза 1.
- `TripEventPayload` (S→C, владельцу) — Фаза 4.
Регистрировать в `NetworkHandler.register` по существующему паттерну (`TYPE`, `STREAM_CODEC`, `handle`, `playToClient`).

---

## 8. Текстуры и модели

### 8.1 Что нужно
- **Тиры (Фаза 2):** отдельные плоские иконки НЕ нужны — иконка кальяна берётся из 3D-модели (`models/item/hookah.json` наследует `block/hookah`), поэтому достаточно перекрасить текстуру блока. Геометрия общая для всех тиров (`models/block/hookah.json`), меняется только текстура. Подробности и UV-регионы — §8.5.
- **Тонометр (Фаза 1):** иконка предмета.
- **Боевые табаки (Фаза 3):** 4 иконки.
- **Табак Бездны (Фаза 4):** 1 иконка.
- **Видения (Фаза 4):** `vision_runner.png` — **даёт пользователь**. Наблюдатель/ложные мобы/копии — переиспользовать ваниль-рендер или временные текстуры (уточнить у пользователя).

### 8.2 Workflow текстур
Пользователь предоставляет готовые текстуры — **ставить как есть, без даунскейла/ресайза** (НЕ ужимать до 16×16). Просто положить файл в нужную папку `assets/hookahmod/textures/...` под правильным именем. Если у файла двойное расширение `*.png.png` — только переименовать в `.png`, содержимое не трогать. Размеры «16x16» в промтах §8.3 — это лишь указание стиля (пиксель-арт), а не итоговое разрешение файла.

### 8.3 Готовые AI-промты (строгий формат пользователя)

> Промты на 4 кальяна ниже — **опциональны**: нужны только если захочешь полностью кастомные текстуры вместо перекраски существующей (рекомендованный путь — §8.5). Промты на тонометр и табаки — актуальны (это плоские предметы).
- **Золотой кальян (цыганский):** `true 16x16 pixel art, minecraft vanilla item icon, ornate golden hookah, shiny brass-gold glass base flask with engraved patterns, gypsy luxury style, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: deep gold #b8860b, bright gold #ffd54a, pale highlight #fff3b0, shadow #5c3d00, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Железный кальян:** `true 16x16 pixel art, minecraft vanilla item icon, iron hookah, grey metal glass base flask with steel shaft, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: dark iron #6b6b6b, light steel #c8c8c8, pale highlight #eeeeee, shadow #3a3a3a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Алмазный кальян:** `true 16x16 pixel art, minecraft vanilla item icon, diamond hookah, cyan crystalline glass base flask glowing softly, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: diamond cyan #4ad6d6, bright cyan #9ff5f5, pale highlight #ddffff, shadow #1f6e6e, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Незеритовый кальян:** `true 16x16 pixel art, minecraft vanilla item icon, netherite hookah, dark grey-black metal glass base flask with purple-tinged trim, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: netherite black #3b3539, dark grey #5a5258, purple trim #6a4a7a, pale highlight #9a909a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Тонометр:** `true 16x16 pixel art, minecraft vanilla item icon, blood pressure gauge tonometer, round brass dial meter with needle and a small rubber bulb, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: brass #c79a3b, white dial #f0f0e8, red needle #d23a3a, black bulb #2a2a2a, shadow #5c4410, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Ядовитый табак:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of toxic green tobacco leaves, small open bag with bright green shredded leaves and faint poison drip, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: toxic green #5fc23a, dark green #2f6e1f, sack tan #b89a6a, shadow #4a3a20, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Огненный табак:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of fiery tobacco, small bag with glowing orange-red embers in shredded leaves, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: ember orange #ff7a1a, hot red #d2331a, yellow glow #ffd24a, sack tan #b89a6a, shadow #4a2a10, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Ледяной табак:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of frozen tobacco, small bag with pale cyan frosted shredded leaves and ice crystals, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: ice cyan #9fe8f5, white frost #e8ffff, blue shadow #4a8fb0, sack tan #b89a6a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Лечебный табак:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of healing tobacco, small bag with soft pink-white shredded leaves and gentle sparkle, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: heal pink #f5a8c8, white #fff0f5, soft red #d2607a, sack tan #b89a6a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Табак Бездны:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of void tobacco, small dark bag with deep purple-black shredded leaves emitting faint starry void glow, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: void purple #6a2a8a, dark indigo #2a1a3a, star white #e8d0ff, sack dark #3a2a3a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`

### 8.4 3D-модели
**План:** позже для каждого тира будут отдельные 3D-модели (у пользователя есть 3D-модельер, сделает модельки на все кальяны). **Сейчас — только перекраска текстур (§8.5).** Поэтому структуру сразу делать совместимой: per-tier модели лежат в отдельных файлах `models/block/hookah_<tier>.json`. Когда придут настоящие модели — их геометрия просто заменяет содержимое этих файлов; код, блокстейты и `HookahBackLayer` (ссылаются на per-tier модель по тиру) менять НЕ нужно. До тех пор все `hookah_<tier>.json` = общая геометрия + своя перекрашенная текстура.

### 8.5 Перекраска тиров кальяна — рекомендованный путь (без Blockbench)
**Blockbench не нужен** — геометрия одна на все тиры, меняется только текстура (а с ней автоматически и иконка в инвентаре). Колба и металл — отдельные UV-регионы, так что красить можно именно их.

- **UV-регионы в `hookah.png` (16×16):** колба `glass_body` = `[0,4]–[6,8]`; стержни/«дерево» ≈ `[2,1.25]–[4,3]`; база/чаша/коннекторы ≈ `[10,4]–[12,4.75]` и `[6,4]–[8,5.5]`.
- **Шаги:**
  1. Сгенерировать 4 перекрашенные копии текстуры: `textures/block/hookah_gold.png`, `_iron.png`, `_diamond.png`, `_netherite.png` — **скриптом** (PowerShell + System.Drawing: palette-remap нужных регионов под палитру тира из §1.1/§8.3), без ручной работы. Позже можно заменить на свои AI-текстуры с теми же именами.
  2. По 4 мини-модели блока: копия `hookah.json` с заменой только `"textures"` → `{ "0": "hookahmod:block/hookah_<tier>", "particle": "hookahmod:block/hookah_<tier>" }` → `models/block/hookah_<tier>.json`.
  3. Item-модели: `models/item/hookah_<tier>.json` = `{ "parent": "hookahmod:block/hookah_<tier>" }` → иконка в инвентаре появится сама.
  4. `HookahBackLayer` (рендер на спине) и рендер блока выбирают модель/blockstate по тиру предмета.
- **Дефолт перекраски (легко поменять):** колба = цвет тира; металл (база/стержни/чаша/коннекторы) — в тон тира (золото — весь золотой/«цыганский»; железо — стальной; незерит — тёмный); алмаз — голубой кристалл; кожаный — текущая текстура без изменений.
- **Нюанс:** у непрозрачных металлических тиров (золото/железо/незерит) колба перекроет внутренний водяной кубоид — воду внутри видно не будет (норм для металлической базы). Кожаный/алмаз можно оставить полупрозрачными.
- **Альтернатива (НЕ рекомендуется):** одна текстура + цвет в коде (`tintindex` + `RegisterColorHandlersEvent` для item/block). Минусы: колбу надо обесцветить (иначе цвет грязный); для носимого кальяна `BlockRenderDispatcher.renderSingleBlock` НЕ применяет block-тинты — пришлось бы тинтить вручную в `HookahBackLayer`. Качество хуже — нельзя передать блики золота/свечение алмаза/тёмный незерит одним умножением цвета.

### 8.6 Имена файлов текстур (чтобы агент сам нашёл и сопоставил)
**Правило:** имя файла = id предмета/текстуры из ТЗ. По имени агент поймёт назначение и положит в нужную папку. Класть в `C:\Users\mashi\Downloads\`. Двойное расширение `*.png.png` — ок, агент переименует в `.png`. Разрешение — любое, без ресайза (§8.2).

| Что | Имя файла | Куда (внутри `assets/hookahmod/textures/`) |
|---|---|---|
| Тонометр (Фаза 1) | `tonometer.png` | `item/tonometer.png` |
| Ядовитый табак (Фаза 3) | `tobacco_poison.png` | `item/tobacco_poison.png` |
| Огненный табак (Фаза 3) | `tobacco_fire.png` | `item/tobacco_fire.png` |
| Ледяной табак (Фаза 3) | `tobacco_ice.png` | `item/tobacco_ice.png` |
| Лечебный табак (Фаза 3) | `tobacco_heal.png` | `item/tobacco_heal.png` |
| Табак Бездны (Фаза 4) | `tobacco_abyss.png` | `item/tobacco_abyss.png` |
| Бегущий силуэт-видение (Фаза 4) | `vision_runner.png` | `entity/vision_runner.png` |
| Золотой кальян — опц.* | `hookah_gold.png` | `block/hookah_gold.png` |
| Железный кальян — опц.* | `hookah_iron.png` | `block/hookah_iron.png` |
| Алмазный кальян — опц.* | `hookah_diamond.png` | `block/hookah_diamond.png` |
| Незеритовый кальян — опц.* | `hookah_netherite.png` | `block/hookah_netherite.png` |

\* Тиры кальяна (Фаза 2) по умолчанию генерятся скриптом перекраски (§8.5) — свой файл нужен ТОЛЬКО если хочешь кастомную текстуру вместо авто-перекраски; имя — как в таблице. Будущие модели от 3D-модельера класть под тем же суффиксом тира (`hookah_<tier>`, §8.4).

---

## 9. Локализация
Все новые строки — `ru_ru.json` (первично) + `en_us.json` (зеркало): имена предметов (4 кальяна, тонометр, 4 боевых табака, Табак Бездны), `message.hookahmod.tonometer`, названия состояний накуренности (Трезв/Лёгкое расслабление/Накурен/Трип/Передоз), тултипы расхода боевых табаков, при необходимости — подсказки трипов.

---

## 10. Порядок реализации
1. **Фаза 1** (накуренность + тонометр) — ядро, тест в `runClient`.
2. **Фаза 2** (тиры + защита).
3. **Фаза 3** (боевые табаки).
4. **Фаза 4** (трипы/галлюцинации) — последней, самая сложная.

Каждая фаза: код → `gradlew build` (зелёная сборка) → пользователь тестит в `runClient` → правки по скриншотам. Не лезть в фазу N+1, пока N не подтверждена.
