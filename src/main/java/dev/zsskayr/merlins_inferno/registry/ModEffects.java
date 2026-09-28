package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.effect.MarkOfDebtEffect;
import dev.zsskayr.merlins_inferno.effect.SanctifiedEffect;

/** Central registry for every {@code MobEffect} this mod adds. */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Merlins_inferno.MODID);

    // Icy white-blue, matching Lyrium's palette. See SanctifiedEffect for the tick logic and
    // event.SanctifiedTickHandler/SanctifiedCombatHandler for how it's applied and what it does
    // in combat. Uses the 3-arg MobEffect constructor so it renders our own custom-textured
    // particle (client.SanctifiedParticle, from textures/particle/sanct.png) instead of vanilla's
    // generic recolored ENTITY_EFFECT swirl - a SimpleParticleType doubles as its own ParticleOptions.
    public static final DeferredHolder<MobEffect, SanctifiedEffect> SANCTIFIED = MOB_EFFECTS.register("sanctified",
            () -> new SanctifiedEffect(MobEffectCategory.HARMFUL, 0xAEE8FF, ModParticles.SANCTIFIED_TYPE));

    // Andras' brand: the bearer takes extra damage (see event.MarkOfDebtHandler). Blood red.
    public static final DeferredHolder<MobEffect, MarkOfDebtEffect> MARK_OF_DEBT = MOB_EFFECTS.register("mark_of_debt",
            () -> new MarkOfDebtEffect(MobEffectCategory.HARMFUL, 0x8B1A1A));

    // Drunk from an Antidote: immunity to Poison (Ostara's petals included). See event.AntivenomHandler.
    public static final DeferredHolder<MobEffect, MarkOfDebtEffect> ANTIVENOM = MOB_EFFECTS.register("antivenom",
            () -> new MarkOfDebtEffect(MobEffectCategory.BENEFICIAL, 0x7FD66B));

    private ModEffects() {
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
