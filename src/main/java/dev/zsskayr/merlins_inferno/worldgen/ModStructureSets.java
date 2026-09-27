package dev.zsskayr.merlins_inferno.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * {@link ResourceKey} constants for this mod's {@code StructureSet}s (the spacing/rarity rules
 * for a {@code Structure}). Actual data built in
 * {@code dev.zsskayr.merlins_inferno.datagen.worldgen.ModStructureProvider}.
 */
public final class ModStructureSets {
    public static final ResourceKey<StructureSet> ROWANWOOD_TREE = key("rowanwood_tree");
    public static final ResourceKey<StructureSet> DRUID_SANCTUARY = key("druid_sanctuary");
    public static final ResourceKey<StructureSet> SACRED_CHURCH = key("sacred_church");

    private ModStructureSets() {
    }

    private static ResourceKey<StructureSet> key(String name) {
        return ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, name));
    }
}
