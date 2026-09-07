package dev.zsskayr.merlins_inferno.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Custom tool tiers, shaped the same way as vanilla {@link net.minecraft.world.item.Tiers}.
 * <p>
 * Design (per project decision): {@code IRONWOOD} mines at Diamond's level (same
 * {@code BlockTags.INCORRECT_FOR_*} tag, so it can harvest obsidian/ancient debris) and matches
 * Diamond's mining speed, but its attack damage bonus matches Iron - the trade-off for sitting
 * "between" the two is a durability pool well past Diamond's.
 */
public final class ModTiers {
    public static final Tier IRONWOOD = new Tier() {
        @Override
        public int getUses() {
            return 2200; // longer-lasting than Diamond's 1561
        }

        @Override
        public float getSpeed() {
            return 8.0F; // matches Diamond - tracks its mining level
        }

        @Override
        public float getAttackDamageBonus() {
            return 2.0F; // matches Iron, by design
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_DIAMOND_TOOL; // same mining level as Diamond
        }

        @Override
        public int getEnchantmentValue() {
            return 14; // matches Iron
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.IRONWOOD_BAR.get());
        }
    };

    private ModTiers() {
    }
}
