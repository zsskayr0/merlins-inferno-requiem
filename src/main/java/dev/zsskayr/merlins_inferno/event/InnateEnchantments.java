package dev.zsskayr.merlins_inferno.event;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;

import dev.zsskayr.merlins_inferno.item.DarkCovenItem;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * The Evil and Holy levels built into the gear of the Infernal and Angelical routes, and the damage each level adds - the numbers of
 * {@code data/merlins_inferno/enchantment/evil.json} and {@code holy.json} (with {@link HolyCombatHandler}'s tier for any other
 * {@link Monster}). The route's gear carries them without the enchantment, so a grindstone cannot take them off.
 * <p>
 * Evil: Demonblood I, Ebony II, the Dark Coven III (stages I-III), IV (IV-VI) and V (VII). Holy: Seraphium I, Luminite II.
 * (The Debt Collector and Vigil's Sorrow carry theirs in their own handlers.)
 */
public final class InnateEnchantments {
    private InnateEnchantments() {
    }

    public static int evilLevel(ItemStack stack) {
        if (stack.is(ModTags.Items.EBONY_TOOLS)) {
            return 2;
        }
        if (stack.is(ModTags.Items.DEMONBLOOD_TOOLS)) {
            return 1;
        }
        if (stack.getItem() instanceof DarkCovenItem coven) {
            int stage = coven.getStage();
            return stage >= 6 ? 5 : stage >= 3 ? 4 : 3;
        }
        return 0;
    }

    public static int holyLevel(ItemStack stack) {
        if (stack.is(ModTags.Items.LUMINITE_TOOLS)) {
            return 2;
        }
        if (stack.is(ModTags.Items.SERAPHIUM_TOOLS)) {
            return 1;
        }
        return 0;
    }

    /** What Evil of this level adds against the victim: +3 (+1.5 per level above the first) to the passive, +1.5 (+1) to the neutral. */
    public static float evilBonus(LivingEntity victim, int level) {
        if (level <= 0) {
            return 0.0F;
        }
        if (victim.getType().is(ModTags.EntityTypes.PASSIVE_MOBS)) {
            return 3.0F + 1.5F * (level - 1);
        }
        if (victim.getType().is(ModTags.EntityTypes.NEUTRAL_MOBS)) {
            return 1.5F + 1.0F * (level - 1);
        }
        return 0.0F;
    }

    /**
     * What Holy of this level adds (or takes, below zero): +3 (+1.5) to the demons, +2 (+1) to the Undead, -1 to the neutral, -2 to the
     * passive, +1 (+0.5) to any other hostile mob.
     */
    public static float holyBonus(LivingEntity victim, int level) {
        if (level <= 0) {
            return 0.0F;
        }
        if (victim.getType().is(ModTags.EntityTypes.DEMON)) {
            return 3.0F + 1.5F * (level - 1);
        }
        if (victim.getType().is(EntityTypeTags.UNDEAD)) {
            return 2.0F + 1.0F * (level - 1);
        }
        if (victim.getType().is(ModTags.EntityTypes.NEUTRAL_MOBS)) {
            return -1.0F;
        }
        if (victim.getType().is(ModTags.EntityTypes.PASSIVE_MOBS)) {
            return -2.0F;
        }
        return victim instanceof Monster ? 1.0F + 0.5F * (level - 1) : 0.0F;
    }
}
