# ТЗ: Обычные табаки (баффы со стакающейся длительностью) + кастомные растения

> **Как пользоваться:** В новом чате (Claude Code в этом репозитории) скажи: «Прочитай `HOOKAH_EXPANSION_TZ.md` и реализуй Фазу 1». Реализовывать строго по фазам: Фаза 1 → `gradlew build` → тест в `runClient` → правки → Фаза 2 и т.д. Числа в таблицах — настраиваемые дефолты, подкрутим по ощущениям.

---

## 0. Контекст проекта (обязательно к прочтению)

- **Стек:** NeoForge **21.1.233** (целевая сборка kal), MC 1.21.1, Java 21, ModDevGradle 2.0.78, Gradle 8.10.2. Mod ID `hookahmod`, пакет `com.hookahmod`. Сборка: `gradlew build` (без демона). Тест: IntelliJ `runClient`.
- **Версия NeoForge — переписывать мод НЕ нужно.** Сборка kal обновилась 21.1.228 → **21.1.233**: тот же MC 1.21.1, та же ветка 21.1.x, это патч-апдейт (API-совместим, без слома). Текущий `neo_version_range=[21.1.0,21.2)` уже включает 233 → мод грузится как есть. Единственная необязательная правка (чтобы компилировать против точной версии сборки): в `gradle.properties` заменить `neo_version=21.1.228` на `neo_version=21.1.233`. Миграции кода нет.
- **Целевая сборка:** `C:\Users\mashi\curseforge\minecraft\Instances\kal`. Установлен **Croptopia** (`croptopia-neoforge-1.21.1-4.2.4`) и **Farmer's Delight**. Из Croptopia используем `croptopia:coffee_beans` и `croptopia:orange`. **Мяты и лаванды в сборке нет — делаем своими кастомными растениями** (Фаза 2).
- **Правила оформления (СТРОГО):**
  - **Без комментариев в коде.** **Без git-коммитов.** (Если явно не просят — не делать.)
  - Идентификаторы/код — английский. UI-строки — русский первичный (`ru_ru.json`), английский зеркальный (`en_us.json`).
  - Предметы добываются крафтом / в творческой вкладке / через JEI — **не через `/give`**.
  - NeoForge best practices: `DeferredRegister`, `DataComponents`, `AttachmentType`, payload+`StreamCodec` для сети, чёткое разделение клиент/сервер.

### Что УЖЕ есть (не ломать, переиспользовать)

- **База табаков:** `item/AbstractTobaccoItem` — поля `category` (`TobaccoCategory.REGULAR|COMBAT|RARE`), `intoxication` (надымленность за полную затяжку), методы:
  - `void onExhale(ServerLevel level, ServerPlayer smoker, float charge, float effectMult, float combatMult)` — стратегия эффекта (по умолчанию пусто).
  - `@Nullable Vector3f smokeColor()` — цвет дыма.
  - `appendHoverText(...)` — общий тултип расхода (Shift).
- **Ядро выдоха уже вызывает всё нужное** (правок ядра по эффектам НЕ требуется). В обоих местах:
  - `block/HookahBlockEntity.applyExhale(ServerPlayer, float charge)` (~стр. 203–211): `IntoxicationState.add(player, gain(tobacco.intoxication(), charge)); tobacco.onExhale(server, player, charge, tier.effectMult(), tier.combatMult());`
  - `item/WornHookah.applyExhale(...)` (~стр. 177–182): то же.
  - Тир определяется автоматически: блок — из `HookahBlock.TIER` (стационарный = `NORMAL`, множитель 1.0); носимый — `HookahTier.fromStack(stack)`.
- **Множители тира:** `item/HookahTier` — `effectMult`: NORMAL/LEATHER 1.0, GOLD 1.35, IRON 1.25, DIAMOND 1.5, NETHERITE 1.75. Для баф-табаков используем `effectMult` (длительность).
- **Расход:** `REGULAR` уже тратится 1 шт. за 20 затяжек (`PUFFS_PER_SOLID`), `COMBAT` — за 10. Наши новые табаки — `REGULAR`, расход 20 (ничего менять не нужно).
- **Регистрация-паттерны:** `registry/ModItems` (`DeferredItem`), `registry/ModBlocks` (`DeferredBlock`), `registry/ModCreativeTabs` (`output.accept(...)`), рецепты `data/hookahmod/recipes/<id>.json` (shapeless), модели предметов `assets/hookahmod/models/item/<id>.json` (`parent minecraft:item/generated`, `layer0 hookahmod:item/<id>`), лут блоков `data/hookahmod/loot_tables/blocks/<id>.json`, блокстейты `assets/hookahmod/blockstates/<id>.json`, локализация `assets/hookahmod/lang/{ru_ru,en_us}.json`. Регистры подключаются в `HookahMod` (конструктор).
- **Образцы для копирования стиля:** `item/CombatTobaccoItem` (стратегия `onExhale`+конус), `item/AbyssTobaccoItem`, рецепт `recipes/tobacco_poison.json`, модель `models/item/tobacco_poison.json`.

