# Cobblemon Evolution Cosmetics (v0.1.0, first prototype)

Visual-only mod for **Minecraft 1.21.1 / Fabric / Cobblemon 1.8.1**.
Put an **Everstone** in a Pokémon's **cosmetic slot** (`Pokemon#cosmeticItem`, *not* the held-item slot) and it
looks like its earliest evolution stage; an **Eviolite** makes it look like its middle stage.
The Pokémon stays the same species with the same stats, moves, ability, held item, UUID, etc.

> **Status:** compiled against the real Cobblemon 1.8.1 jar (JDK 21, Gradle 8.12, Loom 1.10.5) and
> tested in game: Everstone/Eviolite in the cosmetic slot change the Pokémon's appearance.

## How it works
1. `data/evolution_cosmetics/cosmetic_items/evolution_items.json` registers Everstone/Eviolite with Cobblemon's own
   cosmetic-item system for every Cobblemon species (plain species names, the same format that was confirmed working in-game
   as a standalone datapack). Species added by other mods would need their own entries.
   Cobblemon's built-in cosmetic-slot aspect provider turns the slotted item into the aspects
   `evo_cosmetic-earliest` / `evo_cosmetic-middle`. Cobblemon saves the slot with the Pokémon and syncs aspects to clients.
2. `EvolutionResolver` (pure logic) walks the registered `preEvolution` links backward from the Pokémon's own species+form.
3. One client mixin on `VaryingModelRepository` (`getPoser/getTexture/getTextureNoSubstitute/getLayers/getSprite`)
   re-runs the lookup for the ancestor with the ancestor's aspect set, then restores the render state.
   World renderer, party/PC/summary GUI, battle overlay and item renderer all use these lookups.
No Pokémon data is changed and nothing extra is saved.

## Selection rules
Chain = [self, parent, grandparent, ...] (cut at 12 or on cycles). n = chain length.
* **Everstone**: earliest ancestor, needs n >= 2.
* **Eviolite**: stage index (n-1)/2 counting from the earliest, needs n >= 3 (strictly between earliest and self).
  3 stages -> the middle one. 2-stage lines: Eviolite does nothing (no distinct middle stage).
* No valid target -> normal appearance, no crash. Branching lines resolve to the Pokémon's real ancestry because we only walk backward.
* A form without its own pre-evolution falls back to the species' pre-evolution.

## Build
1. Copy `Cobblemon-fabric-1_8_1_1_21_1.jar` into `libs/` (or edit `cobblemon_jar` in `gradle.properties`).
2. JDK 21 + Gradle 8.12+: `gradle wrapper --gradle-version 8.12` (once), then `./gradlew build`.
3. Jar: `build/libs/evolution-cosmetics-0.1.0.jar`.
Dev client: `./gradlew runClient` — dev server: `./gradlew runServer`.

## Install
Client **and** server both need the mod jar plus Cobblemon 1.8.1, Fabric API and Fabric Loader >= 0.17.2.
(The server needs it for the cosmetic-item data; the client needs it for the rendering mixin.)
No separate resource pack is needed: all models/textures/animations are Cobblemon's own.

## If Everstone/Eviolite are not accepted by the cosmetic slot
Run `/evocosmetics status` first: it says whether Cobblemon loaded our assignment. Data inside a mod jar never appears in the
Data Packs screen, so its absence there is normal. As an independent test, drop `evolution-cosmetics-datapack.zip`
(same structure as other Cobblemon cosmetic datapacks: `pack.mcmeta` + `data/cobblemon/cosmetic_items/`) into the world's
`datapacks/` folder; it lists every Cobblemon species by plain name, so it shows up in `/datapack list` and works without the wildcard filter.
The cosmetic option only appears when you hold the item, right-click YOUR OWN Pokémon (sent out) and choose the cosmetic option in the interaction wheel.

## Debug commands (op level 2, read-only)
`/evocosmetics status` — shows how many cosmetic assignments Cobblemon loaded and whether ours is among them.
`/evocosmetics resolve charizard` — prints each form's ancestry and the Everstone/Eviolite targets.
`/evocosmetics audit` — resolves every species/form; chain-length summary in chat, details in the log.

## Known / suspected limitations (verify in game, see TESTING.md)
* Poser mismatch: a descendant's pose set (e.g. flying) may not exist on the ancestor's poser.
* Hitbox, scale and movement stay the original species' (rendering only).
* Aspects that exist only on the original species are dropped; the ancestor's own form aspects are added.
* Everstone/Eviolite are accepted by every listed Cobblemon species (even those with no earlier stage; they just do nothing).
* While worn the item leaves the player's inventory (Cobblemon's normal cosmetic-slot behaviour) and returns when removed.
* Not tested: trading, mod-added species, other mods that also hook VaryingModelRepository.


## Verification status

Verified by build/inspection:
- `gradle build` succeeds against `Cobblemon-fabric-1.8.1+1.21.1.jar`.
- The mixin's shadow/inject descriptors match the real `VaryingModelRepository` methods
  (`getPoser/getTexture/getTextureNoSubstitute/getLayers/getSprite`).
- All 1025 species in the jar are listed in the data file; 70 forms define their own pre-evolution
  (e.g. Alolan Raichu -> Pikachu, Perrserker -> Galarian Meowth).

Verified in game (reported by the author's testing):
- The appearance changes in the world and the other views, including poses/animations of descendants,
  regional/alternate forms, trading and species added by other mods.

Limitations (by design):
- Visual only: hitbox, scale, movement, stats, moves, abilities and held item stay those of the real species.
- Other mods that also hook `VaryingModelRepository` may interact; not exhaustively tested.

Build note (Windows): Gradle fails with "agent library failed Agent_OnLoad" if unpacked in a very long path;
use a short path such as `C:\gradle-8.12`.
