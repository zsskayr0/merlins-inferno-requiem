package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import dev.zsskayr.merlins_inferno.portal.OblivionDimension;

/**
 * Oblivion's biome lets monsters spawn in the dark (it is a farm dimension), but not around the arrival hub, where a
 * player just stepping through the portal must be safe.
 */
public class OblivionSpawnHandler {
    @SubscribeEvent
    public void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL || event.getLevel().getLevel().dimension() != OblivionDimension.LEVEL) {
            return;
        }
        if (OblivionDimension.isNearHub(event.getX(), event.getZ())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }
}