---

## 1. Ключевая механика — баффы со стакающейся ДЛИТЕЛЬНОСТЬЮ

Главная фича. Обычный табак при выдохе даёт ванильный `MobEffect`. При **повторной затяжке длительность НЕ сбрасывается, а ПРИБАВЛЯЕТСЯ** к остатку: Скорость 30 c → ещё затяжка → 60 c → ещё → 90 c и т.д. Уровень эффекта (amplifier) при этом фиксирован (Скорость I остаётся I, не растёт).

- **Потолок:** по решению — **без потолка**. Для безопасности (тип `int` тиков, во избежание переполнения и спец-значения «бесконечно») жёстко клампить итог в `MAX_DURATION_TICKS = 1_000_000` (~13.9 ч) — для геймплея это «без потолка».
- **Влияние затяжки и тира:** прибавляемая длительность = `baseSeconds * 20 * (0.5 + 0.5*charge) * effectMult`. Полная затяжка на обычном кальяне = `baseSeconds`; золотой/незерит дают дольше.
- **Реализация — без миксинов.** Ванила при `addEffect` с тем же уровнем и БОЛЬШЕЙ длительностью продлевает эффект; используем это:

```java
package com.hookahmod.smoking;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public final class StackingEffects {

    public static final int MAX_DURATION_TICKS = 1_000_000;

    private StackingEffects() {}

    public static void addStacked(ServerPlayer player, Holder<MobEffect> effect, int amplifier,
                                  int baseSeconds, float charge, float effectMult) {
        int addTicks = Math.round(baseSeconds * 20.0f * (0.5f + 0.5f * Mth.clamp(charge, 0.0f, 1.0f)) * effectMult);
        MobEffectInstance current = player.getEffect(effect);
        int base = (current != null && current.getAmplifier() == amplifier) ? current.getDuration() : 0;
        int duration = Math.min(base + addTicks, MAX_DURATION_TICKS);
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, true, true));
    }
}
```

- `getEffect` с тем же `effect` и тем же `amplifier` → берём остаток и прибавляем. Если активного нет / другой уровень → `base = 0` (вешаем заново). Если уже висит более сильный эффект из другого источника — ванильный `addEffect` сам не понизит его (это допустимо).

### Обобщённый класс баф-табака

Все обычные табаки одинаковы по поведению — отличаются списком эффектов, надымленностью и цветом дыма. Делаем один класс, конфигурируемый в `ModItems`:

```java
package com.hookahmod.item;

import com.hookahmod.smoking.StackingEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public class BuffTobaccoItem extends AbstractTobaccoItem {

    public record Buff(Holder<MobEffect> effect, int amplifier, int baseSeconds) {}

    private final List<Buff> buffs;
    private final Vector3f smokeColor;

    public BuffTobaccoItem(Properties properties, int intoxication, @Nullable Vector3f smokeColor, Buff... buffs) {
        super(properties, TobaccoCategory.REGULAR, intoxication);
        this.smokeColor = smokeColor;
        this.buffs = List.of(buffs);
    }

    @Override
    public void onExhale(ServerLevel level, ServerPlayer smoker, float charge, float effectMult, float combatMult) {
        for (Buff b : buffs) {
            StackingEffects.addStacked(smoker, b.effect(), b.amplifier(), b.baseSeconds(), charge, effectMult);
        }
    }

    @Override
    @Nullable
    public Vector3f smokeColor() {
        return smokeColor;
    }
}
```

> Эффекты применяются к **курящему** (`smoker`). Базовый `hookah_tobacco` остаётся без эффектов (как сейчас). Накуренность и расход — уже в ядре, трогать не нужно.

### 1.3 Кастомный эффект «Удача моря» (для рыбацкого табака)

Вместо ванильной заглушки `Luck II + Water Breathing` рыбацкий табак даёт **свой уникальный `MobEffect`** с уникальной иконкой. Эффект бенефициарный, тиловый, повышает атрибут удачи (реально влияет на рыбалку/лут): на уровень даёт +1 к `Attributes.LUCK` (ванила умножает базовое значение на `amplifier+1`, т.е. уровень II = +2).

- **Реестр** `effect/ModMobEffects`:
  ```java
  package com.hookahmod.effect;

  import com.hookahmod.HookahMod;
  import net.minecraft.core.registries.Registries;
  import net.minecraft.world.effect.MobEffect;
  import net.neoforged.neoforge.registries.DeferredHolder;
  import net.neoforged.neoforge.registries.DeferredRegister;

  public final class ModMobEffects {
      public static final DeferredRegister<MobEffect> MOB_EFFECTS =
              DeferredRegister.create(Registries.MOB_EFFECT, HookahMod.MOD_ID);

      public static final DeferredHolder<MobEffect, MobEffect> SEA_LUCK =
              MOB_EFFECTS.register("sea_luck", SeaLuckEffect::new);

      private ModMobEffects() {}
  }
  ```
