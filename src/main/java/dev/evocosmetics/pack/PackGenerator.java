package dev.evocosmetics.pack;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.evocosmetics.resolve.CosmeticMode;
import dev.evocosmetics.resolve.EvolutionResolver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds the client resource pack that makes Everstone/Eviolite change the <em>skin only</em>.
 *
 * <p>Cobblemon chooses poser, model, texture and render layers per species from "variations" that are active when all
 * of their {@code aspects} are present on the Pokémon; the last matching variation wins per field and layers merge by
 * name. For every species we append variations that require the marker aspect of the cosmetic slot and that carry the
 * ancestor's poser/model/texture/layers (copied from the ancestor's own resolver data). The Pokémon's species, name,
 * stats, cry, hitbox, ... are never touched because the species identifier stays the real one.
 */
public final class PackGenerator {
    public static final int ORDER = 10_000;      // after every resolver file shipped by Cobblemon/add-ons
    public static final int PACK_FORMAT = 34;     // Minecraft 1.21 - 1.21.1

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private PackGenerator() {}

    private record Group(Set<String> scope, List<JsonObject> variations) {}

    /** @return speciesId ({@code namespace:name}) to the variations to append after the species' own ones. */
    public static Map<String, List<JsonObject>> build(ResolverIndex index) {
        Map<String, List<JsonObject>> out = new TreeMap<>();
        for (Species species : PokemonSpecies.getSpecies()) {
            List<JsonObject> list = buildSpecies(index, species);
            if (!list.isEmpty()) {
                out.put(species.getResourceIdentifier().toString(), list);
            }
        }
        return out;
    }

    private static List<JsonObject> buildSpecies(ResolverIndex index, Species species) {
        FormData standard = species.getStandardForm();
        List<Group> groups = new ArrayList<>();
        for (CosmeticMode mode : CosmeticMode.values()) {
            String marker = mode.aspect();
            Optional<EvolutionResolver.Stage> standardTarget = EvolutionResolver.select(species, standard, mode);
            EvolutionResolver.Stage standardStage = standardTarget.orElse(null);
            if (standardStage != null) {
                groups.add(copyStage(index, scope(marker, List.of()), standardStage));
            }
            for (FormData form : species.getForms()) {
                if (form == standard || form.getAspects().isEmpty()) {
                    continue; // cannot be told apart by aspects
                }
                Optional<EvolutionResolver.Stage> t = EvolutionResolver.select(species, form, mode);
                EvolutionResolver.Stage use;
                if (t.isPresent()) {
                    if (standardStage != null && sameStage(t.get(), standardStage)) {
                        continue; // the marker-only variations already cover this form
                    }
                    use = t.get();
                } else if (standardStage != null) {
                    use = new EvolutionResolver.Stage(species, form); // "no change" for this form: replay its own look
                } else {
                    continue;
                }
                groups.add(copyStage(index, scope(marker, form.getAspects()), use));
            }
        }
        if (groups.isEmpty()) {
            return List.of();
        }

        // Layers are merged by name across all matching variations, so layers of the real species (or of another
        // group) that the ancestor does not have must be switched off explicitly.
        Set<String> allLayerNames = new TreeSet<>();
        for (JsonObject v : index.variations(species.getResourceIdentifier().toString())) {
            allLayerNames.addAll(layerNames(v));
        }
        for (Group g : groups) {
            for (JsonObject v : g.variations()) {
                allLayerNames.addAll(layerNames(v));
            }
        }
        List<JsonObject> result = new ArrayList<>();
        for (Group g : groups) {
            Set<String> declared = new HashSet<>();
            for (JsonObject v : g.variations()) {
                if (aspectsOf(v).equals(g.scope())) {
                    declared.addAll(layerNames(v));
                }
            }
            JsonArray off = new JsonArray();
            for (String name : allLayerNames) {
                if (!declared.contains(name)) {
                    JsonObject layer = new JsonObject();
                    layer.addProperty("name", name);
                    layer.addProperty("enabled", false);
                    off.add(layer);
                }
            }
            if (!off.isEmpty()) {
                JsonObject disable = new JsonObject();
                disable.add("aspects", toArray(g.scope()));
                disable.add("layers", off);
                result.add(disable);
            }
            result.addAll(g.variations());
        }
        return result;
    }

