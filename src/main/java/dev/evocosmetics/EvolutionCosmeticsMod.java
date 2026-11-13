package dev.evocosmetics;

import dev.evocosmetics.debug.DebugCommands;
import dev.evocosmetics.pack.PackGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!server.isDedicated()) {
                return; // single player / LAN: the pack must be installed in the client's resourcepacks folder
            }
            try {
                PackGenerator.Result r = PackGenerator.generateToGameDir();
                LOGGER.info("Client resource pack for {} species written to {} (sha1 {}). Host it and set resource-pack and "
                        + "resource-pack-sha1 in server.properties so players get the Everstone/Eviolite skins.",
                        r.species(), r.zip(), r.sha1());
            } catch (Exception e) {
                LOGGER.error("Could not generate the client resource pack", e);
            }
        });
        LOGGER.info("Cobblemon Evolution Cosmetics loaded (visual-only; uses Pokemon#cosmeticItem, never the held item).");
    }
}

