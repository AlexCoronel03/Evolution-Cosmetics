package dev.evocosmetics;

import dev.evocosmetics.debug.DebugCommands;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint (server logic only, no client code). The mod stores no data and changes no Pokémon: the Everstone/Eviolite
 * assignment lives in data/evolution_cosmetics/cosmetic_items/ (synced to clients by Cobblemon itself), and
 * PokemonServerDelegateMixin makes the server advertise the ancestor's species/aspects in the entity's synced data.
 */
public class EvolutionCosmeticsMod implements ModInitializer {
    public static final String MOD_ID = "evolution_cosmetics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        DebugCommands.register();
        LOGGER.info("Cobblemon Evolution Cosmetics loaded (visual-only; uses Pokemon#cosmeticItem, never the held item).");
    }
}

