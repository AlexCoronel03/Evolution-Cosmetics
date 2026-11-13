# Testing (server-side build)

## A. Server console / RCON (no client needed)
1. Start the server with Cobblemon 1.8.1 + this jar. Expect `Cobblemon Evolution Cosmetics loaded`, no mixin errors.
2. `/evocosmetics status` -> `Ours: [evolution_cosmetics:evolution_items (1025 pokemon filters)]`.
3. `/evocosmetics resolve charizard` -> `Everstone -> Charmander | Eviolite -> Charmeleon`.
4. `/spawnpokemon charizard <x> <y> <z>`, then `/evocosmetics set everstone`, then `/evocosmetics inspect`:
   `REAL species=cobblemon:charizard ... ADVERTISED species=cobblemon:charmander`.
   `set eviolite` -> `charmeleon`; `set none` -> `charizard`. (Entities only refresh while they tick: stay nearby, or
   `/forceload add` the area on an empty test server.)

## B. With a real client (what players will see)
Join with a normal Cobblemon 1.8.1 client that does NOT have this mod.
1. Own a Charizard (or any 3-stage Pokémon), hold an Everstone, open the interaction wheel -> cosmetic option -> apply.
2. Send the Pokémon out: it must look like Charmander. Eviolite: like Charmeleon. Remove the item: Charizard again.
3. Check regional lines: Perrserker (Galarian Meowth), Obstagoon, Mr. Rime, Raichu (Alola).
4. Check that a shiny keeps its colour, and that a flying/swimming Pokémon does not glitch or crash the client.
5. Confirm the party/PC/summary screens still show the real species (expected, see README).
6. Rejoin: the appearance must persist.
Report anything wrong together with the client `latest.log`.
