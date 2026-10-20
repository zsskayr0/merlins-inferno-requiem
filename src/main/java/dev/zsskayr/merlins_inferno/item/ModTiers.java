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
            return 22; // matches Gold's tool enchantability (design doc)
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.ROWANWOOD_BAR.get());
        }
    };

    /**
     * Design (per project decision): {@code DEMONBLOOD} mirrors Netherite's stats exactly (uses,
     * speed, attack damage bonus, mining level, repair item) - its one intrinsic edge is a higher
     * enchantment value. The material's signature trait, +10% damage against "humans" (villagers,
     * pillagers, witches, and their variations), is not a tier field - see
     * {@code dev.zsskayr.merlins_inferno.event.DemonbloodCombatHandler}.
     */
    public static final Tier DEMONBLOOD = new Tier() {
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
            return Ingredient.of(ModItems.DEMONBLOOD_BAR.get());
        }
    };

    /**
     * Design (per project decision): {@code SERAPHIUM} is the top of the 3-pillar trio (Rowanwood,
     * Demonblood, Seraphium) - strictly above both on every tool stat, same mining level as
     * Demonblood/Netherite. Its signature trait (bonus damage vs. Undead, matching Smite's scaling)
     * isn't a tier field - it comes from the Sanctified status effect a player builds up by
     * carrying Raw Lyrium (see {@code event.SanctifiedTickHandler}/{@code SanctifiedCombatHandler}),
     * not from the Seraphium tools themselves.
     */
    public static final Tier SERAPHIUM = new Tier() {
        @Override
        public int getUses() {
            return 2500; // above Demonblood's 2031
        }

        @Override
        public float getSpeed() {
            return 10.0F; // above Demonblood's 9.0
        }

        @Override
        public float getAttackDamageBonus() {
            return 5.0F; // above Demonblood's 4.0
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; // same mining level as Demonblood/Netherite
        }

        @Override
        public int getEnchantmentValue() {
            return 28; // above Demonblood's 22
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.SERAPHIUM_BAR.get());
        }
    };

    /**
     * Design (per project decision): the Hollow Relic hits like a Diamond sword but has a single use - it only
     * exists to be consumed in the legendary swords' recipes. No repair, no mining speed worth speaking of.
     */
    public static final Tier HOLLOW_RELIC = legendary(1, 3.0F, 10);

    /**
     * The Circle 1 legendary swords - Eden's Thorn (Druidic), Debt Collector (Infernal) and Vigil's Sorrow (Angelical), one per
     * route and all equally strong. The mythic tier above them is not craftable (see the weapon-tier rules).
     */
    public static final Tier LEGENDARY = legendary(3500, 7.0F, 30);

    /** The Dark Coven's tier: mythic, above {@link #LEGENDARY} in damage, durability and enchantability. Never craftable. */
    public static final Tier MYTHIC = legendary(5000, 10.0F, 35);

    private static Tier legendary(int uses, float attackBonus, int enchantability) {
        return new Tier() {
            @Override
            public int getUses() {
                return uses;
            }

            @Override
            public float getSpeed() {
                return 9.0F;
            }

            @Override
            public float getAttackDamageBonus() {
                return attackBonus;
            }

            @Override
            public TagKey<Block> getIncorrectBlocksForDrops() {
                return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
            }

            @Override
            public int getEnchantmentValue() {
                return enchantability;
            }

            @Override
            public Ingredient getRepairIngredient() {
                return Ingredient.EMPTY;
            }
        };
    }

    /**
     * Design (per project decision): Circle 2's Ebony, the tier above Seraphium on every stat (the Infernal route's
     * Oblivion material). Its signature - Fire Resistance IV with the full armor set - is in
     * {@code event.EbonyArmorHandler}. The Dark Coven uses {@link #MYTHIC}; the Ebony Sword is the ordinary sword of this tier.
     */
    public static final Tier EBONY = new Tier() {
        @Override
        public int getUses() {
            return 3000; // above Seraphium's 2500
        }

        @Override
        public float getSpeed() {
            return 11.0F; // above Seraphium's 10.0
        }

        @Override
        public float getAttackDamageBonus() {
            return 6.0F; // above Seraphium's 5.0
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 25;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.EBONY_BAR.get());
        }
    };

    private ModTiers() {
    }
}
