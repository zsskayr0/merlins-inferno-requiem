package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/** Custom sound events; the files they play are mapped in {@code assets/merlins_inferno/sounds.json}. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Merlins_inferno.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> STARVED_AMBIENT = register("entity.starved.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARVED_NOTICE = register("entity.starved.notice");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARVED_HUNGER = register("entity.starved.hunger");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARVED_STEP = register("entity.starved.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARVED_ATTACK = register("entity.starved.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARVED_CHARGE = register("entity.starved.charge");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, name)));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
