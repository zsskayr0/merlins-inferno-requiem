package dev.zsskayr.merlins_inferno.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * Oblivion: a Nether with no ceiling (Nether Wastes, Soul Sand Valley, Crimson, Warped, Basalt Deltas and the
 * Black Forest). Travel works like any dimension's: same coordinates (scale 1), the nearest existing arrival portal is
 * reused, and when there is none one is raised at the spot. That portal is Andras' giant sword, the way it hangs
 * ({@code structure/oblivion_sword.nbt}, built by {@code tools/build_oblivion_sword.py}): the portal is the blade, its
 * tip on the ground, with the guard, grip and pommel towering above. Arrivals simply stand on the ground in front.
 */
public final class OblivionDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "oblivion"));
    public static final ResourceLocation SWORD_TEMPLATE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "oblivion_sword");

    /** An existing portal this close (horizontally) to the target is reused instead of building a new sword. */
    private static final int REUSE_RADIUS = 128;
    private static final int MIN_GROUND_Y = 36;
    private static final int MAX_GROUND_Y = 205;
    /** Template-local layout, from the tool's printout: ground level is layer 1, portal's lower-left interior at (4, 1, 1), arrival at (6, 1, 3). */
    private static final BlockPos GROUND_TO_ORIGIN = new BlockPos(-4, -1, -1);
    private static final BlockPos ARRIVAL_FROM_PORTAL = new BlockPos(2, 0, 2);

    private OblivionDimension() {
    }

    /** The block a player arrives on, beside the (found or freshly built) portal nearest to {@code target}. */
    public static BlockPos ensureArrival(ServerLevel level, BlockPos target) {
        OblivionPortals data = OblivionPortals.get(level);
        BlockPos best = null;
        double bestDist = (double) REUSE_RADIUS * REUSE_RADIUS;
        for (BlockPos portal : new java.util.ArrayList<>(data.all())) {
            level.getChunk(portal);
            if (!level.getBlockState(portal).is(ModBlocks.OBLIVION_PORTAL.get())) {
                data.remove(portal);
                continue;
            }
            double dx = portal.getX() - target.getX(), dz = portal.getZ() - target.getZ();
            if (dx * dx + dz * dz <= bestDist) {
                bestDist = dx * dx + dz * dz;
                best = portal;
            }
        }
        if (best == null) {
            best = buildSword(level, target, data);
        }
        return best.offset(ARRIVAL_FROM_PORTAL);
    }

    private static BlockPos buildSword(ServerLevel level, BlockPos target, OblivionPortals data) {
        level.getChunk(target);
        int ground = Math.max(MIN_GROUND_Y, Math.min(MAX_GROUND_Y, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ())));
        BlockPos origin = new BlockPos(target.getX(), ground, target.getZ()).offset(GROUND_TO_ORIGIN);
        StructureTemplate template = level.getServer().getStructureManager().getOrCreate(SWORD_TEMPLATE);
        // the sword always stands on solid ground: fill any gap under its footprint (rift edges, cave roofs)
        net.minecraft.core.Vec3i size = template.getSize();
        for (int x = 0; x < size.getX(); x++) {
            for (int z = 0; z < size.getZ(); z++) {
                for (int dy = 1; dy <= 12; dy++) {
                    BlockPos under = origin.offset(x, -dy, z);
                    if (!level.getBlockState(under).canBeReplaced()) {
                        break;
                    }
                    level.setBlock(under, net.minecraft.world.level.block.Blocks.BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        template.placeInWorld(level, origin, origin, new StructurePlaceSettings().setIgnoreEntities(true), level.getRandom(), Block.UPDATE_CLIENTS);
        BlockPos portal = origin.offset(4, 1, 1);
        OblivionPortalShape.findEmptyPortalShape(level, portal, Direction.Axis.X).ifPresent(OblivionPortalShape::createPortalBlocks);
        data.add(portal);
        return portal;
    }
}