- **Класс** `effect/SeaLuckEffect`:
  ```java
  package com.hookahmod.effect;

  import com.hookahmod.HookahMod;
  import net.minecraft.world.effect.MobEffect;
  import net.minecraft.world.effect.MobEffectCategory;
  import net.minecraft.world.entity.ai.attributes.AttributeModifier;
  import net.minecraft.world.entity.ai.attributes.Attributes;

  public class SeaLuckEffect extends MobEffect {
      public SeaLuckEffect() {
          super(MobEffectCategory.BENEFICIAL, 0x2A8AB0);
          addAttributeModifier(Attributes.LUCK, HookahMod.id("sea_luck"), 1.0, AttributeModifier.Operation.ADD_VALUE);
      }
  }
  ```
- Зарегистрировать `ModMobEffects.MOB_EFFECTS.register(modBus)` в `HookahMod`.
- В `BuffTobaccoItem.Buff` для рыбацкого передавать `ModMobEffects.SEA_LUCK` (это `Holder<MobEffect>`).
- **Иконка эффекта** — ванила сама грузит `assets/hookahmod/textures/mob_effect/sea_luck.png` (по id, клиентского кода не нужно). Промт текстуры — §7.
- **Локализация** `effect.hookahmod.sea_luck` (§6).

---

## 2. Таблица обычных табаков (9 шт., всё tunable)

`id` — в стиле существующих (`tobacco_*`). Базовый ингредиент всех рецептов — `hookahmod:hookah_tobacco` (как у боевых). Все рецепты **shapeless**, результат **count 2** (как `tobacco_poison.json`). «сек/зат.» = `baseSeconds` на полную затяжку (база тира).

| id | Назв. ru / en | Ингредиенты (+ табак) | Эффект(ы) [уровень] | сек/зат. | надым. | Цвет дыма (r,g,b) |
|---|---|---|---|---|---|---|
| `tobacco_mint` | Мятный / Mint | `hookahmod:mint` | Speed I | 30 | 14 | 0.55, 0.95, 0.65 |
| `tobacco_apple` | Яблочный / Apple | `minecraft:apple` | Regeneration I | 20 | 15 | 0.85, 0.20, 0.22 |
| `tobacco_honey` | Медовый / Honey | `minecraft:honey_bottle` | Saturation | 10 | 18 | 0.95, 0.72, 0.20 |
| `tobacco_citrus` | Цитрусовый / Citrus | `croptopia:orange` | Haste I | 30 | 18 | 1.0, 0.55, 0.10 |
| `tobacco_coffee` | Кофейный / Coffee | `croptopia:coffee_beans` | Speed I + Haste I | 30 | 22 | 0.40, 0.26, 0.15 |
| `tobacco_lavender` | Лавандовый / Lavender | `hookahmod:lavender` | Night Vision | 45 | 22 | 0.70, 0.55, 0.90 |
| `tobacco_miner` | Шахтёрский / Miner's | `minecraft:coal` + `minecraft:glow_berries` | Night Vision + Haste I | 45 | 27 | 0.30, 0.30, 0.32 |
| `tobacco_traveler` | Путешественника / Traveler's | `minecraft:sugar` + `minecraft:rabbit_foot` | Speed II | 25 | 27 | 0.80, 0.70, 0.45 |
| `tobacco_fisher` | Рыбацкий / Fisher's | `minecraft:cod` + `minecraft:seagrass` | **Удача моря II** (кастом, §1.3) | 30 | 22 | 0.30, 0.75, 0.80 |

**Маппинг эффектов → `MobEffects` (Holder):** Speed=`MOVEMENT_SPEED`, Haste=`DIG_SPEED`, Regeneration=`REGENERATION`, Saturation=`SATURATION`, Night Vision=`NIGHT_VISION`. «Удача моря» — кастомный `ModMobEffects.SEA_LUCK` (§1.3). Уровень II = `amplifier 1`, I = `amplifier 0`.

**Нюансы:**
- «Удача моря» как `MobEffect` в ваниле отсутствует — поэтому у рыбацкого свой кастомный эффект с уникальной иконкой (§1.3), повышающий атрибут удачи.
- `Saturation`/`Apple Regen` сильные — поэтому маленький `baseSeconds`. Это баланс, легко поменять.
- `tobacco_citrus`/`tobacco_coffee` зависят от Croptopia. В сборке он есть. Если предмет недоступен — рецепт просто не загрузится (без краша). Ванильный фолбэк цитруса: `minecraft:golden_carrot`.

**Зависимость по фазам:** `tobacco_mint` и `tobacco_lavender` используют кастомные предметы `hookahmod:mint`/`hookahmod:lavender` из Фазы 2 — поэтому делаются в Фазе 3. Остальные 7 — в Фазе 1.

---

## 3. ФАЗА 1 — Механика + 7 табаков на готовых ингредиентах

