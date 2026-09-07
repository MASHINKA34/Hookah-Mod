# Hookah Mod

Functional hookahs for Minecraft 1.21.1 / NeoForge: a placeable hookah block with hoses, a mouthpiece,
shared smoking sessions, tobacco blends with combat and buff effects, an intoxication system and a
built-in guidebook.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.233+ (`[21.1.0,21.2)`) |
| Java | 21 |
| GeckoLib | 4.7+ (required) |
| JEI | 19.25+ (optional, client) |
| Create | 6.0.0+ (optional) |
| Croptopia | optional — swaps coffee/citrus recipes to its own crops |

## Gameplay

- **Hookah block** in six tiers (normal, leather, gold, iron, diamond, netherite). The tier is stored in
  the block state and survives placing, breaking and being carried.
- **Worn hookah** — shift-click a hookah to move it into the chest slot; it grants armor by tier and
  other players can connect to it. Shift-right-click a block with **both hands empty** to put it back down.
- **Sessions** — one hose serves one smoker. Claim the mouthpiece from the menu, then hold right-click
  with the mouthpiece in hand to draw and exhale.
- **Tobacco** — buff blends, combat blends (poison / fire / ice / heal cones) and special blends with
  their own visuals.
- **Intoxication** — five bands from sober to overdose, read with the tonometer, decaying over time.
- **Smoke** — exhaled smoke lingers, drifts, and fills enclosed rooms.

## Configuration

Balance lives in `hookahmod-server.toml`, a per-world **server** config
(`saves/<world>/serverconfig/` in single player, `world/serverconfig/` on a dedicated server).
NeoForge syncs it to connecting clients, so hose reach and intoxication bands stay identical
on both sides. Changing the file and reloading applies without a restart.

| Section | What it controls |
|---|---|
| `[smoking]` | Short and long hose reach, exhale cooldown, minimum draw length, how many puffs a tobacco charge and a bottle last |
| `[intoxication]` | Meter ceiling, decay per second, plain-tobacco gain, and the four band thresholds |
| `[combat]` | Master switch for combat cones, whether fire may ignite and ice may freeze blocks, cone reach and half-angle |
| `[smoke]` | Room smoke on/off, particle range, largest room mapped, concurrent smoke-filled rooms, linger time, failed-probe cooldown |
| `[misc]` | Worn-hookah light block, chicken poop chance, luxury preview visibility |

Servers that want no PvP impact can set `combat.enabled = false`; servers on tight CPU budgets can
lower `smoke.maxRoomAirBlocks` or turn `smoke.roomSmokeEnabled` off entirely.

## Building

```
./gradlew build
```

The jar lands in `build/libs/`.

| Task | What it does |
|---|---|
| `./gradlew build` | Compiles and packages the mod |
| `./gradlew test` | 8 JUnit regression tests on an ephemeral server |
| `./gradlew runGameTestServer` | 33 in-world GameTests |
| `./gradlew runClient` | Dev client |
| `./gradlew runServer` | Dev dedicated server |
| `./gradlew runClient2` | Second client (`run2/`) for multiplayer testing |

`libs/flywheel-neoforge-1.21.1-1.0.6.jar` is a dev-runtime-only dependency for the optional Create
integration; it is not part of the published jar.

## Layout

```
src/main/java/com/hookahmod/
  block/       hookah block, block entity, light block, crops
  config/      server config spec and its baked values
  item/        hookah tiers, hoses, mouthpiece, tobaccos, worn-hookah helpers
  menu/        container menu and filtered slots
  network/     payloads (NeoForge play network)
  registry/    DeferredRegister holders
  smoking/     intoxication state, progress, attachments
  smoke/       lingering and room smoke simulation
  combat/      cone targeting and protection-aware block edits
  client/      renderers, screens, particles, trip effects
  integration/ JEI, Create fluids, optional Kingdoms hooks
src/main/resources/  assets, data packs, lang (en_us, ru_ru)
src/test/            JUnit regression tests
src/gameTest/        in-world GameTests
```

## Localization

`en_us` and `ru_ru` are kept at full key parity. All player-facing text, including the guidebook,
goes through translation keys.

## License

All Rights Reserved.
