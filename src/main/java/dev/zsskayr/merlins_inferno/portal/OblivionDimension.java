package dev.zsskayr.merlins_inferno.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * Oblivion: an empty, lightless void dimension. Its only built thing is the arrival hub - a floating gothic
 * chapel ({@code structure/oblivion_hub.nbt}, built by {@code tools/build_oblivion_hub.py}) with the return portal
 * inside, facing north - raised on demand by {@link #ensureHub} the first time somebody comes through.
 */
public final class OblivionDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "oblivion"));
    public static final ResourceLocation HUB_TEMPLATE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "oblivion_hub");

    /** Template-local layout, from the tool's printout: the island's centre is (30, 30), its surface y = 28. */
    private static final BlockPos ORIGIN = new BlockPos(-30, 40, -30);
    /** Lower-left interior block of the return portal (the frame is a Void Block ring around a 3x4 opening in the X-Y plane). */
    private static final BlockPos PORTAL = ORIGIN.offset(29, 29, 33);
    /** Where arrivals stand: in front of (north of) the portal, facing north, out of the doorway's way. */
    public static final BlockPos ARRIVAL = ORIGIN.offset(30, 29, 29);

    /** Horizontal radius around the hub's centre (the world origin) where nothing spawns naturally, so arrivals are safe. */
    private static final int HUB_SAFE_RADIUS = 40;

    private OblivionDimension() {
    }

    public static boolean isNearHub(double x, double z) {
        return x * x + z * z < (double) HUB_SAFE_RADIUS * HUB_SAFE_RADIUS;
    }

    /** Raises the hub if its portal is not there (first arrival, or somebody broke it). */
    public static void ensureHub(ServerLevel level) {
        if (level.getBlockState(PORTAL).is(ModBlocks.OBLIVION_PORTAL.get())) {
            return;
        }
        StructureTemplate template = level.getServer().getStructureManager().getOrCreate(HUB_TEMPLATE);
        template.placeInWorld(level, ORIGIN, ORIGIN, new StructurePlaceSettings().setIgnoreEntities(true), level.getRandom(), Block.UPDATE_CLIENTS);
        OblivionPortalShape.findEmptyPortalShape(level, PORTAL, Direction.Axis.X).ifPresent(OblivionPortalShape::createPortalBlocks);
    }
}
