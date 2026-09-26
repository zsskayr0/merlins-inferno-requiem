package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;

import dev.zsskayr.merlins_inferno.registry.ModEnchantments;

/**
 * Rowanwood's log - "bem parrudo": {@link #TOUGHNESS_MULTIPLIER_WITHOUT_ENCHANT} makes it twice as
 * slow to mine as its already-Obsidian-matching base hardness (see {@code ModBlocks}) unless the
 * axe has "Toque do Druida", which brings it back down to the normal (Obsidian-tier) time. This is
 * purely about break TIME - there's no tool-tier requirement at all (any axe eventually gets
 * through it), matching the design doc's "é sobre tempo de quebra, não tool tier mínimo".
 * <p>
 * The other half of the enchant requirement - that it only drops a plain Ashwood log instead of
 * Rowanwood without the enchant - is a loot table condition instead (see
 * {@code data/merlins_inferno/loot_table/blocks/rowanwood_log.json}), the same way vanilla gates
 * ore self-drops behind Silk Touch.
 */
public class RowanwoodLogBlock extends RotatedPillarBlock {
    private static final float TOUGHNESS_MULTIPLIER_WITHOUT_ENCHANT = 2.0F;

    public RowanwoodLogBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness == -1.0F) {
            return 0.0F;
        }
        boolean hasEnchant = ModEnchantments.getDruidsTouchLevel(player.level(), player.getMainHandItem()) > 0;
        if (!hasEnchant) {
            hardness *= TOUGHNESS_MULTIPLIER_WITHOUT_ENCHANT;
        }
        int divisor = EventHooks.doPlayerHarvestCheck(player, state, level, pos) ? 30 : 100;
        return player.getDigSpeed(state, pos) / hardness / divisor;
    }
}
