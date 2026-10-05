package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FlyTowardsPositionParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * An enchanting-table glyph drawn from the white rune sprites in {@code textures/particle/rune/}, tinted per use:
 * the Oblivion portal's void navy, or the Pandora Box's full-bright range from vivid purple to blue.
 */
public class RuneParticle extends FlyTowardsPositionParticle {
    private final boolean fullBright;

    private RuneParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, float[] rgb, boolean fullBright) {
        super(level, x, y, z, xd, yd, zd);
        this.setColor(rgb[0], rgb[1], rgb[2]);
        this.fullBright = fullBright;
    }

    @Override
    public int getLightColor(float partialTick) {
        return this.fullBright ? LightTexture.FULL_BRIGHT : super.getLightColor(partialTick);
    }

    /** Deep navy, taken from the Void Block's texture. */
    public static class OblivionProvider implements ParticleProvider<SimpleParticleType> {
        private static final float[] COLOR = { 0.16F, 0.23F, 0.48F };
        private final SpriteSet sprites;

        public OblivionProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            RuneParticle particle = new RuneParticle(level, x, y, z, xd, yd, zd, COLOR, false);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }

    /** A random point on a purple-to-blue scale, with a little brightness variation; always full-bright. */
    public static class PandoraProvider implements ParticleProvider<SimpleParticleType> {
        private static final float[] PURPLE = { 0.78F, 0.2F, 1.0F };
        private static final float[] BLUE = { 0.2F, 0.45F, 1.0F };
        private final SpriteSet sprites;

        public PandoraProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            RandomSource random = level.random;
            float t = random.nextFloat();
            float shade = 0.75F + random.nextFloat() * 0.25F;
            float[] color = new float[3];
            for (int i = 0; i < 3; i++) {
                color[i] = (PURPLE[i] + (BLUE[i] - PURPLE[i]) * t) * shade;
            }
            RuneParticle particle = new RuneParticle(level, x, y, z, xd, yd, zd, color, true);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
