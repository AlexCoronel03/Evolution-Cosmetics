package dev.evocosmetics.client;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import dev.evocosmetics.resolve.CosmeticMode;
import dev.evocosmetics.resolve.EvolutionResolver;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side logic behind the VaryingModelRepository mixin. It never touches Pokémon data: it only
 * decides which species identifier / aspect set the renderer should use for a given render state.
 */
public final class AppearanceRedirect {
    public record Redirect(Identifier speciesId, Set<String> aspects) {}

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    private static final Map<String, Optional<EvolutionResolver.Stage>> CACHE = new ConcurrentHashMap<>();

    private AppearanceRedirect() {}

    public static void clearCache() {
        CACHE.clear();
    }

    public static void enter() {
        ACTIVE.set(true);
    }

    public static void exit() {
        ACTIVE.set(false);
    }

    /** @return null when the normal appearance must be used. */
    public static Redirect resolve(Identifier speciesId, PosableState state) {
        if (ACTIVE.get() || state == null) {
            return null;
        }
        Set<String> aspects = state.getCurrentAspects();
        if (aspects == null || aspects.isEmpty()) {
            return null;
        }
        CosmeticMode mode = CosmeticMode.fromAspects(aspects);
        if (mode == null) {
            return null;
        }
        Species species = PokemonSpecies.getByIdentifier(speciesId);
        if (species == null) {
            return null;
        }
        FormData originalForm = species.getForm(aspects);

        String key = speciesId + "|" + originalForm.getName() + "|" + mode;
        Optional<EvolutionResolver.Stage> target =
                CACHE.computeIfAbsent(key, k -> EvolutionResolver.select(species, originalForm, mode));
        if (target.isEmpty()) {
            return null;
        }

        EvolutionResolver.Stage stage = target.get();
        Set<String> out = new HashSet<>(aspects);
        out.removeIf(CosmeticMode::isMarker);
        if (originalForm != species.getStandardForm()) {
            out.removeAll(originalForm.getAspects()); // original form's look must not leak onto the ancestor
        }
        if (stage.form() != stage.species().getStandardForm()) {
            out.addAll(stage.form().getAspects());    // ancestor's own form (e.g. regional) look
        }
        return new Redirect(stage.species().getResourceIdentifier(), out);
    }
}
