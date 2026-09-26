package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.DullahanEntity;
import dev.zsskayr.merlins_inferno.entity.ImpEntity;
import dev.zsskayr.merlins_inferno.entity.StarvedEntity;

/** Natural-spawn placement rules for every custom mob this mod adds. */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class ModSpawnPlacements {
    private ModSpawnPlacements() {
    }

    @SubscribeEvent
    static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Standard ground-dweller placement (same as most vanilla passive mobs) - the actual "only
        // in Hallowed Grove" restriction isn't here at all, it's simply that ModBiomeProvider only
        // ever adds ModEntityTypes.DRUID to that one biome's mob spawn list.
        event.register(ModEntityTypes.DRUID.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);

        // Flying mob: no ground requirement, just open air that isn't above a lava lake. Its
        // actual biomes (every Nether biome) come from data/.../neoforge/biome_modifier/imp_spawns.json.
        event.register(ModEntityTypes.IMP.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ImpEntity::checkImpSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);

        // Rare, solitary, deep-and-dark Nether miniboss; biomes come from biome_modifier/starved_spawns.json.
        event.register(ModEntityTypes.STARVED.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                StarvedEntity::checkStarvedSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);

        // Night-only, Hallowed-Grove-only, one at a time - the rule itself carries all three (the
        // biome's spawn list just makes it a rare entry, see ModBiomeProvider).
        event.register(ModEntityTypes.DULLAHAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                DullahanEntity::checkDullahanSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
