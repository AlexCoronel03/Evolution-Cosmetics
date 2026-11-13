package dev.evocosmetics.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Reads the model "resolver" files Cobblemon (and add-on mods) ship under
 * {@code assets/<ns>/bedrock/pokemon/resolvers/} and merges them per species exactly like the client does:
 * group by the {@code species} field, sort by {@code order}, concatenate the {@code variations}.
 * The files are plain data inside the mod jars, which the server has too.
 */
public final class ResolverIndex {
    public static final String OWN_NAMESPACE = "evolution_cosmetics";
    private static final String MARK = "/bedrock/pokemon/resolvers/";

    private record ModelSet(int order, JsonArray variations) {}

    private final Map<String, List<JsonObject>> bySpecies = new LinkedHashMap<>();

    /** Merged, ordered variations of a species ({@code namespace:name}); empty if it has no resolver. */
    public List<JsonObject> variations(String speciesId) {
        return bySpecies.getOrDefault(speciesId, List.of());
    }

    public static ResolverIndex load() throws IOException {
        Map<String, List<ModelSet>> sets = new LinkedHashMap<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            for (Path root : mod.getRootPaths()) {
                Path assets = root.resolve("assets");
                if (!Files.isDirectory(assets)) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(assets)) {
                    for (Path file : (Iterable<Path>) files.filter(ResolverIndex::isResolver)::iterator) {
                        readInto(sets, file);
                    }
                }
            }
        }
        ResolverIndex index = new ResolverIndex();
        for (Map.Entry<String, List<ModelSet>> e : sets.entrySet()) {
            List<ModelSet> sorted = new ArrayList<>(e.getValue());
            sorted.sort(Comparator.comparingInt(ModelSet::order)); // stable, like Kotlin's sortedBy
            List<JsonObject> flat = new ArrayList<>();
            for (ModelSet s : sorted) {
                for (JsonElement v : s.variations()) {
                    flat.add(v.getAsJsonObject());
                }
            }
            index.bySpecies.put(e.getKey(), flat);
        }
        return index;
    }

    private static boolean isResolver(Path p) {
        String s = p.toString().replace(java.io.File.separatorChar, '/');
        return s.endsWith(".json") && s.contains(MARK) && !s.contains("/assets/" + OWN_NAMESPACE + "/");
    }

    private static void readInto(Map<String, List<ModelSet>> sets, Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (!json.has("species") || !json.has("variations")) {
                return;
            }
            int order = json.has("order") ? json.get("order").getAsInt() : 0;
            sets.computeIfAbsent(json.get("species").getAsString(), k -> new ArrayList<>())
                    .add(new ModelSet(order, json.getAsJsonArray("variations")));
        } catch (RuntimeException notAResolver) {
            // a malformed third-party file must not break pack generation
        }
    }
}
