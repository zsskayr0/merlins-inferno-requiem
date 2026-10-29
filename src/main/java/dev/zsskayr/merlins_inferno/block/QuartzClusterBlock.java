package dev.zsskayr.merlins_inferno.block;

import net.minecraft.world.level.block.AmethystClusterBlock;

/** A quartz crystal growing off Quartzite. Same shape as an amethyst cluster and, like the geode's, it never regrows. */
public class QuartzClusterBlock extends AmethystClusterBlock {
    public QuartzClusterBlock(float height, float aabbOffset, Properties properties) {
        super(height, aabbOffset, properties);
    }
}
