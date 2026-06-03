# ТЗ: Табак «Пал Палыч» + трип-эффект = полноэкранное ВИДЕО

> **Как пользоваться:** В новом чате (Claude Code в этом репозитории) скажи: «Прочитай `TOBACCO_PALPALYCH_TZ.md` и реализуй». Делать единым проходом: конвертация видео (§3) → код → `gradlew build` (без демона, зелёная) → тест в `runClient` → правки. Числа — настраиваемые дефолты.

---

## 0. Контекст и правила (обязательно)

- **Стек:** NeoForge 21.1.x, MC 1.21.1, Java 21, ModDevGradle, Gradle 8.10.2. Mod ID `hookahmod`, пакет `com.hookahmod`. Сборка `gradlew build` (без демона), тест IntelliJ `runClient`.
- **Правила (СТРОГО):** Без комментариев в коде. Без git-коммитов. Код/идентификаторы — английский. UI-строки — русский первичный (`ru_ru.json`) + английское зеркало (`en_us.json`). Предметы — крафтом/в творческой вкладке/JEI, **не `/give`**. NeoForge best practices: `DeferredRegister`, payload+`StreamCodec`, чёткое деление клиент/сервер.

---

## 1. Суть — это АБСОЛЮТНО НОВЫЙ независимый эффект

⚠️ **НИКАКОГО `hashish_spiral`, никакой спирали, никакого качания камеры/FOV, никаких искажений.** Гашишную трип-механику (`HashishTripManager`/`HashishTripPayload`/шейдер) **НЕ переиспользуем и НЕ зовём.**

- Эффект работает так: **пока на игроке висит эффект — во весь экран проигрывается видео `palpalych_trip.mp4` (+ его звук). Само это видео и есть «трип».**
- **Длительность эффекта = длительность видео.** Видео доиграло → эффект снимается. Эффект сорвался раньше (молоко, смерть, выход из мира, новая «передозная» логика и т.п.) → **видео и звук мгновенно обрываются.**
- **Триггер:** при ЛЮБОЙ затяжке шанс **30%**. Пока эффект висит — повторно не триггерится.
- **Что переиспользуем — только ПАТТЕРНЫ** (не гашишный код): стратегия `AbstractTobaccoItem.onExhale(...)`, регистрация предметов/эффектов/звуков, паттерн тикающего звука (как в `client/trip/HashishTripSound`), регистрация payload в `NetworkHandler.register`, подписка клиентских событий в `ClientSetup.onClientSetup`/`onClientTick`.

### ⚠️ Главное ограничение: Minecraft НЕ умеет проигрывать mp4 нативно

Готового «видео-плеера» в MC нет. Рабочий и надёжный путь (без рантайм-зависимостей): **один раз сконвертировать mp4 → последовательность PNG-кадров + `.ogg`-звук (ffmpeg)**, а в моде проигрывать кадры как полноэкранный GUI-оверлей по таймеру эффекта, звук — обычным стримовым `SoundEvent`. Именно так и делаем (§3–§4). Альтернатива (рантайм-декод mp4 через JCodec, шипится 1 файл) — в §4.9, в базовом ТЗ не нужна.

---

## 2. Параметры (всё tunable)

| Параметр | Значение |
|---|---|
| id предмета | `tobacco_palpalych` |
| Имя ru / en | **«Пал Палыч»** / «Pal Palych» |
| Категория / расход | `REGULAR`, надымленность 20, расход 1 шт./20 затяжек (ядро, не трогаем) |
| Эффект (id) | `palpalych_trip` (кастомный, §4.1), своя иконка в HUD |
| Имя эффекта ru / en | «Трип Пал Палыча» / «Pal Palych Trip» |
| Шанс при затяжке | `0.30f` |
| Длительность эффекта | **= длине видео 19.9 c** → `DURATION_TICKS = 398` |
| Цвет дыма (r,g,b) | `0.85, 0.55, 1.0` (tunable) |
| Ингредиенты рецепта (+ табак) | `minecraft:amethyst_shard` + `minecraft:glow_ink_sac`, shapeless, `result count 2` |

---

## 3. Видео → кадры + звук (ffmpeg, один раз)

Нужен **ffmpeg** на машине разработки (в рантайме мода НЕ нужен). Если нет: `winget install Gyan.FFmpeg` (или скачать сборку). Исходник пользователь кладёт в `C:\Users\mashi\Downloads\palpalych_trip.mp4` (см. §6).

### 3.1 Длительность — УЖЕ ЗАМЕРЕНО
Видео `palpalych_trip.mp4`: **19.9 c, источник 360×360, 30 fps**. → **`DURATION_TICKS = 398`** (= round(19.9 × 20)); уже вписано в §2 и §4.5. Перепроверка при желании: `ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 palpalych_trip.mp4`.

