package dev.zsskayr.merlins_inferno.worldgen.elysium.core;

/**
 * The El'ysium biomes. An island has a main biome and, unless it is a sea, may hold regions of its climate neighbours. CHAINFALL is the band of stepping-stone
 * platforms around the giant chains; UNDERHANG is the 3D biome of the space under/inside the islands' undersides.
 * {@link #id} is the biome JSON path under data/merlins_inferno/worldgen/biome/.
 */
public enum ElysiumBiome {
    LARK_MEADOWS("lark_meadows", Size.LARGE, Size.GIANT),
    WINDLESS_STEPPE("windless_steppe", Size.MEDIUM, Size.LARGE),
    HIGHPINE_FOREST("highpine_forest", Size.LARGE, Size.LARGE),
    MOSSY_WEALD("mossy_weald", Size.MEDIUM, Size.LARGE),
    ARGENT_PEAKS("argent_peaks", Size.SMALL, Size.MEDIUM),
    STONECROWN_CLIFFS("stonecrown_cliffs", Size.MEDIUM, Size.MEDIUM),
    CASCADE_VALE("cascade_vale", Size.MEDIUM, Size.LARGE),
    MISTFALL_GORGE("mistfall_gorge", Size.SMALL, Size.MEDIUM),
    GLASS_SEA("glass_sea", Size.MEDIUM, Size.COLOSSAL),
    AZURE_SHALLOWS("azure_shallows", Size.MEDIUM, Size.MEDIUM),
    RIME_EXPANSE("rime_expanse", Size.LARGE, Size.LARGE),
    FROSTCREST_RANGE("frostcrest_range", Size.MEDIUM, Size.MEDIUM),
    CHAINFALL("chainfall", Size.PLATFORM, Size.PLATFORM),
    UNDERHANG("underhang", Size.PLATFORM, Size.PLATFORM);

    /** Island size classes, by width in blocks. PLATFORM = Chainfall stepping stones (40..160), not a real island. */
    public enum Size {
        PLATFORM(40, 160), SMALL(300, 500), MEDIUM(500, 900), LARGE(900, 1500), GIANT(1500, 2200), COLOSSAL(3000, 9000);
        public final int minWidth, maxWidth;
        Size(int min, int max) { this.minWidth = min; this.maxWidth = max; }
    }

    public final String id;
    public final Size minSize, maxSize;

    ElysiumBiome(String id, Size minSize, Size maxSize) {
        this.id = id;
        this.minSize = minSize;
        this.maxSize = maxSize;
    }
}