**Делаем:** `StackingEffects` (§1), `BuffTobaccoItem` (§1), кастомный эффект «Удача моря» (`ModMobEffects` + `SeaLuckEffect`, §1.3 — нужен рыбацкому) и 7 табаков, чьи ингредиенты уже существуют: `tobacco_apple`, `tobacco_honey`, `tobacco_citrus`, `tobacco_coffee`, `tobacco_miner`, `tobacco_traveler`, `tobacco_fisher`.

Для каждого:
1. **`ModItems`** — `DeferredItem<BuffTobaccoItem>` по образцу `TOBACCO_POISON`, с конфигом из §2, напр.:
   ```java
   public static final DeferredItem<BuffTobaccoItem> TOBACCO_COFFEE = ITEMS.register(
           "tobacco_coffee",
           () -> new BuffTobaccoItem(new Item.Properties().stacksTo(64), 22,
                   new Vector3f(0.40f, 0.26f, 0.15f),
                   new BuffTobaccoItem.Buff(MobEffects.MOVEMENT_SPEED, 0, 30),
                   new BuffTobaccoItem.Buff(MobEffects.DIG_SPEED, 0, 30))
   );
   ```
2. **`ModCreativeTabs`** — `output.accept(ModItems.TOBACCO_X.get());` рядом с другими табаками.
3. **Рецепт** `data/hookahmod/recipes/tobacco_x.json` (shapeless, `hookahmod:hookah_tobacco` + ингредиент(ы), `result {id, count:2}`).
4. **Модель** `assets/hookahmod/models/item/tobacco_x.json` (generated, `layer0: hookahmod:item/tobacco_x`).
5. **Локализация** — `item.hookahmod.tobacco_x` в `ru_ru.json` и `en_us.json` (§6).
6. **Текстура** `assets/hookahmod/textures/item/tobacco_x.png` — пользователь даст по промтам §7 (класть как есть, без ресайза; двойное `.png.png` → переименовать в `.png`).

**Критерий готовности Ф1:** 7 табаков крафтятся, на выдохе вешают эффекты; повторные затяжки **прибавляют** длительность (проверить: затяжка → Speed ~30 c, ещё затяжка → ~60 c); на золотом/незеритовом кальяне длительность больше; цвет дыма соответствует. `gradlew build` зелёный.

---

## 4. ФАЗА 2 — Кастомные растения: табак, мята, лаванда

Своя культура (как пшеница): растёт на грядке (farmland), 8 возрастов, случайный тик роста; при сборе даёт продукт + семена. Делаем **три** культуры симметрично: **табак**, **мяту**, **лаванду**.

- **Важно про табак:** продукт сбора табачной культуры = **существующий** предмет `hookahmod:hookah_tobacco` (новый предмет-продукт и его текстура НЕ нужны — иконка уже есть). Старый рецепт `hookah_tobacco` (dried_kelp + brown_mushroom) **оставить** как альтернативу. Новые предметы для табака: только семена `tobacco_seed` (+ блок `tobacco_crop`).
- **Мята/лаванда:** для них создаём и новые продукты (`mint`/`lavender`), и семена (`mint_seed`/`lavender_seed`).
- **Семена всех трёх** падают с травы (GLM, §4.4) — чтобы сразу запустить выживальный цикл и протестить.

### 4.1 Блоки
- Класс `block/ModCropBlock extends CropBlock`:
  ```java
  package com.hookahmod.block;

  import net.minecraft.world.level.ItemLike;
  import net.minecraft.world.level.block.CropBlock;
  import java.util.function.Supplier;

  public class ModCropBlock extends CropBlock {
      private final Supplier<? extends ItemLike> seed;
      public ModCropBlock(Properties properties, Supplier<? extends ItemLike> seed) {
          super(properties);
          this.seed = seed;
      }
      @Override
      protected ItemLike getBaseSeedId() {
          return seed.get();
      }
  }
  ```
- **`ModBlocks`** — `TOBACCO_CROP`, `MINT_CROP`, `LAVENDER_CROP`:
  ```java
  public static final DeferredBlock<ModCropBlock> TOBACCO_CROP = BLOCKS.register(
          "tobacco_crop",
          () -> new ModCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), ModItems.TOBACCO_SEED)
  );
  public static final DeferredBlock<ModCropBlock> MINT_CROP = BLOCKS.register(
          "mint_crop",
          () -> new ModCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), ModItems.MINT_SEED)
  );
  ```
  (Лаванда — аналогично. `ofFullCopy(Blocks.WHEAT)` даёт randomTicks/noCollission/instabreak/sound/pushReaction. `ModItems.*_SEED` — `Supplier`.)

### 4.2 Предметы
- **`ModItems`**:
  - Семена `TOBACCO_SEED` / `MINT_SEED` / `LAVENDER_SEED` — `new ItemNameBlockItem(ModBlocks.TOBACCO_CROP.get(), new Item.Properties())` и т.д. (сажаются ПКМ на грядку, имя — «семена»).
  - Продукт `MINT` / `LAVENDER` — `new Item(new Item.Properties().stacksTo(64))` (ингредиент табаков Ф3). **Для табака продукта НЕ создаём** — сбор даёт существующий `HOOKAH_TOBACCO`.
