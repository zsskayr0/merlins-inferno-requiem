package dev.zsskayr.merlins_inferno.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.block.OblivionPortalBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * Oblivion: an empty, lightless void dimension. Its only built thing is the arrival hub - a small obsidian platform
 * with a return portal - raised on demand by {@link #ensureHub} the first time somebody comes through.
 */
public final class OblivionDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "oblivion"));

    /** The hub's return portal: the lower-left interior block of a two-by-three portal in the X-Y plane, at z = 0. */
    private static final BlockPos PORTAL = new BlockPos(0, 64, 0);
    /** Where arrivals stand, a few blocks in front of the portal. */
    public static final BlockPos ARRIVAL = new BlockPos(1, 64, 3);

    private OblivionDimension() {
    }

    /** Builds the hub if its portal is not there (first arrival, or somebody broke it). */
    public static void ensureHub(ServerLevel level) {
        if (level.getBlockState(PORTAL).is(ModBlocks.OBLIVION_PORTAL.get())) {
            return;
        }
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        for (int x = -6; x <= 7; x++) {
            for (int z = -6; z <= 6; z++) {
                level.setBlock(new BlockPos(x, 63, z), obsidian, 3);
                for (int y = 64; y <= 70; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        for (int x = -1; x <= 2; x++) {
            for (int y = 63; y <= 67; y++) {
                boolean border = x == -1 || x == 2 || y == 63 || y == 67;
                if (border) {
                    level.setBlock(new BlockPos(x, y, 0), obsidian, 3);
                }
            }
        }
        BlockState portal = ModBlocks.OBLIVION_PORTAL.get().defaultBlockState().setValue(OblivionPortalBlock.AXIS, net.minecraft.core.Direction.Axis.X);
        for (int x = 0; x <= 1; x++) {
            for (int y = 64; y <= 66; y++) {
                level.setBlock(new BlockPos(x, y, 0), portal, 18);
            }
        }
    }
}
