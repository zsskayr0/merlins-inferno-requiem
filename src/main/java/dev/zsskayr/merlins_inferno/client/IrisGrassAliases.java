package dev.zsskayr.merlins_inferno.client;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;

/** Copies the active shader's vanilla grass material IDs to Cascade Vale's grass. */
public final class IrisGrassAliases {
    private IrisGrassAliases() {}

    public static void add(Int2ObjectLinkedOpenHashMap<List<?>> blocks) {
        try {
            // Kept reflective so Iris remains entirely optional for the mod and its build.
            Class<?> entry = Class.forName("net.irisshaders.iris.shaderpack.materialmap.BlockEntry");
            Method parse = entry.getMethod("parse", String.class);
            Object shortGrass = parse.invoke(null, "minecraft:short_grass");
            Object tallLower = parse.invoke(null, "minecraft:tall_grass:half=lower");
            Object tallUpper = parse.invoke(null, "minecraft:tall_grass:half=upper");
            Object tallAny = parse.invoke(null, "minecraft:tall_grass");
            Object pink = parse.invoke(null, "merlins_inferno:pink_grass");
            Object pinkLower = parse.invoke(null, "merlins_inferno:tall_pink_grass:half=lower");
            Object pinkUpper = parse.invoke(null, "merlins_inferno:tall_pink_grass:half=upper");

            // Respect a shaderpack that already provides its own mappings.
            boolean hasPink = contains(blocks, pink);
            boolean hasLower = contains(blocks, pinkLower);
            boolean hasUpper = contains(blocks, pinkUpper);
            for (var group : blocks.int2ObjectEntrySet()) {
                List<?> vanilla = group.getValue();
                List<Object> additions = new ArrayList<>(3);
                if (!hasPink && vanilla.contains(shortGrass)) additions.add(pink);
                if (!hasLower && (vanilla.contains(tallLower) || vanilla.contains(tallAny))) additions.add(pinkLower);
                if (!hasUpper && (vanilla.contains(tallUpper) || vanilla.contains(tallAny))) additions.add(pinkUpper);
                if (!additions.isEmpty()) {
                    List<Object> merged = new ArrayList<>(vanilla);
                    merged.addAll(additions);
                    blocks.put(group.getIntKey(), List.copyOf(merged));
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not alias Cascade grass in Iris shader materials", e);
        }
    }

    private static boolean contains(Int2ObjectLinkedOpenHashMap<List<?>> blocks, Object wanted) {
        for (List<?> group : blocks.values()) {
            if (group.contains(wanted)) return true;
        }
        return false;
    }
}
