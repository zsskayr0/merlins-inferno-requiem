package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * What a Lyrium geode does to the unholy: every natural Lyrium Block and Cluster, on its random
 * ticks, applies Sanctified to any undead or demon within {@link #OUTER_RADIUS} blocks - level II
 * out there, level IV for anything inside the geode itself ({@link #INNER_RADIUS} blocks of the
 * ticking block covers the whole cavity, since the lining is only ~2-3 blocks from the centre).
 * <p>
 * There is no tile entity and no scheduled tick per block - a geode is dozens of these blocks, and
 * their random ticks together (each block ticks about once a minute; a whole geode several times a
 * second) keep the effect refreshed, so a lone cluster only flickers it while a full geode holds it
 * steadily. Effects don't downgrade: a distant block's level II never replaces the level IV that a
 * nearer one applied.
 * <p>
 * Only undead ({@link EntityTypeTags#UNDEAD}) and demons ({@link ModTags.EntityTypes#DEMON}) are
 * touched - never players or other mobs, which also keeps this from feeding back into anything
 * that spreads Sanctified.
 */
public final class LyriumRadiance {
    /** Reach of the geode's curse. */
    public static final double OUTER_RADIUS = 16.0;
    /** Anything this close to a geode block counts as inside the geode. */
    public static final double INNER_RADIUS = 5.0;
    /** Long enough to bridge the gaps between ticks of a dense geode; refreshed, never stacked. */
    public static final int DURATION_TICKS = 100;
    public static final int OUTER_AMPLIFIER = 1; // level II
    public static final int INNER_AMPLIFIER = 3; // level IV

    private LyriumRadiance() {
    }

    public static void radiate(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        AABB area = new AABB(pos).inflate(OUTER_RADIUS);
        double innerSqr = INNER_RADIUS * INNER_RADIUS;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LyriumRadiance::isAffected)) {
            int amplifier = entity.position().distanceToSqr(center) <= innerSqr ? INNER_AMPLIFIER : OUTER_AMPLIFIER;
            MobEffectInstance current = entity.getEffect(ModEffects.SANCTIFIED);
            if (current == null || current.getAmplifier() < amplifier
                    || (current.getAmplifier() == amplifier && current.getDuration() < DURATION_TICKS / 2)) {
                entity.addEffect(new MobEffectInstance(ModEffects.SANCTIFIED, DURATION_TICKS, amplifier));
            }
        }
    }

    private static boolean isAffected(LivingEntity entity) {
        return entity.isAlive() && (entity.getType().is(EntityTypeTags.UNDEAD) || entity.getType().is(ModTags.EntityTypes.DEMON));
    }
}
