package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.DruidEntity;

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

    // Same hitbox as a Player/Zombie (both 0.6 x 1.95) - it's built on the same humanoid shape
    // for now (see ModEntityRenderers), no custom model yet.
    public static final DeferredHolder<EntityType<?>, EntityType<DruidEntity>> DRUID = ENTITY_TYPES.register("druid",
            () -> EntityType.Builder.of(DruidEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .build(Merlins_inferno.MODID + ":druid"));

    private ModEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
