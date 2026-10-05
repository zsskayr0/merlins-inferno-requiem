package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.AndrasEntity;
import dev.zsskayr.merlins_inferno.entity.DruidEntity;
import dev.zsskayr.merlins_inferno.entity.DullahanEntity;
import dev.zsskayr.merlins_inferno.entity.GrymnEntity;
import dev.zsskayr.merlins_inferno.entity.EliasEntity;
import dev.zsskayr.merlins_inferno.entity.OstaraEntity;
import dev.zsskayr.merlins_inferno.entity.SacredPriestEntity;
import dev.zsskayr.merlins_inferno.entity.StarvedEntity;
import dev.zsskayr.merlins_inferno.entity.SacredCultistEntity;
import dev.zsskayr.merlins_inferno.entity.ImpEntity;

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

    // Small flying demon of the Nether - see ImpEntity. Fire-immune like the rest of the Nether's
    // residents (lava is only a "hard to recover loot" problem for it, never a death trap).
    public static final DeferredHolder<EntityType<?>, EntityType<ImpEntity>> IMP = ENTITY_TYPES.register("imp",
            () -> EntityType.Builder.of(ImpEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.8F)
                    .eyeHeight(0.6F)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .build(Merlins_inferno.MODID + ":imp"));

    // Four-legged Nether miniboss - see StarvedEntity. ~1.75 blocks at the shoulders in the model.
    public static final DeferredHolder<EntityType<?>, EntityType<StarvedEntity>> STARVED = ENTITY_TYPES.register("starved",
            () -> EntityType.Builder.of(StarvedEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 2.0F)
                    .eyeHeight(1.75F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(Merlins_inferno.MODID + ":starved"));

    // Circle 1 Nether miniboss - see GrymnEntity. Low-flying phantom: ~1.5 blocks wide and 2.6 tall in the model
    // (bind pose spans y 0.13-2.64, head at ~2.25), so the entity origin is its lowest point, hovering above the floor.
    public static final DeferredHolder<EntityType<?>, EntityType<GrymnEntity>> GRYMN = ENTITY_TYPES.register("grymn",
            () -> EntityType.Builder.of(GrymnEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 2.6F)
                    .eyeHeight(2.2F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(Merlins_inferno.MODID + ":grymn"));

    // The Hallowed Grove's night miniboss - see DullahanEntity. Knight and horse are ONE model (about 1.5 blocks wide
    // along its length and 3 tall), so the hitbox is a 1.4 x 3.0 column.
    public static final DeferredHolder<EntityType<?>, EntityType<DullahanEntity>> DULLAHAN = ENTITY_TYPES.register("dullahan",
            () -> EntityType.Builder.of(DullahanEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 3.0F)
                    .eyeHeight(2.7F)
                    .clientTrackingRange(10)
                    .build(Merlins_inferno.MODID + ":dullahan"));


    // The Angelical Circle 1 boss (see EliasEntity), the church's Sacred Priest guardian and its neutral
    // congregation. Elias never spawns on his own: the altar wakes him once the Priest has fallen. The Priest is
    // placed by the church structure; cultists also wander the world rarely (biome modifier).
    // Matches the GeckoLib model's ~52-unit (3.3-block) height; the blades and cape reach far past the box on purpose.
    public static final DeferredHolder<EntityType<?>, EntityType<EliasEntity>> ELIAS = ENTITY_TYPES.register("elias",
            () -> EntityType.Builder.of(EliasEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.4F)
                    .eyeHeight(2.95F)
                    .clientTrackingRange(10)
                    .build(Merlins_inferno.MODID + ":elias"));

    public static final DeferredHolder<EntityType<?>, EntityType<SacredCultistEntity>> SACRED_CULTIST = ENTITY_TYPES.register("sacred_cultist",
            () -> EntityType.Builder.of(SacredCultistEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .clientTrackingRange(8)
                    .build(Merlins_inferno.MODID + ":sacred_cultist"));

    public static final DeferredHolder<EntityType<?>, EntityType<SacredPriestEntity>> SACRED_PRIEST = ENTITY_TYPES.register("sacred_priest",
            () -> EntityType.Builder.of(SacredPriestEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .clientTrackingRange(10)
                    .build(Merlins_inferno.MODID + ":sacred_priest"));

    // Infernal Circle 1 boss - see AndrasEntity. Fire-immune, like the rest of the Nether's residents.
    public static final DeferredHolder<EntityType<?>, EntityType<AndrasEntity>> ANDRAS = ENTITY_TYPES.register("andras",
            () -> EntityType.Builder.of(AndrasEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 3.0F)
                    .eyeHeight(2.6F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(Merlins_inferno.MODID + ":andras"));

    // Mundane (Druidic) Circle 1 boss - see OstaraEntity.
    public static final DeferredHolder<EntityType<?>, EntityType<OstaraEntity>> OSTARA = ENTITY_TYPES.register("ostara",
            () -> EntityType.Builder.of(OstaraEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.2F)
                    .eyeHeight(1.95F)
                    .clientTrackingRange(10)
                    .build(Merlins_inferno.MODID + ":ostara"));

    private ModEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