- **`ModCreativeTabs`** — добавить `TOBACCO_SEED`, `MINT`, `MINT_SEED`, `LAVENDER`, `LAVENDER_SEED` (семена в творческой вкладке = гарантированный источник для теста).

### 4.3 Ресурсы (на примере мяты; табак и лаванда — аналогично)
- **Blockstate** `assets/hookahmod/blockstates/mint_crop.json` — 8 возрастов → 4 модели (как морковь/картофель):
  ```json
  {
    "variants": {
      "age=0": { "model": "hookahmod:block/mint_crop_stage0" },
      "age=1": { "model": "hookahmod:block/mint_crop_stage0" },
      "age=2": { "model": "hookahmod:block/mint_crop_stage1" },
      "age=3": { "model": "hookahmod:block/mint_crop_stage1" },
      "age=4": { "model": "hookahmod:block/mint_crop_stage2" },
      "age=5": { "model": "hookahmod:block/mint_crop_stage2" },
      "age=6": { "model": "hookahmod:block/mint_crop_stage2" },
      "age=7": { "model": "hookahmod:block/mint_crop_stage3" }
    }
  }
  ```
- **Модели стадий** `assets/hookahmod/models/block/mint_crop_stage0..3.json` (4 файла), каждая:
  ```json
  {
    "parent": "minecraft:block/crop",
    "render_type": "minecraft:cutout",
    "textures": { "crop": "hookahmod:block/mint_crop_stage0" }
  }
  ```
  (`render_type: cutout` в JSON модели — прозрачность без клиентского кода.)
- **Модели предметов**: `models/item/mint_seed.json` и `models/item/mint.json` — generated (`layer0` = `hookahmod:item/mint_seed` / `hookahmod:item/mint`).
- **Лут блока** `data/hookahmod/loot_tables/blocks/mint_crop.json` — как пшеница: спелый (age=7) → продукт + семена (binomial), иначе → 1 семя. **Для `tobacco_crop` продукт спелого = `hookahmod:hookah_tobacco`** (а не отдельный предмет), семена = `hookahmod:tobacco_seed`:
  ```json
  {
    "type": "minecraft:block",
    "pools": [
      {
        "rolls": 1.0,
        "entries": [{
          "type": "minecraft:alternatives",
          "children": [
            {
              "type": "minecraft:item",
              "name": "hookahmod:mint",
              "conditions": [{
                "condition": "minecraft:block_state_property",
                "block": "hookahmod:mint_crop",
                "properties": { "age": "7" }
              }]
            },
            { "type": "minecraft:item", "name": "hookahmod:mint_seed" }
          ]
        }]
      },
      {
        "rolls": 1.0,
        "conditions": [{
          "condition": "minecraft:block_state_property",
          "block": "hookahmod:mint_crop",
          "properties": { "age": "7" }
        }],
        "entries": [{
          "type": "minecraft:item",
          "name": "hookahmod:mint_seed",
          "functions": [{
            "function": "minecraft:apply_bonus",
            "enchantment": "minecraft:fortune",
            "formula": "minecraft:binomial_with_bonus_count",
            "parameters": { "extra": 3, "probability": 0.5714286 }
          }]
        }]
      }
    ]
  }
  ```

### 4.4 Семена падают с травы (GLM — обязательно)
Семена всех трёх культур (`tobacco_seed`, `mint_seed`, `lavender_seed`) выпадают с малым шансом при ломании травы — это основной источник, чтобы запустить выживальный цикл без `/give` и сразу протестить (как делает Croptopia).
- `loot/AddItemLootModifier extends LootModifier` с `MapCodec` (поля `item`, `chance`); в `doApply` при `random < chance` добавить стак.
- `DeferredRegister<MapCodec<? extends IGlobalLootModifier>>` на `NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS`, зарегистрировать в `HookahMod`.
- `data/hookahmod/loot_modifiers/global_loot_modifiers.json` (`replace:false`, список из 3 записей) + по файлу на каждое семя; условие — `neoforge:loot_table_id` на `minecraft:blocks/short_grass` и `minecraft:blocks/tall_grass`, `chance` ~0.04 для табака и ~0.03 для мяты/лаванды (tunable).
- Семена также лежат в творческой вкладке (§4.2) — дублирующий гарантированный источник.
- Дикий ворлдген не делаем — `CropBlock` живёт только на грядке.

**Критерий готовности Ф2:** табак, мята и лаванда сажаются на грядку, проходят 4 видимые стадии, растут по тику; сбор спелого даёт продукт (у табака — `hookah_tobacco`) + семена, неспелого — семя; модели рендерятся с прозрачностью; **семена иногда падают из травы** (проверить — наломать травы, получить семена, посадить, вырастить, скрафтить табак). `gradlew build` зелёный.

