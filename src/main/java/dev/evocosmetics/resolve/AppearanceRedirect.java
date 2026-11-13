package dev.evocosmetics.resolve;

import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides which species identifier / aspect set Cobblemon should <em>advertise to clients</em> for a Pokémon whose
 * cosmetic slot holds an Everstone or Eviolite. It is a pure function of (species, form, aspects): it never
 * modifies a Pokémon.
 */
public final class AppearanceRedirect {
    public record Redirect(Identifier speciesId, Set<String> aspects) {}

    private static final Map<String, Optional<EvolutionResolver.Stage>> CACHE = new ConcurrentHashMap<>();

    private AppearanceRedirect() {}

    /** Call when species/evolution data is reloaded. */
    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * @param species the Pokémon's real species
     * @param form    the Pokémon's real form
     * @param aspects the Pokémon's real aspects (containing a marker aspect from the cosmetic slot)
     * @return null when the normal appearance must be used.
     */
    public static Redirect resolve(Species species, FormData form, Set<String> aspects) {
        if (species == null || form == null || aspects == null || aspects.isEmpty()) {
            return null;
        }
        CosmeticMode mode = CosmeticMode.fromAspects(aspects);
        if (mode == null) {
            return null;
        }

        String key = species.getResourceIdentifier() + "|" + form.getName() + "|" + mode;
        Optional<EvolutionResolver.Stage> target =
                CACHE.computeIfAbsent(key, k -> EvolutionResolver.select(species, form, mode));
        if (target.isEmpty()) {
            return null;
        }

        EvolutionResolver.Stage stage = target.get();
        Set<String> out = new HashSet<>(aspects);
        out.removeIf(CosmeticMode::isMarker);
        if (form != species.getStandardForm()) {
            out.removeAll(form.getAspects()); // the real form's look must not leak onto the ancestor
        }
        if (stage.form() != stage.species().getStandardForm()) {
            out.addAll(stage.form().getAspects()); // the ancestor's own form (e.g. regional) look
        }
        return new Redirect(stage.species().getResourceIdentifier(), out);
    }
}
