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
         * vindicators, evokers, illusioners, ravagers, witches) - what Demonite's +20% damage
         * bonus ({@code DemoniteCombatHandler}) and the "Evil" enchantment both target.
         */
        public static final TagKey<EntityType<?>> HUMANS = TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "humans"));

        private EntityTypes() {
        }
    }

    public static final class Items {
        /** Every Demonite tool/weapon - grants the intrinsic +20% damage bonus vs {@link EntityTypes#HUMANS}. */
        public static final TagKey<Item> DEMONITE_TOOLS = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "demonite_tools"));

        private Items() {
        }
    }

    private ModTags() {
    }
}