    /** The ancestor's variations that are valid for its form, re-keyed under {@code scope}. */
    private static Group copyStage(ResolverIndex index, Set<String> scope, EvolutionResolver.Stage target) {
        Species ancestor = target.species();
        Set<String> allFormAspects = allFormAspects(ancestor);
        Set<String> targetFormAspects = new HashSet<>(target.form().getAspects());
        List<JsonObject> out = new ArrayList<>();
        for (JsonObject v : index.variations(ancestor.getResourceIdentifier().toString())) {
            Set<String> va = aspectsOf(v);
            Set<String> formPart = new HashSet<>(va);
            formPart.retainAll(allFormAspects);
            if (!targetFormAspects.containsAll(formPart)) {
                continue; // belongs to a different form of the ancestor
            }
            Set<String> aspects = new LinkedHashSet<>(scope);
            for (String a : va) {
                if (!allFormAspects.contains(a)) {
                    aspects.add(a); // shiny, gender, patterns ...
                }
            }
            JsonObject copy = v.deepCopy();
            copy.add("aspects", toArray(aspects));
            out.add(copy);
        }
        return new Group(scope, out);
    }

    public static Set<String> allFormAspects(Species species) {
        Set<String> all = new HashSet<>(species.getStandardForm().getAspects());
        for (FormData f : species.getForms()) {
            all.addAll(f.getAspects());
        }
        return all;
    }

    static Set<String> scope(String marker, List<String> formAspects) {
        Set<String> s = new LinkedHashSet<>();
        s.add(marker);
        s.addAll(formAspects);
        return s;
    }

    private static boolean sameStage(EvolutionResolver.Stage a, EvolutionResolver.Stage b) {
        return a.species().getResourceIdentifier().equals(b.species().getResourceIdentifier())
                && a.form().getName().equals(b.form().getName());
    }

    public static Set<String> aspectsOf(JsonObject variation) {
        Set<String> s = new LinkedHashSet<>();
        if (variation.has("aspects") && variation.get("aspects").isJsonArray()) {
            for (JsonElement e : variation.getAsJsonArray("aspects")) {
                s.add(e.getAsString());
            }
        }
        return s;
    }

    public static Set<String> layerNames(JsonObject variation) {
        Set<String> s = new LinkedHashSet<>();
        if (variation.has("layers") && variation.get("layers").isJsonArray()) {
            for (JsonElement e : variation.getAsJsonArray("layers")) {
                if (e.isJsonObject()) {
                    // Cobblemon names a layer without a "name" field "" (they then merge under that one name)
                    s.add(e.getAsJsonObject().has("name") ? e.getAsJsonObject().get("name").getAsString() : "");
                }
            }
        }
        return s;
    }

    private static JsonArray toArray(Set<String> values) {
        JsonArray a = new JsonArray();
        values.forEach(a::add);
        return a;
    }

    public record Result(Path zip, String sha1, int species) {}

    /** Generates the pack from the data currently loaded and writes it to {@code <game dir>/evolution_cosmetics_pack.zip}. */
    public static Result generateToGameDir() throws IOException {
        ResolverIndex index = ResolverIndex.load();
        Map<String, List<JsonObject>> generated = build(index);
        Path zip = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir()
                .resolve("evolution_cosmetics_pack.zip").toAbsolutePath();
        String sha1 = write(zip, generated);
        return new Result(zip, sha1, generated.size());
    }

    /** JSON of one generated resolver file. */
    public static String fileJson(String speciesId, List<JsonObject> variations) {
        JsonObject root = new JsonObject();
        root.addProperty("order", ORDER);
        root.addProperty("species", speciesId);
        JsonArray arr = new JsonArray();
        variations.forEach(arr::add);
        root.add("variations", arr);
        return GSON.toJson(root);
    }

    /** Writes the pack zip (deterministic bytes) and returns its SHA-1. */
    public static String write(Path zip, Map<String, List<JsonObject>> generated) throws IOException {
        try (OutputStream os = Files.newOutputStream(zip); ZipOutputStream out = new ZipOutputStream(os)) {
            put(out, "pack.mcmeta", "{\"pack\":{\"pack_format\":" + PACK_FORMAT
                    + ",\"description\":\"Cobblemon Evolution Cosmetics: Everstone/Eviolite skins (generated)\"}}");
            for (Map.Entry<String, List<JsonObject>> e : generated.entrySet()) {
                String file = e.getKey().replace(':', '_').replace('/', '_');
                put(out, "assets/" + ResolverIndex.OWN_NAMESPACE + "/bedrock/pokemon/resolvers/" + file + ".json",
                        fileJson(e.getKey(), e.getValue()));
            }
        }
        try {
            byte[] sum = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(zip));
            StringBuilder sb = new StringBuilder();
            for (byte b : sum) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void put(ZipOutputStream out, String name, String content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTimeLocal(LocalDateTime.of(1980, 1, 1, 0, 0));
        out.putNextEntry(entry);
        out.write(content.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }
}