---

## 5. ФАЗА 3 — Мятный и лавандовый табаки + полировка

- Добавить `tobacco_mint` и `tobacco_lavender` ровно по схеме §3 (рецепты используют `hookahmod:mint` / `hookahmod:lavender` из Ф2), конфиг из §2.
- Финал: проверить творческую вкладку (все 9 табаков + мята/лаванда + семена), авто-JEI, паритет `ru_ru.json`/`en_us.json`.

**Критерий готовности Ф3:** все 9 обычных табаков работают; мятный/лавандовый крафтятся из выращенного; стакающаяся длительность и множитель тира работают для всех. `gradlew build` зелёный.

---

## 6. Локализация (ru первично + en зеркало)

Имена предметов:
- `item.hookahmod.tobacco_mint` — «Мятный табак» / «Mint Tobacco»
- `item.hookahmod.tobacco_apple` — «Яблочный табак» / «Apple Tobacco»
- `item.hookahmod.tobacco_honey` — «Медовый табак» / «Honey Tobacco»
- `item.hookahmod.tobacco_citrus` — «Цитрусовый табак» / «Citrus Tobacco»
- `item.hookahmod.tobacco_coffee` — «Кофейный табак» / «Coffee Tobacco»
- `item.hookahmod.tobacco_lavender` — «Лавандовый табак» / «Lavender Tobacco»
- `item.hookahmod.tobacco_miner` — «Шахтёрский табак» / «Miner's Tobacco»
- `item.hookahmod.tobacco_traveler` — «Табак путешественника» / «Traveler's Tobacco»
- `item.hookahmod.tobacco_fisher` — «Рыбацкий табак» / «Fisher's Tobacco»

Растения:
- `item.hookahmod.tobacco_seed` — «Семена табака» / «Tobacco Seeds»
- `block.hookahmod.tobacco_crop` — «Табак» / «Tobacco»
- `item.hookahmod.mint` — «Мята» / «Mint»
- `item.hookahmod.mint_seed` — «Семена мяты» / «Mint Seeds»
- `item.hookahmod.lavender` — «Лаванда» / «Lavender»
- `item.hookahmod.lavender_seed` — «Семена лаванды» / «Lavender Seeds»
- `block.hookahmod.mint_crop` — «Мята» / «Mint» (не виден в UI, но для полноты)
- `block.hookahmod.lavender_crop` — «Лаванда» / «Lavender»

Эффект:
- `effect.hookahmod.sea_luck` — «Удача моря» / «Sea Luck»

(Тултипы расхода у `BuffTobaccoItem` берутся из общего `appendHoverText` — отдельные строки не нужны.)

---

## 7. Текстуры — AI-промты (строгий формат) + куда класть

**Workflow:** пользователь генерит и кладёт файлы в `C:\Users\mashi\Downloads\` в том виде, в каком отдаёт ИИ (двойное расширение `*.png.png` — норм, агент только переименует в `.png`, содержимое не трогает). **Ставить как есть — НЕ ужимать, НЕ ресайзить (ни до 16×16, ни до 32×32), любое исходное разрешение оставлять как есть.** Числа «16x16»/«18x18» в промтах — это указание стиля пиксель-арта, а не итоговый размер файла.

### 7.0 Иконка эффекта «Удача моря» (mob_effect, 1 шт.)
- **Удача моря:** `true 18x18 pixel art, minecraft status effect icon, sea luck symbol, a teal fishing hook crossed with a small golden four-leaf clover over a tiny wave, simple bold centered emblem, transparent background, hard pixel edges, no antialiasing, limited palette: sea teal #2a8ab0, bright cyan #6fd9e0, lucky gold #ffd24a, white highlight #ffffff, dark outline #103040, vanilla minecraft effect icon style, NOT smooth, NOT HD`

### 7.1 Иконки табаков (9, мешочки в стиле существующих боевых)
- **Мятный:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of mint tobacco, small bag with fresh green shredded mint leaves, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: mint green #6fd98a, deep green #2f7d4a, pale highlight #d8ffe2, sack tan #b89a6a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Яблочный:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of apple tobacco, small bag with red apple chunks and shredded leaves, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: apple red #d2331f, dark red #7a1f14, leaf green #4a9a3a, sack tan #b89a6a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Медовый:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of honey tobacco, small bag with golden honey-glazed shredded leaves with a glossy drip, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: honey gold #f0a81e, amber #c47a10, pale highlight #ffe79a, sack tan #b89a6a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Цитрусовый:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of citrus tobacco, small bag with bright orange citrus zest and shredded leaves, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: citrus orange #ff8a1e, deep orange #c45a10, leaf green #4a9a3a, sack tan #b89a6a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Кофейный:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of coffee tobacco, small bag with dark brown coffee beans mixed into shredded leaves, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: coffee brown #6b4423, dark roast #3a2414, cream #d8b88a, sack tan #b89a6a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Лавандовый:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of lavender tobacco, small bag with purple lavender flowers and shredded leaves, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: lavender purple #a98ad6, deep violet #6a4a9a, pale highlight #e6d8ff, sack tan #b89a6a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Шахтёрский:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of miner tobacco, small bag with black coal bits and glowing greenish berry specks in shredded leaves, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: coal black #2a2a2c, grey #5a5a5e, glow green #8aff8a, sack tan #b89a6a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Путешественника:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of traveler tobacco, small worn leather bag with tan shredded leaves and faint sugar sparkle, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: tan #c2a86a, brown leather #8a6a3a, pale highlight #efe2b0, white sparkle #ffffff, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Рыбацкий:** `true 16x16 pixel art, minecraft vanilla item icon, pouch of fisher tobacco, small bag with teal sea-green shredded leaves and tiny dried seaweed bits, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: sea teal #3aa8b0, deep teal #1f6e74, kelp green #4a8a4a, sack tan #b89a6a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`

