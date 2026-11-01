package dev.zsskayr.merlins_inferno.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * Which half of its 2x2 pair a Tiled Porcelain block is along an axis: an odd number of blocks above the start of its unbroken
 * line. Finding the start walks the line, and every block on the walk learns its own answer from it, so a line is walked
 * once however long it is. The answers are dropped whenever a block changes ({@code LevelRendererMixin}) or a chunk loads or
 * unloads, since either can move the start of a line.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class TiledLines {
    /** Only a guard against a line that never ends; real ones stop at the first gap. */
    private static final int MAX_RUN = 4096;
    private static final int MAX_ENTRIES = 1 << 18;

    /** Per axis: position to {@code version << 1 | answer}; an entry from an older version is stale and ignored. */
    @SuppressWarnings("unchecked")
    private static final Map<Long, Long>[] SECOND = new Map[] {
            new ConcurrentHashMap<Long, Long>(), new ConcurrentHashMap<Long, Long>(), new ConcurrentHashMap<Long, Long>() };
    private static final AtomicInteger VERSION = new AtomicInteger();

    private TiledLines() {
    }

    public static void invalidate() {
        VERSION.incrementAndGet();
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        invalidate();
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        invalidate();
    }

    /**
     * @param same whether the block at a position belongs to the line
     * @param cache false when the answer depends on more than the world (an AE2 facade is drawn from a side of its cable)
     */
    public static boolean secondHalf(BlockPos pos, Direction.Axis axis, Predicate<BlockPos> same, boolean cache) {
        Map<Long, Long> known = SECOND[axis.ordinal()];
        if (cache) {
            Long hit = known.get(pos.asLong());
            if (hit != null && (hit >> 1) == VERSION.get()) {
                return (hit & 1L) == 1L;
            }
        }
        int version = VERSION.get();
        Direction down = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
        BlockPos.MutableBlockPos at = pos.mutable();
        int above = 0;
        while (above < MAX_RUN) {
            at.move(down);
            if (!same.test(at)) {
                break;
            }
            above++;
        }
        if (cache && version == VERSION.get()) {
            if (known.size() >= MAX_ENTRIES) {
                known.clear(); // mostly stale entries by now
            }
            BlockPos.MutableBlockPos on = pos.mutable();
            for (int walked = 0; walked <= above; walked++) {
                known.put(on.asLong(), ((long) version << 1) | ((above - walked) & 1));
                on.move(down);
            }
        }
        return (above & 1) == 1;
    }
}
