package dev.zsskayr.merlins_inferno.worldgen.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModStructureTypes;

/**
 * Rowanwood's landmark tree, as a real vanilla {@code Structure} instead of a decoration
 * {@code Feature} - the only way to make it findable with {@code /locate structure}, which knows
 * nothing about {@code Feature}/{@code PlacedFeature}. This also solves the overlap problem the
 * old feature-based version needed its own runtime footprint scan for: a {@code StructureSet}'s
 * spacing/separation guarantees a minimum distance between instances by construction (see
 * {@code data/merlins_inferno/worldgen/structure_set/rowanwood_tree.json}), so there's no custom
 * collision check here at all.
 * <p>
 * One piece, one of the 4 pre-rotated NBT files chosen at random - see {@link RowanwoodTreePiece}.
 * Terrain adaptation is left at {@code NONE} (same as vanilla's single-piece surface structures
 * like the desert pyramid or igloo). Height is the MEAN of the piece's 4 actual footprint corners
 * (via {@link Structure#getMeanFirstOccupiedHeight}), not a single point - the footprint is 48-51
 * blocks wide, far bigger than vanilla single-piece structures (13-21 blocks), so anchoring off
 * just one corner or the chunk center left it visibly floating/buried on anything but dead-flat
 * ground. Averaging the corners still isn't a full terrain scan, but it's a large improvement for
 * a rare landmark and reuses a helper the vanilla base class already provides.
 * <p>
 * <b>Where the shape came from:</b> an oak ("Oak 5-1") from a "God Tree Pack" (creative-build-scale
 * trees - its own smallest tree was still 60-280 blocks per side, way past natural-spawn
 * territory; a weeping willow from the same pack was the first pick, swapped out after
 * playtesting for this rounder classic-canopy shape instead), downscaled ~2.67x
 * (128x99x137 -> 48x37x51, still far bigger than any Ashwood variant) by OR-ing each output cell
 * from its source region (log beats leaf beats air) so thin branches survive shrinking instead of
 * being sampled away. It was a classic {@code .schematic} (numeric block ids/data, not modern
 * block states); mapping was simple - id 17 (any data - a handful of stray voxels used other
 * wood types/orientations, folded in as plain log too) -> rowanwood_log, id 18 (leaves) -> baked
 * straight in as rowanwood_leaves with persistent=true so the pasted canopy never decays. Log
 * axis is uniformly "y" since the source data doesn't carry real per-branch orientation. A
 * one-off conversion, not something this codebase redoes at build time.
 */
public class RowanwoodTreeStructure extends Structure {
    public static final MapCodec<RowanwoodTreeStructure> CODEC = simpleCodec(RowanwoodTreeStructure::new);

    private static final String[] VARIANTS = {"", "_r1", "_r2", "_r3"};

    public RowanwoodTreeStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        // Not using the Structure.onTopOfChunkCenter() helper: it samples height at a single point
        // (the chunk center) and doesn't hand that computed position to the piece-building
        // callback - and our template's local origin is a corner (same convention
        // StructurePasteFeature used), not a center, so height needs sampling relative to the
        // corner this piece actually gets placed at, across its whole (large) footprint.
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMinBlockX();
        int z = chunkPos.getMinBlockZ();

        WorldgenRandom random = context.random();
        String variant = VARIANTS[random.nextInt(VARIANTS.length)];
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "rowanwood_tree/oak" + variant);
        StructureTemplateManager templateManager = context.structureTemplateManager();
        Vec3i size = templateManager.getOrCreate(location).getSize(Rotation.NONE);

        int y = getMeanFirstOccupiedHeight(context, x, size.getX(), z, size.getZ());
        BlockPos pos = new BlockPos(x, y, z);
        return Optional.of(new Structure.GenerationStub(pos, builder -> builder.addPiece(new RowanwoodTreePiece(templateManager, location, pos))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.ROWANWOOD_TREE.get();
    }
}
