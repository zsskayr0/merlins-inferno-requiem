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
 * Design (per project decision): {@code ROWANWOOD} mines at Diamond's level (same
 * {@code BlockTags.INCORRECT_FOR_*} tag, so it can harvest obsidian/ancient debris) and matches
 * Diamond's mining speed, but its attack damage bonus matches Iron - the trade-off for sitting
 * "between" the two is a durability pool well past Diamond's.
 */
public final class ModTiers {
    public static final Tier ROWANWOOD = new Tier() {
        @Override
        public int getUses() {
            return 1751; // longer-lasting than Diamond's 1561
        }

        @Override
        public float getSpeed() {
            return 8.0F; // matches Diamond
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
            return 25; // matches Gold
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.ROWANWOOD_BAR.get());
        }
    };

    /**
     * Design (per project decision): {@code DEMONITE} mirrors Netherite's stats exactly (uses,
     * speed, attack damage bonus, mining level, repair item) - its one intrinsic edge is a higher
     * enchantment value. The material's signature trait, +20% damage against "humans" (villagers,
     * pillagers, witches, and their variations), is not a tier field - see
     * {@code dev.zsskayr.merlins_inferno.event.DemoniteCombatHandler}.
     */
    public static final Tier DEMONITE = new Tier() {
        @Override
        public int getUses() {
            return 2031; // matches Netherite
        }

        @Override
        public float getSpeed() {
            return 9.0F; // matches Netherite
        }

        @Override
        public float getAttackDamageBonus() {
            return 4.0F; // matches Netherite
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; // same mining level as Netherite
        }

        @Override
        public int getEnchantmentValue() {
            return 22; // higher than Netherite's 15, by design
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.DEMONITE_BAR.get());
        }
    };

    private ModTiers() {
    }
}
