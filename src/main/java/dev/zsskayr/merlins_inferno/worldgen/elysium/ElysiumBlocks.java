package dev.zsskayr.merlins_inferno.worldgen.elysium;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import dev.zsskayr.merlins_inferno.block.CloudSeaBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.Mat;

/**
 * The core's abstract {@link Mat}erials as Minecraft block states. Built once, lazily (mod blocks only exist after
 * registration), then a plain array lookup by ordinal so the chunk generator's inner loop stays cheap.
 */
public final class ElysiumBlocks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile BlockState[] table;

    /** Cave vines: the plant (body) blocks and the bottom tip. Fully grown so they never keep growing down. */
    private static volatile BlockState vineBody, vineBodyBerries, vineHead, vineHeadBerries;

    private ElysiumBlocks() {
    }

    public static BlockState state(Mat mat) {
        BlockState[] t = table;
        if (t == null) {
            t = build();
        }
        return t[mat.ordinal()];
    }

    /** A block of the physical cloud layer, tint 0..2. */
    public static BlockState cloud(int tone) {
        return ModBlocks.CLOUD_SEA.get().defaultBlockState().setValue(CloudSeaBlock.TONE, tone);
    }

    public static BlockState water() {
        return Blocks.WATER.defaultBlockState();
    }

    /** A cave-vine block hanging under an island: {@code tip} is the lowest block of the strand. */
    public static BlockState caveVine(boolean tip, boolean berries) {
        if (table == null) {
            build();
        }
        if (tip) {
            return berries ? vineHeadBerries : vineHead;
        }
        return berries ? vineBodyBerries : vineBody;
    }

    private static synchronized BlockState[] build() {
        if (table != null) {
            return table;
        }
        vineBody = Blocks.CAVE_VINES_PLANT.defaultBlockState().setValue(CaveVines.BERRIES, false);
        vineBodyBerries = Blocks.CAVE_VINES_PLANT.defaultBlockState().setValue(CaveVines.BERRIES, true);
        vineHead = Blocks.CAVE_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, 25).setValue(CaveVines.BERRIES, false);
        vineHeadBerries = Blocks.CAVE_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, 25).setValue(CaveVines.BERRIES, true);

        Mat[] mats = Mat.values();
        BlockState[] t = new BlockState[mats.length];
        for (Mat m : mats) {
            t[m.ordinal()] = map(m);
        }
        table = t;
        return t;
    }

    private static BlockState map(Mat m) {
        switch (m) {
            case AIR: return Blocks.AIR.defaultBlockState();
            // stone family
            case ARGANITE: return ModBlocks.ARGANITE.get().defaultBlockState();
            case QUARTZITE: return ModBlocks.QUARTZITE.get().defaultBlockState();
            case CALCITE: return Blocks.CALCITE.defaultBlockState();
            case STONE: return Blocks.STONE.defaultBlockState();
            case ANDESITE: return Blocks.ANDESITE.defaultBlockState();
            case DIORITE: return Blocks.DIORITE.defaultBlockState();
            case TUFF: return Blocks.TUFF.defaultBlockState();
            case SMOOTH_BASALT: return Blocks.SMOOTH_BASALT.defaultBlockState();
            // soils
            case GRASS_BLOCK: return Blocks.GRASS_BLOCK.defaultBlockState();
            case DIRT: return Blocks.DIRT.defaultBlockState();
            case COARSE_DIRT: return Blocks.COARSE_DIRT.defaultBlockState();
            case ROOTED_DIRT: return Blocks.ROOTED_DIRT.defaultBlockState();
            case PODZOL: return Blocks.PODZOL.defaultBlockState();
            case MOSS_BLOCK: return Blocks.MOSS_BLOCK.defaultBlockState();
            case MUD: return Blocks.MUD.defaultBlockState();
            case CLAY: return Blocks.CLAY.defaultBlockState();
            case GRAVEL: return Blocks.GRAVEL.defaultBlockState();
            case SAND: return Blocks.SAND.defaultBlockState();
            case SANDSTONE: return Blocks.SANDSTONE.defaultBlockState();
            case TERRACOTTA: return Blocks.TERRACOTTA.defaultBlockState();
            case ORANGE_TERRACOTTA: return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            case BROWN_TERRACOTTA: return Blocks.BROWN_TERRACOTTA.defaultBlockState();
            case WHITE_TERRACOTTA: return Blocks.WHITE_TERRACOTTA.defaultBlockState();
            case LIGHT_GRAY_TERRACOTTA: return Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState();
            // cold
            case SNOW_BLOCK: return Blocks.SNOW_BLOCK.defaultBlockState();
            case POWDER_SNOW: return Blocks.POWDER_SNOW.defaultBlockState();
            case ICE: return Blocks.ICE.defaultBlockState();
            case PACKED_ICE: return Blocks.PACKED_ICE.defaultBlockState();
            case BLUE_ICE: return Blocks.BLUE_ICE.defaultBlockState();
            case SNOW_LAYER: return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1);
            // fluids
            case WATER: return Blocks.WATER.defaultBlockState();
            // underside dressing
            case HANGING_ROOTS: return Blocks.HANGING_ROOTS.defaultBlockState();
            case CAVE_VINES: return vineHead;
            case GLOW_LICHEN_CEIL: return Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.UP), true);
            case MOSS_CARPET: return Blocks.MOSS_CARPET.defaultBlockState();
            // misc
            case MOSSY_COBBLESTONE: return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case COBBLESTONE: return Blocks.COBBLESTONE.defaultBlockState();
            case QUARTZ_BLOCK: return Blocks.QUARTZ_BLOCK.defaultBlockState();
            case AMETHYST_BLOCK: return Blocks.AMETHYST_BLOCK.defaultBlockState();
            default:
                // A Mat added to the core after this table was written: keep generating, but say so loudly.
                LOGGER.error("ElysiumBlocks has no block for Mat.{}; using stone. Add it to ElysiumBlocks.map", m);
                return Blocks.STONE.defaultBlockState();
        }
    }
}
