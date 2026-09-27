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
import dev.zsskayr.merlins_inferno.entity.DullahanSteedEntity;
import dev.zsskayr.merlins_inferno.entity.GhostEntity;
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

    // The Hallowed Grove's night miniboss - see DullahanEntity. 0.75 x 2.3 matches the model (2.295 blocks tall
    // on foot). ridingOffset lowers it onto the saddle: its hips sit 0.875 above its feet and the mounted clips
    // only bend the legs, so without -0.8 it would float ~0.8 above the seat (seat point 1.32 on the steed).
    public static final DeferredHolder<EntityType<?>, EntityType<DullahanEntity>> DULLAHAN = ENTITY_TYPES.register("dullahan",
            () -> EntityType.Builder.of(DullahanEntity::new, MobCategory.MONSTER)
                    .sized(0.75F, 2.3F)
                    .eyeHeight(2.0F)
                    .ridingOffset(-0.8F)
                    .clientTrackingRange(10)
                    .build(Merlins_inferno.MODID + ":dullahan"));

    // The Dullahan's mount - vanilla skeleton-horse size, own type so its AI can be replaced (see the class).
    // MISC: it never counts against a mob cap and never spawns on its own.
    public static final DeferredHolder<EntityType<?>, EntityType<DullahanSteedEntity>> DULLAHAN_STEED = ENTITY_TYPES.register("dullahan_steed",
            () -> EntityType.Builder.of(DullahanSteedEntity::new, MobCategory.MISC)
                    .sized(1.3964844F, 1.6F)
                    .eyeHeight(1.52F)
                    .passengerAttachments(1.31875F)
                    .clientTrackingRange(10)
                    .build(Merlins_inferno.MODID + ":dullahan_steed"));

    // The Angelical Circle 1 boss (see EliasEntity), the church's Sacred Priest guardian and its neutral
    // congregation. Elias never spawns on his own: the altar wakes him once the Priest has fallen. The Priest is
    // placed by the church structure; cultists also wander the world rarely (biome modifier).
    public static final DeferredHolder<EntityType<?>, EntityType<EliasEntity>> ELIAS = ENTITY_TYPES.register("elias",
            () -> EntityType.Builder.of(EliasEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.1F)
                    .eyeHeight(1.85F)
                    .clientTrackingRange(10)
                    .build(Merlins_inferno.MODID + ":elias"));

    public static final DeferredHolder<EntityType<?>, EntityType<SacredCultistEntity>> SACRED_CULTIST = ENTITY_TYPES.register("sacred_cultist",
            () -> EntityType.Builder.of(SacredCultistEntity::new, MobCategory.CREATURE)
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
                    .sized(0.8F, 2.4F)
                    .eyeHeight(2.1F)
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

    // A harmless Otherworld spirit - night only, and only once the nearest player has reached Circle 2
    // (see GhostEntity::checkGhostSpawnRules). The only source of Otherworld Essence.
    public static final DeferredHolder<EntityType<?>, EntityType<GhostEntity>> GHOST = ENTITY_TYPES.register("ghost",
            () -> EntityType.Builder.of(GhostEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .eyeHeight(1.6F)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .build(Merlins_inferno.MODID + ":ghost"));

    private ModEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
