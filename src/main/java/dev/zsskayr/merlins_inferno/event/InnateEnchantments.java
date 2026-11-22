package dev.zsskayr.merlins_inferno.event;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;

import dev.zsskayr.merlins_inferno.item.DarkCovenItem;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * The Evil and Holy levels built into the gear of the Infernal and Angelical routes, and the damage each level adds - the numbers of
 * {@code data/merlins_inferno/enchantment/evil.json}, {@code holy.json} and {@code bane_of_humanity.json} (with {@link HolyCombatHandler}'s tier
 * for any other {@link Monster}). The route's gear carries them without the enchantment, so a grindstone cannot take them off.
 * <p>
 * Every bonus is a <b>fraction of the blow</b> (0.15 = 15% more damage, -0.05 = 5% less), never a flat number, so it keeps its weight whatever the
 * weapon, the strength or the mods around it.
 * <p>
 * Evil: Demonblood I, Ebony II (plus its own corruption, {@link #ebonyBonus}), the Dark Coven III (stages I-III), IV (IV-VI) and V (VII).
 * Holy: Seraphium I, Luminite II. (The Debt Collector and Vigil's Sorrow carry theirs in their own handlers.)
 */
public final class InnateEnchantments {
    /** Ebony's corruption: what its blade adds against any target that is not a demon, when Evil and the Bane have nothing larger to say. */
    public static final float EBONY_GENERAL = 0.15F;
    /** The Bane of Humanity level Ebony carries against humans. */
    public static final int EBONY_BANE_LEVEL = 2;

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

    /** What Evil of this level adds against the victim: +20% (+10% per level above the first) to the passive, +10% (+5%) to the neutral. */
    public static float evilBonus(LivingEntity victim, int level) {
        if (level <= 0) {
            return 0.0F;
        }
        if (victim.getType().is(ModTags.EntityTypes.PASSIVE_MOBS)) {
            return 0.20F + 0.10F * (level - 1);
        }
        if (victim.getType().is(ModTags.EntityTypes.NEUTRAL_MOBS)) {
            return 0.10F + 0.05F * (level - 1);
        }
        return 0.0F;
    }

    /** What Bane of Humanity of this level adds against humans: +15% (+10% per level above the first). */
    public static float baneBonus(int level) {
        return level <= 0 ? 0.0F : 0.15F + 0.10F * (level - 1);
    }

    /**
     * What an Ebony weapon adds against the victim: the largest of Evil II (passive, neutral), Bane of Humanity II (humans) and a flat
     * {@link #EBONY_GENERAL} against everything else - and nothing against a demon, which is the route's own.
     */
    public static float ebonyBonus(LivingEntity victim) {
        var type = victim.getType();
        if (type.is(ModTags.EntityTypes.DEMON)) {
            return 0.0F;
        }
        float bonus = Math.max(EBONY_GENERAL, evilBonus(victim, 2));
        if (type.is(ModTags.EntityTypes.HUMANS)) {
            bonus = Math.max(bonus, baneBonus(EBONY_BANE_LEVEL));
        }
        return bonus;
    }

    /**
     * What Holy of this level adds (or takes, below zero): +15% (+7.5%) to the demons, +10% (+5%) to the Undead, -5% to the neutral, -10% to the
     * passive, +5% (+2.5%) to any other hostile mob.
     */
    public static float holyBonus(LivingEntity victim, int level) {
        if (level <= 0) {
            return 0.0F;
        }
        if (victim.getType().is(ModTags.EntityTypes.DEMON)) {
            return 0.15F + 0.075F * (level - 1);
        }
        if (victim.getType().is(EntityTypeTags.UNDEAD)) {
            return 0.10F + 0.05F * (level - 1);
        }
        if (victim.getType().is(ModTags.EntityTypes.NEUTRAL_MOBS)) {
            return -0.05F;
        }
        if (victim.getType().is(ModTags.EntityTypes.PASSIVE_MOBS)) {
            return -0.10F;
        }
        return victim instanceof Monster ? 0.05F + 0.025F * (level - 1) : 0.0F;
    }
}
