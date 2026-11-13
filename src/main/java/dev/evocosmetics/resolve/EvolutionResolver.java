package dev.evocosmetics.resolve;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.evolution.PreEvolution;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import dev.evocosmetics.mixin.CobblemonLazyPreEvolutionAccessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Pure, data-driven ancestry resolver. It only walks the pre-evolution links that Cobblemon has
 * registered ({@code FormData#getPreEvolution}, falling back to {@code Species#getPreEvolution});
 * no species is hardcoded.
 *
 * <p>Rules (deterministic, documented in README):
 * <ol>
 *   <li>Build the chain from the Pokémon's own species+form walking backward: [self, parent, grandparent, ...].
 *       Walking backward means branching lines (Eevee) always resolve to the Pokémon's real ancestry.</li>
 *   <li>Let n = chain length, ordered earliest-first. Everstone = stage 0, needs n >= 2.</li>
 *   <li>Eviolite = stage (n-1)/2 (integer division), needs n >= 3 so it is strictly between the earliest stage and
 *       the Pokémon itself. For n = 3 that is the middle stage, n = 4 the second stage, n = 5 the third.</li>
 *   <li>Otherwise there is no valid target and the normal appearance is kept.</li>
 * </ol>
 * Cycles and over-long chains are cut off safely.
 */
public final class EvolutionResolver {
    private static final int MAX_DEPTH = 12;

    private EvolutionResolver() {}

    public record Stage(Species species, FormData form) {
        public String label() {
            boolean standard = form == null || form == species.getStandardForm();
            return species.getName() + (standard ? "" : "(" + form.getName() + ")");
        }
    }

    /** Chain ordered latest-first: index 0 is the Pokémon itself, the last element is its earliest ancestor. */
    public static List<Stage> ancestry(Species species, FormData form) {
        List<Stage> chain = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Species curSpecies = species;
        FormData curForm = form != null ? form : species.getStandardForm();

        while (curSpecies != null && curForm != null && chain.size() < MAX_DEPTH) {
            String key = curSpecies.getResourceIdentifier() + "#" + curForm.getName();
            if (!seen.add(key)) {
                break; // cycle in registered data
            }
            chain.add(new Stage(curSpecies, curForm));

            PreEvolution pre = preEvolutionOf(curSpecies, curForm);
            if (pre == null) {
                break;
            }
            Species nextSpecies;
            FormData nextForm;
            try {
                nextSpecies = pre.getSpecies();
                nextForm = pre.getForm();
            } catch (RuntimeException unresolvable) {
                break; // lazily-resolved pre-evolution referenced something that does not exist
            }
            if (nextSpecies == null) {
                break;
            }
            curSpecies = nextSpecies;
            curForm = nextForm != null ? nextForm : nextSpecies.getStandardForm();
            if (curForm == nextSpecies.getStandardForm()) {
                // "meowth galarian": the form is expressed as an aspect, which PreEvolution#getForm ignores
                Set<String> aspects = aspectsOf(pre);
                if (!aspects.isEmpty()) {
                    FormData byAspects = nextSpecies.getForm(aspects);
                    if (byAspects != null) {
                        curForm = byAspects;
                    }
                }
            }
        }
        return chain;
    }

    public static Optional<Stage> select(Species species, FormData form, CosmeticMode mode) {
        List<Stage> chain = ancestry(species, form);
        int n = chain.size();
        return switch (mode) {
            case EARLIEST -> n >= 2 ? Optional.of(chain.get(n - 1)) : Optional.empty();
            case MIDDLE -> {
                if (n < 3) {
                    yield Optional.empty();
                }
                int earliestFirstIndex = (n - 1) / 2;
                yield Optional.of(chain.get(n - 1 - earliestFirstIndex));
            }
        };
    }

    private static Set<String> aspectsOf(PreEvolution pre) {
        if ((Object) pre instanceof CobblemonLazyPreEvolutionAccessor accessor) {
            try {
                PokemonProperties props = accessor.evocosmetics$getProperties();
                Set<String> aspects = props == null ? null : props.getAspects();
                return aspects == null ? Set.of() : aspects;
            } catch (RuntimeException ignored) {
                // fall through: keep the form PreEvolution#getForm reported
            }
        }
        return Set.of();
    }

    private static PreEvolution preEvolutionOf(Species species, FormData form) {
        PreEvolution pre = form != null ? form.getPreEvolution() : null;
        if (pre == null) {
            pre = species.getPreEvolution();
        }
        return pre;
    }
}
