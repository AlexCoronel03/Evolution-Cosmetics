# Test plan (none of this has been executed yet)

Setup: `./gradlew runClient`, new creative world, `/give @s cobblemon:everstone`, `cobblemon:eviolite`, spawn/give Pokémon with Cobblemon commands.
First check the log for a cosmetic_items load error (see README risk #1).

| # | Case | Steps | Expected |
|---|------|-------|----------|
| 1 | Resolver audit | `/evocosmetics audit`, `/evocosmetics resolve charizard` | Charizard <- Charmeleon <- Charmander; Everstone->charmander, Eviolite->charmeleon |
| 2 | 3-stage | Charizard + Everstone, then Eviolite (right-click Pokémon with item) | Looks like Charmander / Charmeleon; `/pokemon` data still Charizard |
| 3 | 2-stage | Raticate: Everstone -> Rattata; Eviolite -> no change | as stated |
| 4 | Branching | Vaporeon / Umbreon / Sylveon + Everstone | all become Eevee |
| 5 | Long chain | Resolver output for lines with n >= 4 (audit log) | Rules in README |
| 6 | No pre-evolution | Tauros / legendary + Everstone | accepted, appearance unchanged, no errors |
| 7 | Regional/alternate forms | Alolan Raichu, Galarian forms, Hisuian lines | Ancestor of the same region where data exists; note any wrong results |
| 8 | Shiny | Shiny Charizard + Everstone | Shiny Charmander texture |
| 9 | Remove/replace | Take item back; swap Everstone<->Eviolite | Appearance updates immediately |
| 10 | Party/PC | Move equipped Pokémon party<->PC, open summary | Cosmetic persists; GUI model matches world model |
| 11 | Multiplayer | Dedicated server + 2 clients | Both see the ancestor look; relog and server restart keep it |
| 12 | Identity | Compare species, level, stats, moves, ability, held item, UUID before/after | Identical |
| 13 | Held item untouched | Give a held item + cosmetic Everstone, battle | Held item works normally; Everstone has no battle effect |
| 14 | Animations | Walk, swim, fly, battle idle, send-out/recall with each target | Note any missing-pose glitches (esp. flying Pokémon) |
