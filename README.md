# Cobblemon Evolution Cosmetics (v0.3.1)

Put an **Everstone** in a Pokémon's *cosmetic slot* and it looks like its **earliest** evolution stage; an **Eviolite**
makes it look like its **middle** stage. **Only the skin changes** (model, texture, render layers). The Pokémon stays
the same species everywhere: name, stats, moves, ability, held item, cry, hitbox, movement, Pokédex data.

Server mod + a resource pack. Two ways to use it:
- **Dedicated server (players install nothing):** the server pushes the resource pack; players only accept the prompt.
- **Single player / LAN / a player who installs the mod:** the jar already contains the pack (built for Cobblemon 1.8.1),
  so putting this one file in `mods/` is enough. No resource pack to enable.

## Requirements
- Server: Minecraft 1.21.1 · Fabric Loader >= 0.17.2 · Fabric API · Fabric Language Kotlin · Cobblemon 1.8.1 (Fabric) · Java 21.
- Players: their normal Cobblemon 1.8.1 client. Nothing else.

## Install (server owner)
1. Put `evolution-cosmetics-0.3.1.jar` in the server's `mods/` folder and start the server.
2. On startup the mod writes `evolution_cosmetics_pack.zip` in the server folder and logs its SHA-1:
   `Client resource pack for N species written to ... (sha1 ...)`. (Also available with `/evocosmetics generatepack`.)
3. Upload that zip anywhere that gives a **direct download link** (for example a GitHub release asset).
4. In `server.properties` set:
   ```
   resource-pack=<direct link to the zip>
   resource-pack-sha1=<the sha1 from the log>
   require-resource-pack=true
   ```
   `require-resource-pack=false` also works: players who decline simply see the normal skins.
5. Restart. When Cobblemon, an add-on mod or this mod is updated, restart once more and update the link and SHA-1
   (the pack is regenerated at every start from the Cobblemon data actually installed).

A ready-made pack for plain Cobblemon 1.8.1 is published next to the jar (`evolution-cosmetics-pack-cobblemon-1.8.1.zip`).

Single player / LAN: nothing extra. The jar's own assets contain the pack (Fabric loads them like any mod's assets).
If a server also pushes a pack, its identically named files replace the bundled ones.

## How it works
1. `data/evolution_cosmetics/cosmetic_items/evolution_items.json` lets Cobblemon's normal cosmetic-slot interaction accept
   Everstone/Eviolite and attach the aspect `evo_cosmetic-earliest` / `evo_cosmetic-middle` to the Pokémon. Cobblemon
   syncs these assignments and aspects to players. The Pokémon itself is not modified.
2. Cobblemon picks a species' poser, model, texture and layers from "variations" whose `aspects` are all present on the
   Pokémon (the last matching variation wins per field; layers merge by name). The generated resource pack adds, for
   every species and form, variations that require the marker aspect and carry the **ancestor's** poser, model, texture
   and layers (copied from the ancestor's own Cobblemon resolver data, including shiny and other aspect variants).
3. The ancestor is found by walking Cobblemon's registered pre-evolution data backwards from the Pokémon's own
   species/form (`EvolutionResolver`, no per-species code; regional forms such as Galarian Meowth are kept).
4. Nothing hooks the renderer or changes entity data, so everything that is not a skin stays real.
   The pack only adds files (it never replaces Cobblemon's), so removing it restores the normal look.

## Selection rules
Chain = [self, parent, grandparent, ...] (capped at 12). With n stages: Everstone = earliest (n >= 2); Eviolite =
stage index (n-1)/2 counted from the earliest (n >= 3); otherwise nothing changes (Eevee, Tauros, 2-stage lines
with Eviolite, ...).

## Debug commands (op level 2)
- `/evocosmetics status` : is our cosmetic data loaded?
- `/evocosmetics resolve <species>` : ancestry and Everstone/Eviolite targets for every form.
- `/evocosmetics audit` : resolves every species/form (details in the log).
- `/evocosmetics inspect` : species, aspects and cosmetic item of nearby Pokémon.
- `/evocosmetics generatepack` : (re)writes `evolution_cosmetics_pack.zip` and prints its SHA-1.
- `/evocosmetics verifypack` : checks the generated pack against Cobblemon's own client resolver code (see below).
- `/evocosmetics set <everstone|eviolite|none>` : **test helper**, sets the cosmetic slot of every Pokémon within 64 blocks
  through Cobblemon's own `Pokemon#setCosmeticItem`. Test worlds only.

## Verification status
Verified on a real dedicated server (Fabric Loader 0.17.2, Minecraft 1.21.1, Fabric API 0.116.6, Cobblemon 1.8.1,
Fabric Language Kotlin 1.13.6, Windows, Java 21), with no graphical client attached:
- The mod loads without errors; the pack is generated at startup (461 species, ~230 KiB, identical SHA-1 on every run).
- With an Everstone in the cosmetic slot the Pokémon keeps `species=cobblemon:charizard`, `form=Normal`; it only gains the
  marker aspect. Removing the item restores the original aspects.
- `/evocosmetics verifypack` runs Cobblemon's own `VaryingRenderableResolver` on the real resolver data plus the generated
  pack: 7277 checks (every species/form with an ancestor, both items, with and without shiny and every other aspect used
  by the species' variations) all give exactly the ancestor's poser/model/texture/layers, and Pokémon without the marker
  are unchanged. Examples: Charizard -> Charmander (flame layer included) / Charmeleon; Obstagoon -> Galarian Zigzagoon;
  Alolan Raichu -> Pichu; Perrserker -> Galarian Meowth.

NOT verified (needs a real client, see TESTING.md):
- That a Minecraft client loads the pack and the Pokémon is drawn with the ancestor's model in the world, party, PC and
  summary screens. Logic is verified; rendering itself is not.
- That the client computes/receives the marker aspect for the GUI screens (the world entity case is covered by the
  entity data checks above).
- Animations of descendants whose poses the ancestor's model lacks (e.g. a flying pose) may be imperfect.

## Limitations
- On a dedicated server, players must accept the server resource pack; a client that has neither the pack nor the jar
  sees nothing change. The bundled pack is built for Cobblemon 1.8.1; with another Cobblemon version or add-on species,
  rely on the pack the server regenerates at startup.
- Pose/animation: the ancestor's poser animates the real Pokémon's poses (walk, fly, swim, battle). A pose the ancestor
  lacks falls back to Cobblemon's default handling.
- Hitbox, scale and movement stay those of the real species, so a tiny Charmander skin may sit inside a Charizard-sized hitbox.
- Forms or lines whose look depends on feature properties instead of aspects (for example `wolf_form=dusk`) are not reproduced.
- Species added by other mods are covered automatically if their resolver files are in a loaded mod and they register
  pre-evolution data, but they are not in the shipped data file: add their names to
  `data/evolution_cosmetics/cosmetic_items/evolution_items.json` (or ship another `cosmetic_items` datapack).
- Other resource packs that replace a Pokémon model with their own resolver for the same aspects may take priority.

## Build
JDK 21 and Gradle 8.12 (Windows: unpack Gradle in a short path such as `C:\gradle-8.12`). Put the Cobblemon jar in
`libs/` as `Cobblemon-fabric-1_8_1_1_21_1.jar`, then run `gradle build`. A development client cannot boot a release
Cobblemon with Yarn mappings (Kotlin metadata), so test on a real server.

## License
MIT. The generated pack only contains JSON that points at Cobblemon's own models and textures; it contains none of
their files.
