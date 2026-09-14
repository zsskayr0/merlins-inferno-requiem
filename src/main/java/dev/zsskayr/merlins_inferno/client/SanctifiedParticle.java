package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Sanctified's own particle - a slow-drifting sparkle rendered from the mod's
 * {@code textures/particle/sanct.png}, replacing vanilla's generic recolored {@code ENTITY_EFFECT}
 * swirl. Motion/lifetime mirror vanilla's {@code SpellParticle} (used for Instant Health/potion
 * swirls), just without the spyglass-scoping fade-out, which doesn't apply here.
 */
@OnlyIn(Dist.CLIENT)
public class SanctifiedParticle extends TextureSheetParticle {
    private static final RandomSource RANDOM = RandomSource.create();
    private final SpriteSet sprites;

    protected SanctifiedParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed,
            double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, 0.5 - RANDOM.nextDouble(), ySpeed, 0.5 - RANDOM.nextDouble());
        this.friction = 0.96F;
        this.gravity = -0.1F;
        this.speedUpWhenYMotionIsBlocked = true;
        this.sprites = sprites;
        this.yd *= 0.2F;
        if (xSpeed == 0.0 && zSpeed == 0.0) {
            this.xd *= 0.1F;
            this.zd *= 0.1F;
        }
        this.quadSize *= 0.75F;
        this.lifetime = (int) (8.0 / (Math.random() * 0.8 + 0.2));
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new SanctifiedParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
