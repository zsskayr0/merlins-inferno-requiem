package dev.zsskayr.merlins_inferno.block;

import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The natural red geode's counterpart to LyriumRadiance. */
public final class RedLyriumRadiance {
    private RedLyriumRadiance() {}

    public static boolean isHuman(LivingEntity entity) {
        return entity instanceof Player || (entity.getType().is(ModTags.EntityTypes.HUMANS)
                && !entity.getType().is(EntityTypeTags.UNDEAD));
    }

    public static void radiate(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(pos).inflate(LyriumRadiance.OUTER_RADIUS),
                living -> living.isAlive() && isHuman(living))) {
            int amplifier = entity.position().distanceToSqr(center) <= LyriumRadiance.INNER_RADIUS * LyriumRadiance.INNER_RADIUS
                    ? LyriumRadiance.INNER_AMPLIFIER : LyriumRadiance.OUTER_AMPLIFIER;
            MobEffectInstance current = entity.getEffect(ModEffects.DEMONIFIED);
            if (current == null || current.getAmplifier() < amplifier
                    || (current.getAmplifier() == amplifier && current.getDuration() < LyriumRadiance.DURATION_TICKS / 2)) {
                entity.addEffect(new MobEffectInstance(ModEffects.DEMONIFIED, LyriumRadiance.DURATION_TICKS, amplifier));
            }
        }
    }
}
