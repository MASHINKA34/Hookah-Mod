# Changelog

All notable changes to Hookahs. Versions target Minecraft 1.21.1 / NeoForge.

## [1.0.5] - 2026-09-09

### Added
- Server config `hookahmod-server.toml` for hose reach, draw timing, consumption, intoxication bands,
  combat smoke and room smoke. It is synced to clients and reloads without a restart.
- Client config `hookahmod-client.toml` to turn off trip visuals: in-world visions, the screamer,
  the full-screen video, the spiral shader and camera sway, one by one or all at once.
- Upgrading a hookah to the next tier keeps the hose, tobacco, coal and liquid inside it.
- Partial tobacco and liquid use is saved per item type and survives placing, pickup,
  crafting upgrades and world reloads. Older saves are migrated automatically.
- A worn hookah with lit coal now lights up the area around the player.
- Block tags: crops keep farmland moist, hookahs are mined with a pickaxe, seeds are in `c:seeds`.
- The guidebook is fully translated into English and Russian.
- Optional Kingdoms hooks for hookah mastery and puff events.

### Changed
- The Pal Palych video is packed into sprite sheets: the jar went from about 17 MB to about 5 MB
  and the video no longer stutters while loading frames.
- Room smoke, the hookah renderer, worn hookah ticking and smoke particle traffic are much cheaper.
- Hose reach checks use the real hose length (5 blocks short, 10 blocks long).
- Regeneration from smoking follows the vanilla healing cadence.
- Abyss trips keep their remaining time after reconnecting.

### Fixed
- A worn hookah is only put down with both hands empty.
- A player could see a phantom puff when using someone else's hookah.
- The hookah coal state did not update when coal was taken out.
- Separate rooms with the same size could merge into one smoke room.
- Fire and ice smoke respect claims, PvP rules and adventure mode; TNT is lit only once.
- Hookah contents, menu sync and sessions after death, logout and dimension change.
- With Kingdoms installed, a failed permission check now denies hookah moving and equipping
  instead of allowing it, and is retried on the next action.

## [1.0.4] - 2026-07-16

### Added
- White Monster drink: brewed from Sweet Water, gives Speed for 30 seconds with soap bubbles and a burp.
  Cans can be placed (up to 3 per block), and an empty can is returned after drinking.
- White Monster works as hookah liquid; the empty can is returned when it runs out.
- Sweet Water, brewed from a water bottle and sugar.
- Optional Create support: filling, emptying and mixing recipes for Sweet Water and White Monster.
- Optional JEI support showing the brewing recipes.
- Abyss Trip effect icon.
- Tobacco, mint and lavender seeds can be composted.

### Changed
- Seeds now drop from grass only through loot modifiers; the duplicate drop from breaking blocks is removed.

## [1.0.3] - 2026-07-09

### Fixed
- Client-only code is no longer loaded on dedicated servers.

## [1.0.2] - 2026-07-09

### Added
- Optional Kingdoms integration: protected areas block moving and equipping hookahs,
  and Kingdoms bonuses can change combat smoke strength and intoxication.

### Fixed
- The worn hookah menu now picks up changes made to the item from outside the menu.

## [1.0.1] - 2026-06-06

### Added
- Hose slot in the hookah menu, slot hints and a session status line.
- Tobacco, hookah charcoal and water bottle as consumables, shown on the hookah: glowing coal,
  water in the flask and tobacco in the bowl.
- Hold right-click with the mouthpiece to draw; a longer draw gives a bigger exhale.
  The mouthpiece is animated.
- Tobacco and coal last 20 puffs, water lasts 200 puffs. Hold Shift on an item to see this.
- Wearable hookah in the chest slot with its own menu and key binding; other players can connect to it.
- Hookah parts: bowl, shaft and flask, crafted into a hookah.
- Exhaled smoke that lingers and drifts.
- Bubbling sound that nearby players can hear.
- Hookah tiers: leather, gold, iron, diamond and netherite, giving armor when worn.
- Intoxication meter with five bands from sober to overdose, and the Tonometer to read it.
- Combat tobaccos: poison, fire, ice and healing smoke cones.
- Abyss Tobacco with trip visions.
- Colored smoke for each tobacco.
- Buff tobaccos with stacking effect duration: apple, honey, citrus, coffee, miner's, traveler's,
  fisher's, mint and lavender. Fisher's tobacco gives the new Sea Luck effect.
- Tobacco, mint and lavender crops; their seeds drop from grass.
- Hashish Tobacco with a spiral screen effect and music.
- Pal Palych tobacco with a full-screen video trip.
- Chickens sometimes drop Chicken Poop.
- Hookah Guide book.
- Crafting recipes for every item, Croptopia variants for coffee and citrus tobacco,
  and charcoal smelting and blasting recipes.

## [1.0.0] - 2026-05-13

### Added
- Hookah block with a short (5 blocks) and long (10 blocks) hose and a mouthpiece.
- Shared smoking session: one player at a time per hose, synced to everyone nearby.
