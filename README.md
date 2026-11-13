# Cobblemon Evolution Cosmetics (v0.2.0, server-side)

Put an **Everstone** in a Pokémon's *cosmetic slot* and it looks like its **earliest** evolution stage; an **Eviolite**
makes it look like its **middle** stage. Visual only: species, stats, moves, ability, held item, UUID, hitbox and
movement never change.

**Install it on the server only.** Players join with their normal Cobblemon 1.8.1 setup and install nothing extra.

## Requirements (server)
Minecraft 1.21.1 · Fabric Loader >= 0.17.2 · Fabric API · Fabric Language Kotlin · Cobblemon 1.8.1 (Fabric) · Java 21.
Clients: exactly what your Cobblemon server already requires (Cobblemon itself). No client jar, no resource pack, no config.

## Install
1. Copy `evolution-cosmetics-0.2.0.jar` into the server's `mods/` folder.
2. Restart the server. The log must show `Cobblemon Evolution Cosmetics loaded`.
3. In game (op): `/evocosmetics status` should report `Ours: [evolution_cosmetics:evolution_items (1025 pokemon filters)]`.

Players then use Cobblemon's normal cosmetic-slot interaction: hold an Everstone/Eviolite, open the Pokémon's
interaction wheel, choose the cosmetic option. Removing the item restores the normal look.

## How it works (no client code)
1. `data/evolution_cosmetics/cosmetic_items/evolution_items.json` lists every Cobblemon species and maps
   Everstone -> aspect `evo_cosmetic-earliest`, Eviolite -> `evo_cosmetic-middle`. Cobblemon loads it like any
   cosmetic-item data and itself syncs the assignments to connected players.
2. Cobblemon's built-in cosmetic-slot aspect provider turns the slot item into that marker aspect on the Pokémon.
3. Every tick, `PokemonServerDelegate#updateTrackedValues` copies the Pokémon's species and aspects into the
   entity's synced data (`SPECIES`, `ASPECTS`). The Cobblemon client reads exactly those two values to choose the
   model and texture. Cobblemon takes them from a private `getMock()` override (the Illusion/Transform mechanism) when
   present. `PokemonServerDelegateMixin` supplies a `getMock()` result carrying only the ancestor's species and aspects.
4. `EvolutionResolver` finds the ancestor by walking Cobblemon's registered pre-evolution data backwards from the
   Pokémon's own species/form (no per-species code). Branching lines (Eevee) always resolve to the real ancestry.
5. Nothing else is overridden and no `MocKEffect` is registered, so the `Pokemon` object, its real species/form, hitbox,
   flight/swimming logic and Pokédex scan are untouched. If a real Illusion/Transform effect is active it wins.

## Selection rules
Chain = [self, parent, grandparent, ...] (capped at 12). With n stages: Everstone = earliest (n >= 2); Eviolite =
stage index (n-1)/2 counted from the earliest (n >= 3); otherwise nothing changes. Pre-evolutions written as
`"meowth galarian"` are resolved through their aspects so regional forms are kept (Perrserker -> Galarian Meowth).

## Debug commands (op level 2)
- `/evocosmetics status` : is our data loaded?
- `/evocosmetics resolve <species>` : ancestry and Everstone/Eviolite targets for every form.
- `/evocosmetics audit` : resolves every species/form; details in the server log.
- `/evocosmetics inspect` : Pokémon within 64 blocks, real species/aspects vs what is advertised to clients.
- `/evocosmetics set <everstone|eviolite|none>` : **test helper**, sets the cosmetic slot of every Pokémon within
  64 blocks through Cobblemon's own `Pokemon#setCosmeticItem`. Use only on test worlds.

## Verification status
Verified on a real dedicated server (Fabric Loader 0.17.2, Minecraft 1.21.1, Fabric API 0.116.6, Cobblemon 1.8.1,
Fabric Language Kotlin 1.13.6, intermediary mappings, Windows, Java 21), with no client attached:
- The mod and its mixins load without errors.
- With an Everstone/Eviolite in the cosmetic slot, the values the server advertises for `SPECIES`/`ASPECTS`
  change to the ancestor (e.g. Charizard -> Charmander / Charmeleon; Azumarill -> Azurill / Marill; Obstagoon ->
  Zigzagoon(galarian) / Linoone(galarian); Mr. Rime -> Mime Jr. / Mr. Mime(galarian); Raichu(Alola) -> Pichu / Pikachu),
  while the real species/form/aspects stay as they were. Shiny and gender aspects are preserved.
- Species with too short a chain (Tauros, Eevee, Pikachu with Eviolite, ...) are left unchanged.
- Removing the item restores the normal values.

Verified in bytecode, NOT exercised with a client by this build:
- The Cobblemon client applies `SPECIES`/`ASPECTS` from entity data (`PokemonClientDelegate#onSyncedDataUpdated`) and
  reloads the model; Cobblemon syncs cosmetic-item assignments to players on join (`CobblemonCosmeticItems#sync`).
- The earlier client-side prototype (v0.1.0) was reported working in game; v0.2.0 reaches the same appearance through
  server data instead, but the in-game look of v0.2.0 itself still needs a check with a real client (see TESTING.md).

## Limitations (by design or untested)
- **World rendering only.** Party screen, PC, summary, Pokédex and battle UI are drawn from the real Pokémon data the
  server sends and keep showing the real species. Changing that would mean sending false Pokémon data to clients.
- Hitbox, scale, movement, flying/swimming and moves stay those of the real species. Poses a descendant has but the
  ancestor's model lacks (e.g. a flying pose) may animate imperfectly.
- Entities only refresh while they tick (a player must be nearby), which is normal for any entity.
- Forms that depend on feature properties rather than aspects in a pre-evolution (e.g. `wolf_form=dusk`) are not
  reproduced; the ancestor appears in its standard look. Where Cobblemon's own data names a plain pre-evolution
  (e.g. Hisuian Typhlosion -> `quilava`), that is what is shown.
- Species added by other mods are not in the generated data file; add their names to
  `data/evolution_cosmetics/cosmetic_items/evolution_items.json` (or ship another cosmetic_items datapack).
- Untested: other mods that override `getMock()` or the entity synced data, trading, very large servers.

## Build
Needs JDK 21 and Gradle 8.12 (Windows: unpack Gradle in a short path, e.g. `C:\gradle-8.12`).
Put the Cobblemon jar in `libs/` as `Cobblemon-fabric-1_8_1_1_21_1.jar`, then run `gradle build`.
The dev client cannot boot a release Cobblemon with Yarn (Kotlin reflection metadata), so test on a real server.

## License
MIT.
