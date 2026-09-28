package dev.zsskayr.merlins_inferno.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * {@link ResourceKey} constants for this mod's {@code Structure}s. Actual data built in
 * {@code dev.zsskayr.merlins_inferno.datagen.worldgen.ModStructureProvider}.
 */
public final class ModStructures {
    public static final ResourceKey<Structure> ROWANWOOD_TREE = key("rowanwood_tree");
    public static final ResourceKey<Structure> DRUID_SANCTUARY = key("druid_sanctuary");
    public static final ResourceKey<Structure> SACRED_CHURCH = key("sacred_church");
    public static final ResourceKey<Structure> ANCIENT_BATTLEFIELD = key("ancient_battlefield");

    private ModStructures() {
    }

    private static ResourceKey<Structure> key(String name) {
        return ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, name));
    }
}
