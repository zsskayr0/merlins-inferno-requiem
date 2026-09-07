package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * Central registry for every custom {@code EntityType} the mod adds (hostile mobs first,
 * but any entity - projectiles, thrown items, etc. - goes here too).
 * <p>
 * A registered {@code EntityType} only defines the entity's identity/attributes builder;
 * remember it still needs, separately:
 * <ul>
 *     <li>a default {@code AttributeSupplier} registered via {@code EntityAttributeCreationEvent} (common setup),</li>
 *     <li>a client-side renderer registered via {@code EntityRenderersEvent.RegisterRenderers} (client setup only),</li>
 *     <li>spawn placement rules (biome tags, light level, etc.) if it should spawn naturally.</li>
 * </ul>
 */
public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Merlins_inferno.MODID);

    private ModEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