### 7.2 Продукты растений (2)
- **Мята (продукт):** `true 16x16 pixel art, minecraft vanilla item icon, sprig of fresh mint leaves, a small bunch of bright green serrated mint leaves on a short stem, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: mint green #6fd98a, deep green #2f7d4a, pale highlight #d8ffe2, stem green #3a6a2a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Лаванда (продукт):** `true 16x16 pixel art, minecraft vanilla item icon, bunch of lavender flowers, several tall purple lavender flower spikes tied together with green stems, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: lavender purple #a98ad6, deep violet #6a4a9a, pale highlight #e6d8ff, stem green #4a7a3a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`

### 7.3 Семена (3)
- **Семена табака:** `true 16x16 pixel art, minecraft vanilla item icon, tobacco seeds, a small scattered pile of tiny brown plant seeds, square-ish minecraft silhouette, top-down item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: seed tan #c2a86a, dark brown #5a3a20, dark speck #3a2414, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Семена мяты:** `true 16x16 pixel art, minecraft vanilla item icon, mint seeds, a small scattered pile of tiny green-tan plant seeds, square-ish minecraft silhouette, top-down item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: seed tan #c2a86a, green tint #6f9a5a, dark speck #4a3a20, black outline, vanilla minecraft item style, NOT smooth, NOT HD`
- **Семена лаванды:** `true 16x16 pixel art, minecraft vanilla item icon, lavender seeds, a small scattered pile of tiny purple-tan plant seeds, square-ish minecraft silhouette, top-down item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, transparent background, limited palette: seed tan #c2a86a, purple tint #8a6ab0, dark speck #4a2a4a, black outline, vanilla minecraft item style, NOT smooth, NOT HD`

