# Testing

## A. Server only (no client needed)
1. Start the server with Cobblemon 1.8.1 + this jar. Expect `Cobblemon Evolution Cosmetics loaded` and
   `Client resource pack for N species written to ...`.
2. `/evocosmetics status` -> `Ours: [evolution_cosmetics:evolution_items (1025 pokemon filters)]`.
3. `/evocosmetics resolve charizard` -> `Everstone -> Charmander | Eviolite -> Charmeleon`.
4. `/evocosmetics verifypack` -> `... checks over N species, 0 failures`. The log also prints `[verifypack sample]` lines
   with the poser/model/texture Cobblemon's own resolver picks, e.g. Charizard + `evo_cosmetic-earliest` ->
   `poser=cobblemon:charmander ... charmander.png`.
5. `/spawnpokemon charizard <x> <y> <z>`, `/evocosmetics set everstone`, `/evocosmetics inspect` -> still
   `species=cobblemon:charizard`, aspects now contain `evo_cosmetic-earliest`. `set none` removes it.

## B. With a real client (what players will see)
Single player: put only the jar in `mods/` and skip step 1.
1. Host `evolution_cosmetics_pack.zip`, set `resource-pack` and `resource-pack-sha1` in `server.properties`, restart.
2. Join with a normal Cobblemon 1.8.1 client that has NOT got this mod. Accept the resource-pack prompt.
3. Own a Charizard (or any 3-stage Pokémon), hold an Everstone, open the interaction wheel, choose the cosmetic option.
4. Send it out: it must look like Charmander (flame included). Eviolite: Charmeleon. Remove the item: Charizard again.
5. Check that nothing else changed: name, level, moves and cry are Charizard's; the summary screen shows Charizard data.
6. Check the party, PC and summary 3D previews (they should show the same skin), a shiny Pokémon, and regional lines
   (Perrserker, Obstagoon, Mr. Rime, Raichu of Alola).
7. Check a flying or swimming Pokémon for animation glitches or crashes, and that rejoining keeps the skin.
Report anything wrong together with the client `latest.log`.
