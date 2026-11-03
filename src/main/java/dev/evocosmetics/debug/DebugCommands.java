package dev.evocosmetics.debug;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.cobblemon.mod.common.CobblemonCosmeticItems;
import com.cobblemon.mod.common.pokemon.cosmetic.CosmeticItemAssignment;
import com.cobblemon.mod.common.pokemon.cosmetic.CosmeticItemDefinition;
import dev.evocosmetics.EvolutionCosmeticsMod;
import dev.evocosmetics.resolve.CosmeticMode;
import dev.evocosmetics.resolve.EvolutionResolver;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * /evocosmetics resolve <species>  - prints each form's ancestry and the Everstone / Eviolite targets.
 * /evocosmetics audit              - resolves every registered species+form; summary in chat, details in the log.
 * Read-only: no Pokémon is modified.
 */
public final class DebugCommands {
    private DebugCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("evocosmetics")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("resolve")
                                .then(CommandManager.argument("species", StringArgumentType.word())
                                        .executes(DebugCommands::resolve)))
                        .then(CommandManager.literal("status").executes(DebugCommands::status))
                        .then(CommandManager.literal("audit").executes(DebugCommands::audit))));
    }

    private static List<FormData> formsOf(Species species) {
        List<FormData> forms = new ArrayList<>();
        forms.add(species.getStandardForm());
        for (FormData f : species.getForms()) {
            if (!forms.contains(f)) {
                forms.add(f);
            }
        }
        return forms;
    }

    private static String describe(Species species, FormData form) {
        List<EvolutionResolver.Stage> chain = EvolutionResolver.ancestry(species, form);
        String path = chain.stream().map(EvolutionResolver.Stage::label).collect(Collectors.joining(" <- "));
        Optional<EvolutionResolver.Stage> e = EvolutionResolver.select(species, form, CosmeticMode.EARLIEST);
        Optional<EvolutionResolver.Stage> m = EvolutionResolver.select(species, form, CosmeticMode.MIDDLE);
        return path + "  | Everstone -> " + e.map(EvolutionResolver.Stage::label).orElse("none")
                + " | Eviolite -> " + m.map(EvolutionResolver.Stage::label).orElse("none");
    }

    private static int resolve(CommandContext<ServerCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "species").toLowerCase();
        Species species = PokemonSpecies.getByName(name);
        if (species == null) {
            ctx.getSource().sendError(Text.literal("Unknown species: " + name));
            return 0;
        }
        for (FormData form : formsOf(species)) {
            String line = form.getName() + ": " + describe(species, form);
            ctx.getSource().sendFeedback(() -> Text.literal(line), false);
        }
        return 1;
    }

    /** Reports whether Cobblemon actually loaded an assignment that contains our Everstone/Eviolite markers. */
    private static int status(CommandContext<ServerCommandSource> ctx) {
        int total = CobblemonCosmeticItems.cosmeticItems.size();
        List<String> ours = new ArrayList<>();
        for (CosmeticItemAssignment assignment : CobblemonCosmeticItems.cosmeticItems) {
            for (CosmeticItemDefinition def : assignment.getCosmeticItems()) {
                boolean marker = def.getAspects().stream().anyMatch(CosmeticMode::isMarker);
                if (marker) {
                    String entry = assignment.getId() + " (" + assignment.getPokemon().size() + " pokemon filters)";
                    if (!ours.contains(entry)) {
                        ours.add(entry);
                    }
                }
            }
        }
        String msg = "Cosmetic-item assignments loaded: " + total + ". Ours: "
                + (ours.isEmpty() ? "NONE (data not loaded - check /datapack list and the log)" : ours);
        ctx.getSource().sendFeedback(() -> Text.literal(msg), false);
        return 1;
    }

    private static int audit(CommandContext<ServerCommandSource> ctx) {
        Map<Integer, Integer> byLength = new TreeMap<>();
        int total = 0;
        for (Species species : PokemonSpecies.getSpecies()) {
            for (FormData form : formsOf(species)) {
                total++;
                int n = EvolutionResolver.ancestry(species, form).size();
                byLength.merge(n, 1, Integer::sum);
                EvolutionCosmeticsMod.LOGGER.info("[audit] {} / {} : {}", species.getName(), form.getName(), describe(species, form));
            }
        }
        int finalTotal = total;
        ctx.getSource().sendFeedback(() -> Text.literal(
                "Audited " + finalTotal + " species/forms. Chain length -> count: " + byLength
                        + " (details in the server log, lines starting with [audit])"), false);
        return 1;
    }
}
