package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * {@link TagKey} constants for tags this mod defines (data files live under
 * {@code data/merlins_inferno/tags/}).
 */
public final class ModTags {

    public static final class EntityTypes {
        /**
         * "Humans": villagers and their variants, plus the raid/illager cast (pillagers,
         * vindicators, evokers, illusioners, ravagers, witches) - what Demonblood's +10% damage
         * bonus ({@code DemonbloodCombatHandler}) and the "Evil" enchantment both target.
         */
        public static final TagKey<EntityType<?>> HUMANS = TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "humans"));

        /** Same roster the "Evil" enchantment discounts/bonuses against - see SeraphiumCombatHandler. */
        public static final TagKey<EntityType<?>> PASSIVE_MOBS = TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "passive_mobs"));
        public static final TagKey<EntityType<?>> NEUTRAL_MOBS = TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "neutral_mobs"));

        /**
         * Cross-mod compat hook for anything that adds "fairy" mobs - empty by default (this mod
         * doesn't know which fairy-adding mods, if any, are installed). Add entries with
         * {@code "required": false} to {@code data/merlins_inferno/tags/entity_type/fairies.json}
         * for specific mods as they're identified - see {@code data/merlins_inferno/loot_modifiers/fae_essence_from_fairies.json}.
         */
        public static final TagKey<EntityType<?>> FAIRIES = TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "fairies"));

        /** "Toque do Druida"'s bonus-loot roster for swords - see {@code event.DruidsTouchHandler}. */
        public static final TagKey<EntityType<?>> MAGICAL_MOBS = TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "magical_mobs"));

        /**
         * Mundane, entirely non-magical fauna (cows, sheep, wolves, ordinary Overworld critters) -
         * Rowanwood's intrinsic weak side, see {@code event.RowanwoodCombatHandler}. Deliberately
         * excludes anything with a magical/supernatural nature even if it looks "ordinary" at a
         * glance - Piglins (Nether-born), Iron/Snow Golems (magic constructs), the Warden, Endermen,
         * skeleton horses, and Striders are all left out for that reason. Disjoint from
         * {@link #MAGICAL_MOBS}, which gets the opposite treatment.
         */
        public static final TagKey<EntityType<?>> RIGID_MOBS = TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "rigid_mobs"));

        /**
         * Demons (the Imp so far; future demons join here): take extra damage from Seraphium
         * weapons - see {@code event.SeraphiumCombatHandler}.
         */
        public static final TagKey<EntityType<?>> DEMON = TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "demon"));

        private EntityTypes() {
        }
    }

    public static final class Items {
        /** Every Demonblood tool/weapon - grants the intrinsic +10% damage bonus vs {@link EntityTypes#HUMANS}. */
        public static final TagKey<Item> DEMONBLOOD_TOOLS = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "demonblood_tools"));

        /** Every Demonblood armor piece - grants the Demon's Fury trait (see DemonbloodCombatHandler). */
        public static final TagKey<Item> DEMONBLOOD_ARMOR = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "demonblood_armor"));

        /** Every Seraphium tool/weapon - see SeraphiumCombatHandler for what this grants. */
        public static final TagKey<Item> SERAPHIUM_TOOLS = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "seraphium_tools"));

        /** Every Rowanwood tool/weapon - see RowanwoodCombatHandler for what this grants. */
        public static final TagKey<Item> ROWANWOOD_TOOLS = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "rowanwood_tools"));

        /** Gold materials an Imp will steal from the ground or from a player's hand - see {@code entity.ai.ImpStealGoal}. */
        public static final TagKey<Item> IMP_STEALABLE = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "imp_stealable"));

        private Items() {
        }
    }

    private ModTags() {
    }
}
