package dev.zsskayr.merlins_inferno.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

import dev.zsskayr.merlins_inferno.item.DarkCovenItem;

/**
 * What others see of an invoked coven: a few motes of dust drifting up around the bearer's feet, the colour of the stage,
 * and now and then a wisp of soul fire. Meant to be noticed only by someone looking - the more so as the stage climbs.
 */
public final class DemonicPactAura {
    private DemonicPactAura() {
    }

    /** One breath of the aura; called every few ticks while the coven lasts. */
    public static void emit(ServerLevel level, LivingEntity bearer, int stage) {
        var random = bearer.getRandom();
        float[] c = DarkCovenItem.color(stage);
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 0.7F);
        int motes = 1 + stage / 3;
        for (int i = 0; i < motes; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 0.35 + random.nextDouble() * 0.35;
            level.sendParticles(dust, bearer.getX() + Math.cos(angle) * radius, bearer.getY() + 0.05 + random.nextDouble() * 0.5,
                    bearer.getZ() + Math.sin(angle) * radius, 0, 0.0, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(6 - Math.min(stage, 4)) == 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, bearer.getX() + (random.nextDouble() - 0.5) * 0.6, bearer.getY() + 0.1,
                    bearer.getZ() + (random.nextDouble() - 0.5) * 0.6, 0, 0.0, 0.03, 0.0, 1.0);
        }
    }
}
