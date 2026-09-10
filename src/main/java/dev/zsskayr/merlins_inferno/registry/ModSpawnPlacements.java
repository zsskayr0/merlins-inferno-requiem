package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

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
    }
}
