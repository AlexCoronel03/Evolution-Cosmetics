package dev.evocosmetics.pack;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.scheduling.SchedulingTracker;
import com.cobblemon.mod.common.client.render.ModelAssetVariation;
import com.cobblemon.mod.common.client.render.ModelLayer;
import com.cobblemon.mod.common.client.render.ModelVariationSet;
import com.cobblemon.mod.common.client.render.VaryingRenderableResolver;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.evocosmetics.resolve.CosmeticMode;
import dev.evocosmetics.resolve.EvolutionResolver;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Checks the generated pack against Cobblemon's own client selection code ({@link VaryingRenderableResolver}) without
 * needing a graphical client. For every species, form, mode and a set of extra aspects (shiny, gender, ...), the
 * result with the pack applied and the marker aspect present must equal what the ancestor's resolver gives for the
 * ancestor's own aspects; and with no marker the result must be unchanged.
 */
public final class PackVerifier {
    public record Report(int checks, int speciesChecked, int failureCount, List<String> failures) {}

    private static final class State extends PosableState {
        State(Set<String> aspects) {
            setCurrentAspects(new HashSet<>(aspects));
        }

        private final SchedulingTracker tracker = new SchedulingTracker();

        @Override
        public SchedulingTracker getSchedulingTracker() {
            return tracker;
        }

        @Override
        public Entity getEntity() {
            return null;
        }

        @Override
        public void updatePartialTicks(float f) {}
    }

    private PackVerifier() {}

    public static Report run(ResolverIndex index, Map<String, List<JsonObject>> generated) {
        Gson gson = VaryingRenderableResolver.Companion.getGSON();
        Map<String, VaryingRenderableResolver> original = new HashMap<>();
        List<String> failures = new ArrayList<>();
        int checks = 0;
        int speciesChecked = 0;
        int[] failureCount = {0};

        for (Species species : PokemonSpecies.getSpecies()) {
            String id = species.getResourceIdentifier().toString();
            List<JsonObject> extra = generated.getOrDefault(id, List.of());
            if (extra.isEmpty()) {
                continue;
            }
            speciesChecked++;
            VaryingRenderableResolver own = resolver(gson, original, index, id);
            List<JsonObject> combined = new ArrayList<>(index.variations(id));
            combined.addAll(extra);
            VaryingRenderableResolver patched = build(gson, id, combined);

            List<FormData> forms = new ArrayList<>();
            forms.add(species.getStandardForm());
            for (FormData f : species.getForms()) {
                if (f != species.getStandardForm() && !f.getAspects().isEmpty()) {
                    forms.add(f);
                }
            }
            for (CosmeticMode mode : CosmeticMode.values()) {
                for (FormData form : forms) {
                    Optional<EvolutionResolver.Stage> target = EvolutionResolver.select(species, form, mode);
                    VaryingRenderableResolver expectedResolver = target.isPresent()
                            ? resolver(gson, original, index, target.get().species().getResourceIdentifier().toString())
                            : own;
                    Set<String> expectedForm = target.isPresent()
                            ? new HashSet<>(target.get().form().getAspects()) : new HashSet<>(form.getAspects());
                    Set<String> formAspects = PackGenerator.allFormAspects(species);
                    if (target.isPresent()) {
                        formAspects.addAll(PackGenerator.allFormAspects(target.get().species()));
                    }
                    for (Set<String> extras : extraSets(index, id, target, formAspects)) {
                        Set<String> actualAspects = new HashSet<>(extras);
                        actualAspects.addAll(form.getAspects());
                        actualAspects.add(mode.aspect());
                        Set<String> expectedAspects = new HashSet<>(extras);
                        expectedAspects.addAll(expectedForm);
                        checks++;
                        String where = id + "/" + form.getName() + " " + mode + " " + extras;
                        try {
                            String a = describe(patched, new State(actualAspects));
                            String e = describe(expectedResolver, new State(expectedAspects));
                            if (!a.equals(e)) {
                                fail(failures, failureCount, where + "\n      got      " + a + "\n      expected " + e);
                            }
                        } catch (Throwable t) {
                            fail(failures, failureCount, where + " threw " + t);
                        }
                    }
                }
            }
            // without a marker nothing may change
            for (FormData form : forms) {
                checks++;
                Set<String> plain = new HashSet<>(form.getAspects());
                try {
                    String a = describe(patched, new State(plain));
                    String e = describe(own, new State(plain));
                    if (!a.equals(e)) {
                        fail(failures, failureCount, id + "/" + form.getName() + " without marker changed\n      got      " + a
                                + "\n      expected " + e);
                    }
                } catch (Throwable t) {
                    fail(failures, failureCount, id + "/" + form.getName() + " no-marker threw " + t);
                }
            }
        }
        return new Report(checks, speciesChecked, failureCount[0], failures);
    }

