package dev.zsskayr.merlins_inferno.worldgen.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import dev.zsskayr.merlins_inferno.registry.ModStructureTypes;

/**
 * Andras' Ancient Battlefield: a floating island in the Nether (see {@link AncientBattlefieldPiece}) that always
 * sits in the lava sea, surrounded by lava, with only its top six layers showing above the surface.
 * <p>
 * Nothing here hard-codes the Nether's numbers, so it keeps working with mods that reshape it: the lava line comes
 * from the generator's own {@code getSeaLevel()}, lava is recognised by the {@code minecraft:lava} fluid tag (modded
 * lava counts), and the sea is read from the generator's own base noise ({@code getBaseColumn}), which includes
 * whatever noise settings a datapack or mod swapped in. Biomes are whatever carries {@code #minecraft:is_nether}.
 * <p>
 * Site search: the vanilla placement picks one start chunk per region, and a fully lava-ringed 100x100 patch is
 * not something every start chunk has under it. So the structure tries the start chunk first, then up to
 * {@link #SEARCH_RINGS} rings of candidate centres around it, nearest first, and takes the first that passes
 * {@link #isLavaSite}. The island may end up a few chunks away from its start chunk; none of that is visible to
 * {@code /locate}, which reports where the piece really is.
 */
public class AncientBattlefieldStructure extends Structure {
    public static final MapCodec<AncientBattlefieldStructure> CODEC = simpleCodec(AncientBattlefieldStructure::new);

    /** Candidate centres are this many blocks apart, in rings around the start chunk's centre. */
    private static final int SEARCH_STEP = 24;
    private static final int SEARCH_RINGS = 2;
    /** Lava must be found this far out from the centre - past the island's own ~41-block radius, so it is ringed. */
    private static final int SITE_RANGE = 48;
    private static final int SITE_STEP = 12;
    /** Within this distance of the centre (the part the island sits on) every sample must be lava... */
    private static final int STRICT_RANGE = 24;
    /** ...and out in the ring around it, this many dry samples (of 81) are forgiven. */
    private static final int MAX_DRY_OUTER = 8;
    /** Clear air needed above the middle columns for the sword (its tip is ~42 layers over the lava line). */
    private static final int HEADROOM_LOW = 8, HEADROOM_HIGH = 44;

    /** How far above the generator's sea level the lava surface is searched for. */
    private static final int MAX_LAVA_RISE = 24;

    private static final List<int[]> CANDIDATES = candidates();

    public AncientBattlefieldStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    /** {dx, dz} offsets in blocks, the start chunk's own centre first and then ring by ring outwards. */
    private static List<int[]> candidates() {
        List<int[]> list = new ArrayList<>();
        for (int ring = 0; ring <= SEARCH_RINGS; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                        list.add(new int[] {dx * SEARCH_STEP, dz * SEARCH_STEP});
                    }
                }
            }
        }
        return list;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        StructureTemplateManager templateManager = context.structureTemplateManager();
        Vec3i size = templateManager.getOrCreate(AncientBattlefieldPiece.TEMPLATE).getSize();

        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        RandomState randomState = context.randomState();
        int seaTopY = generator.getSeaLevel() - 1;

        ChunkPos chunkPos = context.chunkPos();
        for (int[] offset : CANDIDATES) {
            int cx = chunkPos.getMiddleBlockX() + offset[0];
            int cz = chunkPos.getMiddleBlockZ() + offset[1];
            // The lava's real surface can sit above the generator's sea level (modded or datapack Nethers raise it),
            // so read it off the base noise instead of assuming it: the island must rise 6 layers over the actual lava.
            int lavaTopY = lavaSurface(generator.getBaseColumn(cx, cz, heightAccessor, randomState), seaTopY);
            if (lavaTopY < 0 || !isLavaSite(generator, heightAccessor, randomState, cx, cz, lavaTopY)) {
                continue;
            }
            int originY = lavaTopY - AncientBattlefieldPiece.LAVA_TOP_LOCAL_Y - 1; // one layer lower: 5 layers of rim show above the lava
            BlockPos origin = new BlockPos(cx - size.getX() / 2, originY, cz - size.getZ() / 2);
            return Optional.of(new Structure.GenerationStub(new BlockPos(cx, originY, cz),
                    builder -> builder.addPiece(new AncientBattlefieldPiece(templateManager, origin))));
        }
        return Optional.empty();
    }

    /**
     * True if the sea around (cx, cz) is lava all the way out to {@link #SITE_RANGE} (a few dry columns tolerated in
     * the outer ring) and the middle has the headroom for the sword. Cheap centre samples go first, so the many
     * hopeless candidates are rejected after a handful of noise columns.
     */
    private static boolean isLavaSite(ChunkGenerator generator, LevelHeightAccessor heightAccessor, RandomState randomState,
            int cx, int cz, int lavaTopY) {
        int dry = 0;
        // Inner samples first (they must all pass), then the outer ring.
        for (int pass = 0; pass < 2; pass++) {
            for (int dx = -SITE_RANGE; dx <= SITE_RANGE; dx += SITE_STEP) {
                for (int dz = -SITE_RANGE; dz <= SITE_RANGE; dz += SITE_STEP) {
                    boolean inner = Math.abs(dx) <= STRICT_RANGE && Math.abs(dz) <= STRICT_RANGE;
                    if (inner != (pass == 0)) {
                        continue;
                    }
                    NoiseColumn column = generator.getBaseColumn(cx + dx, cz + dz, heightAccessor, randomState);
                    if (!column.getBlock(lavaTopY).getFluidState().is(FluidTags.LAVA)) {
                        if (inner || ++dry > MAX_DRY_OUTER) {
                            return false;
                        }
                        continue;
                    }
                    if (inner && Math.abs(dx) <= SITE_STEP && Math.abs(dz) <= SITE_STEP
                            && (!isOpen(column.getBlock(lavaTopY + HEADROOM_LOW)) || !isOpen(column.getBlock(lavaTopY + HEADROOM_HIGH)))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Y of the topmost lava block of the lava column that starts at {@code seaTopY}, or -1 if there is no lava there. */
    private static int lavaSurface(NoiseColumn column, int seaTopY) {
        if (!column.getBlock(seaTopY).getFluidState().is(FluidTags.LAVA)) {
            return -1;
        }
        int y = seaTopY;
        while (y < seaTopY + MAX_LAVA_RISE && column.getBlock(y + 1).getFluidState().is(FluidTags.LAVA)) {
            y++;
        }
        return y;
    }

    private static boolean isOpen(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty();
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.ANCIENT_BATTLEFIELD.get();
    }
}