### 3.2 Извлечь кадры (PNG)
Источник квадратный 360×360 — апскейл НЕ нужен, `scale` не указываем:
```
ffmpeg -i "C:\Users\mashi\Downloads\palpalych_trip.mp4" -vf "fps=15" ^
  "src\main\resources\assets\hookahmod\textures\gui\palpalych_trip\frame_%04d.png"
```
- `fps=15` → при 19.9 c выйдет **~298–299 кадров** (≈12 МБ в jar; tunable: 12 fps легче, 30 fps плавнее).
- **Посчитать фактическое число файлов** → записать в `VideoTripManager.FRAME_COUNT`; константы `FPS=15.0f`, `FRAME_W=360`, `FRAME_H=360` (§4.3).

### 3.3 Извлечь звук (OGG — единственный формат звука в MC)
```
ffmpeg -i "C:\Users\mashi\Downloads\palpalych_trip.mp4" -vn -ac 2 -ar 44100 ^
  "src\main\resources\assets\hookahmod\sounds\palpalych_trip.ogg"
```
(Если у пользователя отдельная звуковая дорожка — конвертировать её в `palpalych_trip.ogg`; mp3 MC не играет.)

---

## 4. Реализация (код, без комментариев)

### 4.1 Кастомный эффект `palpalych_trip`
Маркерный `NEUTRAL`-эффект: держит иконку в HUD и служит «жизнью» трипа (есть эффект → крутится видео; пропал → видео стоп). Атрибутов не даёт.

`effect/PalPalychTripEffect`:
```java
package com.hookahmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class PalPalychTripEffect extends MobEffect {
    public PalPalychTripEffect() {
        super(MobEffectCategory.NEUTRAL, 0xC85CFF);
    }
}
```
В `effect/ModMobEffects`:
```java
public static final DeferredHolder<MobEffect, MobEffect> PALPALYCH_TRIP =
        MOB_EFFECTS.register("palpalych_trip", PalPalychTripEffect::new);
```
(Иконку ванила грузит сама из `textures/mob_effect/palpalych_trip.png` — §5.2.)

### 4.2 Payload `VideoTripPayload` (S→C)
`network/VideoTripPayload`:
```java
package com.hookahmod.network;

import com.hookahmod.HookahMod;
import com.hookahmod.client.trip.VideoTripManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VideoTripPayload(int durationTicks) implements CustomPacketPayload {

    public static final Type<VideoTripPayload> TYPE = new Type<>(HookahMod.id("palpalych_trip"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VideoTripPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VideoTripPayload::durationTicks, VideoTripPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(VideoTripPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(VideoTripManager::start);
    }
}
```
Зарегистрировать в `NetworkHandler.register` рядом с остальными `playToClient(...)`:
```java
registrar.playToClient(VideoTripPayload.TYPE, VideoTripPayload.STREAM_CODEC, VideoTripPayload::handle);
```

### 4.3 Клиентский видео-менеджер `VideoTripManager`
Проигрывает кадры полноэкранным оверлеем; сам останавливается, если эффект пропал или кадры кончились. **Выставить константы из §3.**
```java
package com.hookahmod.client.trip;

import com.hookahmod.HookahMod;
import com.hookahmod.effect.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class VideoTripManager {

    private static final int FRAME_COUNT = 299;
    private static final float FPS = 15.0f;
    private static final int FRAME_W = 360;
    private static final int FRAME_H = 360;

    private static boolean active;
    private static int ageTicks;
    private static int lastFrame = -1;

    private VideoTripManager() {}

    public static void start() {
        active = true;
        ageTicks = 0;
        lastFrame = -1;
    }

    public static boolean isActive() { return active; }

    public static void tick() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        boolean alive = mc.player != null && mc.level != null
                && mc.player.getEffect(ModMobEffects.PALPALYCH_TRIP) != null
                && currentFrame() < FRAME_COUNT;
        if (!alive) { stop(); return; }
        ageTicks++;
        PalPalychSoundController.tick(mc, mc.player);
    }

    public static void render(RenderGuiEvent.Post event) {
        if (!active) return;
        int idx = currentFrame();
        if (idx < 0 || idx >= FRAME_COUNT) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        g.blit(frameId(idx), 0, 0, sw, sh, 0.0F, 0.0F, FRAME_W, FRAME_H, FRAME_W, FRAME_H);
        if (lastFrame >= 0 && lastFrame != idx) {
            mc.getTextureManager().release(frameId(lastFrame));
        }
        lastFrame = idx;
    }

    public static void stop() {
        if (!active) return;
        active = false;
        lastFrame = -1;
        PalPalychSoundController.stop(Minecraft.getInstance());
    }

    private static int currentFrame() {
        return (int) Math.floor(ageTicks / 20.0f * FPS);
    }

    private static ResourceLocation frameId(int idx) {
        return HookahMod.id(String.format("textures/gui/palpalych_trip/frame_%04d.png", idx + 1));
    }
}
```
> Уточнить под 1.21.1 точную перегрузку `GuiGraphics.blit(ResourceLocation, x, y, destW, destH, uOffset, vOffset, srcW, srcH, texW, texH)` (та, что масштабирует source→dest). `getGuiScaledWidth/Height` — полноэкранный размер GUI. `FRAME_COUNT=299` — поставить фактическое число извлечённых кадров (§3.2). Видео квадратное 360×360 → блит растягивает его на весь экран (как просили «во весь экран»; возможна лёгкая деформация — если не нужна, центрировать с сохранением пропорций, пилларбокс).