    /** What Cobblemon's client resolver picks for {@code species} when its aspects are {@code aspects}. */
    public static String sample(ResolverIndex index, Map<String, List<JsonObject>> generated, String speciesId,
                                Set<String> aspects) {
        Gson gson = VaryingRenderableResolver.Companion.getGSON();
        List<JsonObject> combined = new ArrayList<>(index.variations(speciesId));
        combined.addAll(generated.getOrDefault(speciesId, List.of()));
        return describe(build(gson, speciesId, combined), new State(aspects));
    }

    private static void fail(List<String> failures, int[] count, String message) {
        count[0]++;
        if (failures.size() < 40) {
            failures.add(message);
        }
    }

    private static Set<Set<String>> extraSets(ResolverIndex index, String speciesId,
                                              Optional<EvolutionResolver.Stage> target, Set<String> formAspects) {
        Set<String> names = new TreeSet<>();
        for (JsonObject v : index.variations(speciesId)) {
            names.addAll(PackGenerator.aspectsOf(v));
        }
        target.ifPresent(t -> {
            for (JsonObject v : index.variations(t.species().getResourceIdentifier().toString())) {
                names.addAll(PackGenerator.aspectsOf(v));
            }
        });
        names.removeAll(formAspects);
        Set<Set<String>> sets = new java.util.LinkedHashSet<>();
        sets.add(Set.of());
        for (String n : names) {
            sets.add(Set.of(n));
            if (!n.equals("shiny")) {
                sets.add(Set.of(n, "shiny"));
            }
        }
        return sets;
    }

    private static String describe(VaryingRenderableResolver r, PosableState state) {
        List<String> layers = new ArrayList<>();
        for (ModelLayer l : r.getResolvedLayers(state)) {
            layers.add(l.getName() + "=" + (l.getTexture() == null ? "-" : l.getTexture().invoke(state))
                    + (l.getEmissive() ? "+e" : "") + (l.getTranslucent() ? "+t" : ""));
        }
        layers.sort(String::compareTo);
        return "poser=" + r.getResolvedPoser(state) + " model=" + r.getResolvedModel(state)
                + " tex=" + r.getResolvedTexture(state) + " layers=" + layers;
    }

    private static VaryingRenderableResolver resolver(Gson gson, Map<String, VaryingRenderableResolver> cache,
                                                      ResolverIndex index, String id) {
        return cache.computeIfAbsent(id, k -> build(gson, k, index.variations(k)));
    }

    private static VaryingRenderableResolver build(Gson gson, String id, List<JsonObject> variations) {
        JsonArray arr = new JsonArray();
        variations.forEach(arr::add);
        JsonObject set = new JsonObject();
        set.addProperty("order", 0);
        set.addProperty("species", id);
        set.add("variations", arr);
        ModelVariationSet parsed = gson.fromJson(set, ModelVariationSet.class);
        List<ModelAssetVariation> list = new ArrayList<>(parsed.getVariations());
        return new VaryingRenderableResolver(Identifier.of(id), list);
    }
}
