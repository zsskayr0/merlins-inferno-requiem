package dev.zsskayr.merlins_inferno.client;

import java.util.Map;

/** Adds dimension fallbacks in memory; shader archives and player settings are never rewritten. */
public final class IrisDimensionAliases {
    private static final System.Logger LOGGER = System.getLogger(IrisDimensionAliases.class.getName());

    private IrisDimensionAliases() {}

    public static void add(Map<Object, String> dimensions) {
        try {
            // Iris is optional. Only resolve its ID class when a shader pack actually loads.
            var constructor = Class.forName("net.irisshaders.iris.shaderpack.materialmap.NamespacedId")
                    .getConstructor(String.class, String.class);
            Object oblivion = constructor.newInstance("merlins_inferno", "oblivion");
            if (dimensions.containsKey(oblivion)) return;

            Object nether = constructor.newInstance("minecraft", "the_nether");
            String netherPrograms = dimensions.get(nether);
            // Use the pack's actual folder, including custom folder names. No Nether programs means no alias.
            if (netherPrograms != null) dimensions.put(oblivion, netherPrograms);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Could not add Oblivion's Iris dimension fallback", e);
        }
    }
}
