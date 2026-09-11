package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.effect.SanctifiedEffect;

/** Central registry for every {@code MobEffect} this mod adds. */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Merlins_inferno.MODID);

    // Icy white-blue, matching Lyrium's palette. See SanctifiedEffect for the tick logic and
    // event.SanctifiedTickHandler/SanctifiedCombatHandler for how it's applied and what it does
    // in combat.
    public static final DeferredHolder<MobEffect, SanctifiedEffect> SANCTIFIED = MOB_EFFECTS.register("sanctified",
            () -> new SanctifiedEffect(MobEffectCategory.HARMFUL, 0xAEE8FF));

    private ModEffects() {
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
