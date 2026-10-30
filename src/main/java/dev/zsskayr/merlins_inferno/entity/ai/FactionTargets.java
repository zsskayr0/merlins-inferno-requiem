package dev.zsskayr.merlins_inferno.entity.ai;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Witch;

import dev.zsskayr.merlins_inferno.compat.tensura.TensuraCompat;
import dev.zsskayr.merlins_inferno.entity.EliasEntity;
import dev.zsskayr.merlins_inferno.entity.ImpEntity;
import dev.zsskayr.merlins_inferno.entity.OstaraEntity;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Who the two routes hunt on sight, for the mobs of this mod and (through the tags) for other mods' mobs such as
 * Tensura's. The Angelical route purges the unholy, witches and, with Tensura, the Majin-aligned ({@link #isAngelicPrey});
 * the Infernal route preys on the humans ({@link #isDemonicPrey}, open through {@code merlins_inferno:humans}).
 */
public final class FactionTargets {

    private FactionTargets() {
    }

    /** Undead, demons, witches and (with Tensura) the Majin-aligned - but not the Druidic route's own (Ostara), nor other humans. */
    public static boolean isAngelicPrey(LivingEntity living) {
        if (living instanceof Witch) {
            return true;
        }
        if (living instanceof OstaraEntity || living instanceof EliasEntity || living instanceof ImpEntity
                || living.getType().is(ModTags.EntityTypes.HUMANS)) {
            return false;
        }
        return living.getType().is(EntityTypeTags.UNDEAD)
                || living.getType().is(ModTags.EntityTypes.DEMON)
                || TensuraCompat.isMajin(living);
    }

    /** Humans: villagers, illagers and the like, the cultists included, plus other mods' (Tensura otherworlders). */
    public static boolean isDemonicPrey(LivingEntity living) {
        return living.getType().is(ModTags.EntityTypes.HUMANS) && !living.getType().is(ModTags.EntityTypes.DEMON);
    }

    /** Priority-5 style goal: angelic mobs hunt {@link #isAngelicPrey}. */
    public static NearestAttackableTargetGoal<Mob> huntForAngels(Mob mob) {
        return new NearestAttackableTargetGoal<>(mob, Mob.class, 5, false, false, FactionTargets::isAngelicPrey);
    }

    /** Demonic mobs hunt {@link #isDemonicPrey}. */
    public static NearestAttackableTargetGoal<Mob> huntForDemons(Mob mob) {
        return new NearestAttackableTargetGoal<>(mob, Mob.class, 5, false, false, FactionTargets::isDemonicPrey);
    }
}
