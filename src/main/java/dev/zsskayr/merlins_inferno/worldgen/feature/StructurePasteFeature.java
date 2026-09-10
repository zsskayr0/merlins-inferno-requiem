package dev.zsskayr.merlins_inferno.worldgen.feature;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Pastes a random pre-made NBT structure from {@link StructurePasteConfiguration#structures()},
 * centered under the placement origin - no procedural trunk/foliage generation, no runtime
 * rotation (each orientation is its own baked file), and an optional block swap (see
 * {@link StructurePasteConfiguration#blockSwaps()}) to retexture the pasted blocks. Used for the
 * Ashwood tree (see {@code ModTreeProvider}), whose shapes are ported from a friend's
 * structure-based tree pack.
 * <p>
 * Ground handling is done here rather than in the placed feature's placement chain, because it
 * needs the actual structure footprint (which the placement modifiers don't know about): the
 * naive "just use the placement's heightmap column" approach left trees floating/sunken on any
 * slope under their (multi-block-wide) footprint, and did nothing to stop two trees rolling their
 * random spot inside each other's footprint - near-guaranteed given how wide these structures are
 * relative to a chunk. So this feature (a) scans height across the whole footprint and bails if
 * it's too uneven to sit flush, and (b) bails if any of that footprint already has log/leaves in
 * it (another tree, ours or vanilla's) rather than pasting straight through it.
 * <p>
 * His original feature paired this with a large auto-generated block-predicate for the same
 * overlap problem; that predicate itself was intentionally NOT ported (would need decompiling his
 * compiled-only Feature class, and checking hundreds of fixed block offsets per attempt is much
 * more expensive than the coarse scan below) - this is a from-scratch, cheaper equivalent.
 */
public class StructurePasteFeature extends Feature<StructurePasteConfiguration> {
    /** Sample every 3 blocks across the footprint instead of every block - plenty for trees this size, far cheaper. */
    private static final int SAMPLE_STEP = 3;
    // Reject the spot if the ground varies more than this across the footprint (steps/cliffs ->
    // floating trees). Loose enough for Rowanwood's 48x39 footprint to still find spots on rolling
    // terrain (a tight value tuned for Ashwood's ~13-wide footprint made a footprint this size
    // fail almost everywhere) - some minor embedding/floating at the edges of a tree this size is
    // much less noticeable than never spawning at all.
    private static final int MAX_FOOTPRINT_UNEVENNESS = 8;
    // Extra margin checked *beyond* the footprint itself in footprintHasExistingTree - without
    // this, "no overlap" still let canopies grow edge-to-edge/touching. Gives every tree (Ashwood
    // or the Rowanwood structure, which places its blocks first - see StructureSettings) some
    // breathing room instead of just not literally intersecting.
    private static final int TREE_CLEARANCE = 4;

    public StructurePasteFeature(Codec<StructurePasteConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<StructurePasteConfiguration> context) {
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        StructurePasteConfiguration config = context.config();

        ResourceLocation chosen = config.structures().get(random.nextInt(config.structures().size()));
        StructureTemplateManager templateManager = level.getLevel().getServer().getStructureManager();
        StructureTemplate template = templateManager.getOrCreate(chosen);

        // Center the footprint under the chosen point rather than pasting from a corner - matches
        // vanilla's own FossilFeature technique, and means "where did the game roll this tree" and
        // "where does it actually end up" stay close together.
        Vec3i size = template.getSize(Rotation.NONE);
        BlockPos corner = origin.offset(-size.getX() / 2, 0, -size.getZ() / 2);

        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;
        for (int x = 0; x <= size.getX(); x += SAMPLE_STEP) {
            for (int z = 0; z <= size.getZ(); z += SAMPLE_STEP) {
                int height = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, corner.getX() + x, corner.getZ() + z);
                minHeight = Math.min(minHeight, height);
                maxHeight = Math.max(maxHeight, height);
            }
        }
        if (maxHeight - minHeight > MAX_FOOTPRINT_UNEVENNESS) {
            return false;
        }

        BlockPos placeAt = new BlockPos(corner.getX(), minHeight, corner.getZ());
        if (footprintHasExistingTree(level, placeAt, size)) {
            return false;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        if (!config.blockSwaps().isEmpty()) {
            settings.addProcessor(new BlockPaletteSwapProcessor(config.blockSwaps()));
        }

        // Flag 4 (Block.UPDATE_INVISIBLE) matches vanilla FossilFeature - worldgen doesn't need
        // client update notifications, the chunk isn't sent to anyone yet.
        return template.placeInWorld(level, placeAt, placeAt, settings, random, 4);
    }

    /**
     * Coarse check: is there already a log/leaves block (any tree, ours or vanilla's) inside where
     * this one would go, or within {@link #TREE_CLEARANCE} blocks of its footprint's edge?
     */
    private static boolean footprintHasExistingTree(WorldGenLevel level, BlockPos corner, Vec3i size) {
        int sampleTop = Math.min(size.getY(), 8);
        for (int x = -TREE_CLEARANCE; x <= size.getX() + TREE_CLEARANCE; x += SAMPLE_STEP) {
            for (int z = -TREE_CLEARANCE; z <= size.getZ() + TREE_CLEARANCE; z += SAMPLE_STEP) {
                for (int y = 0; y <= sampleTop; y += SAMPLE_STEP) {
                    BlockState state = level.getBlockState(corner.offset(x, y, z));
                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