Провязка в `ClientSetup.onClientSetup` (рядом с существующими `addListener`):
```java
NeoForge.EVENT_BUS.addListener(VideoTripManager::render);
```
И в `ClientSetup.onClientTick` добавить строку:
```java
VideoTripManager.tick();
```

### 4.4 Звук трипа (стрим OGG)
В `registry/ModSounds`:
```java
public static final DeferredHolder<SoundEvent, SoundEvent> PALPALYCH_TRIP =
        SOUNDS.register("palpalych_trip", () -> SoundEvent.createVariableRangeEvent(HookahMod.id("palpalych_trip")));
```
`assets/hookahmod/sounds.json` — добавить запись:
```json
"palpalych_trip": {
  "subtitle": "subtitles.hookahmod.palpalych_trip",
  "sounds": [ { "name": "hookahmod:palpalych_trip", "stream": true } ]
}
```
Класс `client/trip/PalPalychSound` + `PalPalychSoundController` — **точная копия** `HashishTripSound`/`HashishTripSoundController`, но: звук `ModSounds.PALPALYCH_TRIP`, и условие жизни — `VideoTripManager.isActive()`. (Громкость/затухание `Attenuation.NONE` как у образца.)

### 4.5 Предмет `PalPalychTobaccoItem`
```java
package com.hookahmod.item;

import com.hookahmod.effect.ModMobEffects;
import com.hookahmod.network.VideoTripPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

public class PalPalychTobaccoItem extends AbstractTobaccoItem {

    private static final float TRIP_CHANCE = 0.30f;
    private static final int DURATION_TICKS = 398;
    private static final Vector3f SMOKE_COLOR = new Vector3f(0.85f, 0.55f, 1.0f);

    public PalPalychTobaccoItem(Properties properties) {
        super(properties, TobaccoCategory.REGULAR, 20);
    }

    @Override
    public void onExhale(ServerLevel level, ServerPlayer smoker, float charge, float effectMult, float combatMult) {
        if (smoker.getEffect(ModMobEffects.PALPALYCH_TRIP) != null) return;
        if (smoker.getRandom().nextFloat() >= TRIP_CHANCE) return;

        smoker.addEffect(new MobEffectInstance(ModMobEffects.PALPALYCH_TRIP, DURATION_TICKS, 0, false, true, true), smoker);
        PacketDistributor.sendToPlayer(smoker, new VideoTripPayload(DURATION_TICKS));
    }

    @Override
    public Vector3f smokeColor() {
        return SMOKE_COLOR;
    }
}
```
> `DURATION_TICKS = 398` (видео 19.9 c). Должно совпадать с длиной кадров (`FRAME_COUNT/FPS*20 ≈ 398`), чтобы эффект и видео кончались вместе.

### 4.6 Регистрация предмета
`registry/ModItems`:
```java
public static final DeferredItem<PalPalychTobaccoItem> TOBACCO_PALPALYCH = ITEMS.register(
        "tobacco_palpalych",
        () -> new PalPalychTobaccoItem(new Item.Properties().stacksTo(64))
);
```
`registry/ModCreativeTabs` (рядом с другими табаками): `output.accept(ModItems.TOBACCO_PALPALYCH.get());`

### 4.7 Рецепт + модель
`data/hookahmod/recipe/tobacco_palpalych.json`:
```json
{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": [
    { "item": "hookahmod:hookah_tobacco" },
    { "item": "minecraft:amethyst_shard" },
    { "item": "minecraft:glow_ink_sac" }
  ],
  "result": { "id": "hookahmod:tobacco_palpalych", "count": 2 }
}
```
`assets/hookahmod/models/item/tobacco_palpalych.json`:
```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "hookahmod:item/tobacco_palpalych" } }
```

