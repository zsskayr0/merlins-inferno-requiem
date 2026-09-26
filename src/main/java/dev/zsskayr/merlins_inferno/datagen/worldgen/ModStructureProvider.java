package dev.zsskayr.merlins_inferno.datagen.worldgen;

import net.minecraft.core.HolderGetter;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import dev.zsskayr.merlins_inferno.worldgen.ModStructureSets;
import dev.zsskayr.merlins_inferno.worldgen.ModStructures;
import dev.zsskayr.merlins_inferno.worldgen.biome.ModBiomes;
import dev.zsskayr.merlins_inferno.worldgen.structure.DruidSanctuaryStructure;
import dev.zsskayr.merlins_inferno.worldgen.structure.RowanwoodTreeStructure;

/**
 * Rowanwood's landmark tree as a real {@code Structure} + {@code StructureSet} (see
 * {@code RowanwoodTreeStructure}'s javadoc for why it's a Structure and not a decoration Feature
 * like Ashwood). Restricted to Hallowed Grove only via the {@code StructureSettings}' biome
 * {@code HolderSet}.
 * <p>
 * Spacing (24 chunks) / separation (10 chunks) is deliberately loose - roughly one per
 * 24x24-chunk region on average, rarer than the old feature's "~1 in 32 chunks" since a structure
 * this size (48x25x39) reads better as a true landmark than something you trip over regularly.
 */
public final class ModStructureProvider {
    private ModStructureProvider() {
    }

    public static void bootstrapStructures(BootstrapContext<Structure> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        context.register(ModStructures.ROWANWOOD_TREE,
                new RowanwoodTreeStructure(new Structure.StructureSettings(HolderSet.direct(biomes.getOrThrow(ModBiomes.HALLOWED_GROVE)))));
        context.register(ModStructures.DRUID_SANCTUARY,
                new DruidSanctuaryStructure(new Structure.StructureSettings(HolderSet.direct(biomes.getOrThrow(ModBiomes.HALLOWED_GROVE)))));
    }

    public static void bootstrapStructureSets(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);

        Holder.Reference<StructureSet> rowanwoodTrees = context.register(ModStructureSets.ROWANWOOD_TREE,
                new StructureSet(structures.getOrThrow(ModStructures.ROWANWOOD_TREE),
                        new RandomSpreadStructurePlacement(24, 10, RandomSpreadType.LINEAR, 0x524F5754))); // salt: arbitrary, just needs to be unique across structure sets ("ROWT" in hex-ish)

        // The Druid's sanctuary (design doc, 4.3): far more common than a Rowanwood tree (one per
        // ~10x10 chunks of Grove, spacing 10 / separation 4) - it's the guaranteed Druid, meant to
        // be found on ordinary exploration - but never within 5 chunks of a Rowanwood tree, whose
        // 48x51 footprint would otherwise swallow it.
        context.register(ModStructureSets.DRUID_SANCTUARY,
                new StructureSet(structures.getOrThrow(ModStructures.DRUID_SANCTUARY),
                        new RandomSpreadStructurePlacement(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, 0x44525549,
                                Optional.of(new StructurePlacement.ExclusionZone(rowanwoodTrees, 5)), 10, 4, RandomSpreadType.LINEAR))); // salt: "DRUI"
    }
}
