package dev.zsskayr.merlins_inferno.worldgen.structure;

import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.entity.SacredCultistEntity;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;

/**
 * Who sits where in the Sacred Church. The congregation fills the middle pews - two worshippers at the end seats of each
 * pew beside the aisle - and once a player has reached Circle 2 one more row of pews on each side fills up too. The church
 * spawns the Circle 1 congregation when it is generated; its altar ({@code SacredAltarBlockEntity}) refills empty seats
 * every {@link #REFILL_INTERVAL_TICKS}, so the cultists are a renewable (if slow) source of their drops.
 * <p>
 * Rows and seats are template-relative: the pews are spruce stairs every other block along the nave (x = 16..32), three
 * seats wide on each side of the aisle (z = 16..18 and 26..28).
 */
public final class ChurchCongregation {
    /** Template x of the pew rows that make up the Circle 1 congregation: the middle three of the nave's nine. */
    private static final int[] BASE_ROWS = {22, 24, 26};
    /** The row added on each side from Circle 2 on (the next one toward the altar). */
    private static final int[] EXTRA_ROWS = {28};
    /** The two end seats of each inner pew (the middle seat stays empty). */
    private static final int[] PEW_END_Z = {16, 18, 26, 28};

    /** Facing east, toward the altar. */
    public static final float FACE_ALTAR = 270.0F;
    /** Seven in-game days between refills. */
    public static final long REFILL_INTERVAL_TICKS = 7L * 24000L;

    /** A stair's low step sits half a block up, on its front (east) half; feet go there. */
    private static final double SEAT_STEP_HEIGHT = 0.5, SEAT_X_OFFSET = 0.8;
    /** How far from the nave they roam by day - reaching the churchyard around it. */
    public static final int ROAM_RADIUS = 30;

    private ChurchCongregation() {
    }

    public static List<BlockPos> baseSeats(List<BlockPos> pewSeats, BlockPos templateOrigin) {
        return seats(pewSeats, templateOrigin, BASE_ROWS);
    }

    public static List<BlockPos> extraSeats(List<BlockPos> pewSeats, BlockPos templateOrigin) {
        return seats(pewSeats, templateOrigin, EXTRA_ROWS);
    }

    private static List<BlockPos> seats(List<BlockPos> pewSeats, BlockPos origin, int[] rows) {
        return pewSeats.stream()
                .filter(seat -> contains(rows, seat.getX() - origin.getX()) && contains(PEW_END_Z, seat.getZ() - origin.getZ()))
                .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ))
                .toList();
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    /** Where a worshipper's feet go for a given pew seat. */
    public static Vec3 postFor(BlockPos seat) {
        return new Vec3(seat.getX() + SEAT_X_OFFSET, seat.getY() + SEAT_STEP_HEIGHT, seat.getZ() + 0.5);
    }

    /** Puts a fresh cultist in the given seat. */
    public static void spawn(ServerLevelAccessor level, BlockPos seat, BlockPos churchCenter) {
        SacredCultistEntity cultist = ModEntityTypes.SACRED_CULTIST.get().create(level.getLevel());
        if (cultist == null) {
            return;
        }
        Vec3 post = postFor(seat);
        cultist.moveTo(post.x, post.y, post.z, FACE_ALTAR, 0.0F); // in the pew, facing the altar
        cultist.setPersistenceRequired();
        cultist.restrictTo(churchCenter, ROAM_RADIUS);
        cultist.setServicePost(post, FACE_ALTAR);
        cultist.finalizeSpawn(level, level.getCurrentDifficultyAt(seat), MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(cultist);
    }
}
