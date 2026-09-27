package dev.zsskayr.merlins_inferno.worldgen.structure;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The Sacred Church's Great Bell: a vanilla bell (the heart, at {@code center}) inside a five-block-tall shell of
 * oxidized copper - a crown, a shouldered body and a flared rim - hung from the vault on a chain.
 * Elias tolls it ({@link #ring}) and its breaking ({@link #shatter}) ends his vigil's first phase.
 */
public final class GreatBell {
    private static final BlockState BODY = Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState();
    private static final BlockState RIM = Blocks.WAXED_WEATHERED_COPPER.defaultBlockState();
    private static final BlockState HEART = Blocks.BELL.defaultBlockState()
            .setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, Direction.NORTH);

    /** Offsets from the bell block: {dx, dy, dz, 0 = body / 1 = rim}. The bell block itself is (0, 0, 0). */
    private static final int[][] SHELL = buildShell();

    private GreatBell() {
    }

    private static int[][] buildShell() {
        java.util.List<int[]> shell = new java.util.ArrayList<>();
        shell.add(new int[] {0, 2, 0, 0});                                 // crown
        for (int[] d : new int[][] {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            shell.add(new int[] {d[0], 1, d[1], 0});                       // shoulders, closed over the bell
        }
        for (int dy : new int[] {0, -1}) {                                 // body: a 3x3 ring around the bell's column
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        shell.add(new int[] {dx, dy, dz, 0});
                    }
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {                                 // rim: a 5x5 outline, corners cut
            for (int dz = -2; dz <= 2; dz++) {
                boolean outline = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                if (outline && !corner) {
                    shell.add(new int[] {dx, -2, dz, 1});
                }
            }
        }
        return shell.toArray(new int[0][]);
    }

    /** Height of the chain's lowest link above the bell block (the crown is at +2). */
    public static final int CHAIN_START = 3;

    /**
     * Builds the bell. During world generation pass the chunk's box (blocks outside it are skipped) and everything is
     * placed; at runtime (a new vigil) pass {@code null} and only empty spots are refilled.
     */
    public static void build(LevelAccessor level, @Nullable BoundingBox box, BlockPos center) {
        place(level, box, center, HEART);
        for (int[] s : SHELL) {
            place(level, box, center.offset(s[0], s[1], s[2]), s[3] == 0 ? BODY : RIM);
        }
    }

    private static void place(LevelAccessor level, @Nullable BoundingBox box, BlockPos p, BlockState state) {
        if (box != null) {
            if (box.isInside(p)) {
                level.setBlock(p, state, Block.UPDATE_CLIENTS);
            }
        } else if (level.getBlockState(p).isAir()) {
            level.setBlock(p, state, Block.UPDATE_ALL);
        }
    }

    public static boolean isIntact(LevelAccessor level, BlockPos center) {
        return level.getBlockState(center).is(Blocks.BELL);
    }

    /** One toll: the bell swings and its note rolls out over the whole church. */
    public static void ring(ServerLevel level, BlockPos center) {
        level.blockEvent(center, Blocks.BELL, 1, Direction.NORTH.get3DDataValue());
        level.playSound(null, center, SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 8.0F, 0.5F);
        level.playSound(null, center, SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 8.0F, 0.5F);
        level.sendParticles(ParticleTypes.NOTE, center.getX() + 0.5, center.getY() - 2.5, center.getZ() + 0.5, 20, 2.5, 0.5, 2.5, 1.0);
    }

    /** Breaks the whole bell - heart and shell - with the usual crash of each block, dropping nothing. */
    public static void shatter(ServerLevel level, BlockPos center) {
        if (level.getBlockState(center).is(Blocks.BELL)) {
            level.destroyBlock(center, false);
        }
        for (int[] s : SHELL) {
            BlockPos p = center.offset(s[0], s[1], s[2]);
            BlockState state = level.getBlockState(p);
            if (state.is(BODY.getBlock()) || state.is(RIM.getBlock())) {
                level.destroyBlock(p, false);
            }
        }
    }
}