### 7.4 Стадии роста (12: по 4 на растение) — спрайт для крестовой (#) модели
- **Табак stage0:** `true 16x16 pixel art, minecraft crop texture, tiny tobacco seedling sprout, two small broad green leaves near the bottom, single plant sprite for hash cross block model, transparent background, hard pixel edges, no antialiasing, limited palette: leaf green #5a9a3a, deep green #2f6e1f, soil brown #5a3a20, vanilla minecraft crop style, NOT smooth, NOT HD`
- **Табак stage1:** `true 16x16 pixel art, minecraft crop texture, young tobacco plant, short central stalk with a few broad green leaves, reaching one third height, single plant sprite for hash cross block model, transparent background, hard pixel edges, no antialiasing, limited palette: leaf green #5a9a3a, deep green #2f6e1f, stalk green #4a7a2a, vanilla minecraft crop style, NOT smooth, NOT HD`
- **Табак stage2:** `true 16x16 pixel art, minecraft crop texture, growing tobacco plant, taller stalk with several large broad leaves, reaching two thirds height, single plant sprite for hash cross block model, transparent background, hard pixel edges, no antialiasing, limited palette: leaf green #5a9a3a, deep green #2f6e1f, stalk green #4a7a2a, vanilla minecraft crop style, NOT smooth, NOT HD`
- **Табак stage3:** `true 16x16 pixel art, minecraft crop texture, fully grown tobacco plant, tall stalk with big broad green leaves and small pale pink flower cluster at the top, ready to harvest, filling most of the tile, single plant sprite for hash cross block model, transparent background, hard pixel edges, no antialiasing, limited palette: leaf green #5a9a3a, deep green #2f6e1f, stalk green #4a7a2a, flower pink #e6b8d0, vanilla minecraft crop style, NOT smooth, NOT HD`
- **Мята stage0:** `true 16x16 pixel art, minecraft crop texture, tiny mint seedling sprout, two small green shoots near the bottom, single plant sprite for hash cross block model, transparent background, hard pixel edges, no antialiasing, limited palette: mint green #6fd98a, deep green #2f7d4a, soil brown #5a3a20, vanilla minecraft crop style, NOT smooth, NOT HD`
- **Мята stage1:** `true 16x16 pixel art, minecraft crop texture, young mint plant, short green stems with a few small serrated leaves, reaching one third height, single plant sprite for hash cross block model, transparent background, hard pixel edges, no antialiasing, limited palette: mint green #6fd98a, deep green #2f7d4a, vanilla minecraft crop style, NOT smooth, NOT HD`
- **Мята stage2:** `true 16x16 pixel art, minecraft crop texture, growing mint plant, taller bushy green stems with many serrated mint leaves, reaching two thirds height, single plant sprite for hash cross block model, transparent background, hard pixel edges, no antialiasing, limited palette: mint green #6fd98a, deep green #2f7d4a, pale highlight #d8ffe2, vanilla minecraft crop style, NOT smooth, NOT HD`
- **Мята stage3:** `true 16x16 pixel art, minecraft crop texture, fully grown mint plant, tall lush bush of bright green serrated mint leaves filling most of the tile, ready to harvest, single plant sprite for hash cross block model, transparent background, hard pixel edges, no antialiasing, limited palette: mint green #6fd98a, deep green #2f7d4a, pale highlight #d8ffe2, vanilla minecraft crop style, NOT smooth, NOT HD`
- **Лаванда stage0:** `true 16x16 pixel art, minecraft crop texture, tiny lavender seedling sprout, two small green shoots near the bottom, single plant sprite for hash cross block model, transparent background, hard pixel edges, no antialiasing, limited palette: stem green #4a7a3a, leaf green #6a9a5a, soil brown #5a3a20, vanilla minecraft crop style, NOT smooth, NOT HD`
- **Лаванда stage1:** `true 16x16 pixel art, minecraft crop texture, young lavender plant, short green stems with narrow leaves, reaching one third height, no flowers yet, single plant sprite for hash cross block model, transparent background, hard pixel edges, no antialiasing, limited palette: stem green #4a7a3a, leaf green #6a9a5a, vanilla minecraft crop style, NOT smooth, NOT HD`
- **Лаванда stage2:** `true 16x16 pixel art, minecraft crop texture, growing lavender plant, taller stems with narrow leaves and small purple flower buds forming at the tips, reaching two thirds height, single plant sprite for hash cross block model, transparent background, hard pixel edges, no antialiasing, limited palette: stem green #4a7a3a, leaf green #6a9a5a, lavender purple #a98ad6, vanilla minecraft crop style, NOT smooth, NOT HD`
- **Лаванда stage3:** `true 16x16 pixel art, minecraft crop texture, fully grown lavender plant, tall green stems topped with bright purple lavender flower spikes filling most of the tile, ready to harvest, single plant sprite for hash cross block model, transparent background, hard pixel edges, no antialiasing, limited palette: stem green #4a7a3a, leaf green #6a9a5a, lavender purple #a98ad6, deep violet #6a4a9a, vanilla minecraft crop style, NOT smooth, NOT HD`

### 7.5 Имена файлов → папки (агент сам сопоставит по имени)
Класть в `C:\Users\mashi\Downloads\`. Путь — внутри `src/main/resources/assets/hookahmod/textures/`.

| Что | Имя файла | Папка |
|---|---|---|
| 9 табаков | `tobacco_<flavor>.png` | `item/` |
| Иконка эффекта «Удача моря» | `sea_luck.png` | `mob_effect/` |
| Семена табака | `tobacco_seed.png` | `item/` |
| Семена мяты | `mint_seed.png` | `item/` |
| Семена лаванды | `lavender_seed.png` | `item/` |
| Мята (продукт) | `mint.png` | `item/` |
| Лаванда (продукт) | `lavender.png` | `item/` |
| Стадии табака (4) | `tobacco_crop_stage0..3.png` | `block/` |
| Стадии мяты (4) | `mint_crop_stage0..3.png` | `block/` |
| Стадии лаванды (4) | `lavender_crop_stage0..3.png` | `block/` |

> Продукт табачной культуры — существующий `hookah_tobacco` (его иконка `item/hookah_tobacco.png` уже есть, новый файл не нужен). **Итого новых текстур: 27** (9 табаков + 1 иконка эффекта + 3 семени + 2 продукта мята/лаванда + 12 стадий роста; иконки мятного/лавандового табака входят в «9 табаков»).

---

## 8. Порядок реализации
1. **Фаза 1** — `StackingEffects` + `BuffTobaccoItem` + кастомный эффект «Удача моря» (`ModMobEffects`) + 7 табаков. Тест в `runClient` (особенно стак длительности, тиры, иконка эффекта рыбацкого).
2. **Фаза 2** — три кастомные культуры табак/мята/лаванда (блоки, семена, продукты, лут, модели) + GLM-дроп семян из травы. Тест: наломать траву → семена → вырастить → скрафтить табак.
3. **Фаза 3** — мятный/лавандовый табаки + полировка.

Каждая фаза: код → `gradlew build` (зелёная) → тест в `runClient` → правки по скриншотам. Не лезть в фазу N+1, пока N не подтверждена. **Без комментариев в коде, без коммитов.**