### 4.8 Локализация
`ru_ru.json`:
```json
"item.hookahmod.tobacco_palpalych": "Пал Палыч",
"effect.hookahmod.palpalych_trip": "Трип Пал Палыча",
"subtitles.hookahmod.palpalych_trip": "Пал Палыч"
```
`en_us.json`:
```json
"item.hookahmod.tobacco_palpalych": "Pal Palych",
"effect.hookahmod.palpalych_trip": "Pal Palych Trip",
"subtitles.hookahmod.palpalych_trip": "Pal Palych"
```

### 4.9 (Опционально) Рантайм-декод mp4 вместо кадров
Если не хочется сотни PNG в ресурсах: подключить `org.jcodec:jcodec` + `jcodec-javase` через NeoForge **JarJar (JiJ)**, шипить один `palpalych_trip.mp4` в ресурсах, в `VideoTripManager` на фоновом потоке декодить кадры (`FrameGrab.getNativeFrame` → `AWTUtil` → `NativeImage` → `DynamicTexture`) и блитить в оверлее. Сложнее (бандл либы, потоки, совместимость кодека) — в базовом ТЗ не нужно.

---

## 5. Текстуры — AI-промты (2 шт)

> Тема визуала — призматическо-радужная (как выбрано ранее). Стиль/формат — как у прошлых табаков. Ставить как есть, не ресайзить.

### 5.1 Иконка предмета `tobacco_palpalych.png`
`true 16x16 pixel art, minecraft vanilla item icon, pouch of weird prismatic tobacco, small cloth bag overflowing with iridescent rainbow oil-slick shredded leaves that shimmer cyan-magenta-violet-gold, a tiny glowing amethyst crystal shard poking out of the top, square-ish minecraft silhouette, 3/4 isometric item viewpoint, item fills ~12x12 of the 16x16, strict 1 pixel black outline #0d0d0d, hard pixel edges, no antialiasing, no blur, transparent background, limited palette: prism cyan #4fe8ff, prism magenta #ff5cc8, prism violet #a85cff, prism gold #ffd24a, pale highlight #ffffff, sack tan #b89a6a, black outline #0d0d0d, vanilla minecraft item style, NOT smooth, NOT HD, NOT concept art`

### 5.2 Иконка эффекта `palpalych_trip.png`
`true 18x18 pixel art, minecraft status effect icon, swirling psychedelic rainbow symbol, a glass triangular prism splitting a white light beam into a fanning rainbow swirl, simple bold centered symmetrical emblem, transparent background, hard pixel edges, no antialiasing, no blur, limited palette: prism cyan #4fe8ff, prism magenta #ff5cc8, prism violet #a85cff, prism gold #ffd24a, rainbow red #ff5a5a, rainbow green #5aff8a, white beam #ffffff, dark outline #1a1030, vanilla minecraft effect icon style, NOT smooth, NOT HD`

---

## 6. Имена файлов — что и как назвать в `C:\Users\mashi\Downloads\`

| Назначение | Имя файла в Downloads | Куда кладётся в проекте |
|---|---|---|
| **Иконка предмета** «Пал Палыч» | **`tobacco_palpalych.png`** | `src/main/resources/assets/hookahmod/textures/item/` |
| **Иконка эффекта** | **`palpalych_trip.png`** | `src/main/resources/assets/hookahmod/textures/mob_effect/` |
| **Видео трипа** | **`palpalych_trip.mp4`** (переименовать `videoplayback (online-video-cutter.com).mp4`) | через ffmpeg (§3) → кадры в `textures/gui/palpalych_trip/` + `sounds/palpalych_trip.ogg` |

- PNG-иконки AI отдаёт как `*.png.png` — норм, агент только переименует в `.png` (содержимое не трогать, не ресайзить).
- Имена строго такие: иконка эффекта **обязана** называться `palpalych_trip.png` (ванила грузит её по id эффекта); иконка предмета — `tobacco_palpalych.png` (совпадает с `layer0`).
- Видео обязательно переименовать без пробелов/скобок → `palpalych_trip.mp4`.

---

## 7. Критерий готовности

- `tobacco_palpalych` («Пал Палыч») крафтится (табак + аметист + светящийся чернильный мешок → 2 шт), лежит в творческой вкладке.
- При курении **~30% затяжек** вешают эффект «Трип Пал Палыча»; пока он висит — **во весь экран играет видео** `palpalych_trip.mp4` со звуком.
- **Длительность эффекта = длине видео**; доиграло → эффект снят. Снять эффект досрочно (молоко/смерть/выход) → **видео и звук сразу обрываются**.
- Пока эффект висит — повторный трип не триггерится. В HUD видна новая иконка эффекта; иконка предмета — на месте; цвет дыма фиолетовый.
- НИКАКОЙ спирали/искажений/качания камеры. `gradlew build` (без демона) — **зелёный**; гашишный табак и остальное не сломаны.
