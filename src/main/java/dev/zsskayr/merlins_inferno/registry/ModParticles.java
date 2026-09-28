package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/** Central registry for every custom {@code ParticleType} this mod adds. */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Merlins_inferno.MODID);

    // Held as a plain static instance (not looked up via the DeferredHolder below) because
    // Registries.MOB_EFFECT's RegisterEvent fires before Registries.PARTICLE_TYPE's - a fixed
    // vanilla registry-layer order, unaffected by our own .register(modEventBus) call order in
    // Merlins_inferno's constructor. ModEffects.SANCTIFIED needs this instance already
    // constructed (as the ParticleOptions for its 3-arg MobEffect constructor) at a point where
    // ModParticles.SANCTIFIED.get() would still throw "unbound value".
    public static final SimpleParticleType SANCTIFIED_TYPE = new SimpleParticleType(false);

    // Sanctified's signature particle - see client.SanctifiedParticle for the actual sprite/motion,
    // and assets/merlins_inferno/particles/sanctified.json for the texture it points to
    // (textures/particle/sanct.png).
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SANCTIFIED = PARTICLE_TYPES.register("sanctified",
            () -> SANCTIFIED_TYPE);

    // Oblivion portal runes: drawn with the enchanting-table glyph particle class (see client.ModEntityRenderers),
    // in dark, desaturated glyph textures (assets/.../particles/oblivion_rune.json).
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> OBLIVION_RUNE = PARTICLE_TYPES.register("oblivion_rune",
            () -> new SimpleParticleType(false));

    private ModParticles() {
    }

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
