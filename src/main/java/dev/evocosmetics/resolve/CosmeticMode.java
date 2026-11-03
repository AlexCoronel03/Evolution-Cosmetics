package dev.evocosmetics.resolve;

import java.util.Collection;

/** The two appearance modes and the marker aspect Cobblemon's cosmetic slot attaches for each. */
public enum CosmeticMode {
    /** Everstone: earliest stage of the Pokémon's own ancestry. */
    EARLIEST("evo_cosmetic-earliest"),
    /** Eviolite: middle stage of the Pokémon's own ancestry. */
    MIDDLE("evo_cosmetic-middle");

    private final String aspect;

    CosmeticMode(String aspect) {
        this.aspect = aspect;
    }

    public String aspect() {
        return aspect;
    }

    public static CosmeticMode fromAspects(Collection<String> aspects) {
        for (CosmeticMode mode : values()) {
            if (aspects.contains(mode.aspect)) {
                return mode;
            }
        }
        return null;
    }

    public static boolean isMarker(String aspect) {
        for (CosmeticMode mode : values()) {
            if (mode.aspect.equals(aspect)) {
                return true;
            }
        }
        return false;
    }
}
