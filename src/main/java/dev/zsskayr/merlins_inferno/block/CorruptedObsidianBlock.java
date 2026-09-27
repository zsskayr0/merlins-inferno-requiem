package dev.zsskayr.merlins_inferno.block;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;

/**
 * Corrupted Obsidian: Crying Obsidian's demonic cousin, and Andras' Citadel's portal frame material (see
 * {@code worldgen.structure.InfernalCitadelPiece}). Per design:
 * <ul>
 *     <li><b>Circle 1:</b> outright unbreakable in survival, whatever the tool - like Bedrock.</li>
 *     <li><b>Circle 2:</b> breakable, but at three times Obsidian's own hardness ({@link #HARDNESS}) and gated
 *     behind a tool tier above Netherite (see {@code data/minecraft/tags/block/incorrect_for_netherite_tool.json})
 *     - no such tool exists yet (that's future content), so in practice it stays impractical to mine even once
 *     a player reaches Circle 2, until those tools are added.</li>
 * </ul>
 * The Circle check is per-player, not a world/block property, so it can't be plain block hardness - hence the
 * {@link #getDestroyProgress} override rather than a {@code -1.0F} hardness like {@code SacredAltarBlock}'s.
 */
public class CorruptedObsidianBlock extends Block {
    /** Three times vanilla Obsidian's own hardness (50.0). */
    public static final float HARDNESS = 150.0F;

    public CorruptedObsidianBlock(Properties properties) {
        super(properties);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, net.minecraft.core.BlockPos pos) {
        if (!ProgressionHelper.hasReached(player, ProgressionHelper.SECOND_CIRCLE)) {
            return 0.0F; // Circle 1: never progresses, exactly like Bedrock
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}
