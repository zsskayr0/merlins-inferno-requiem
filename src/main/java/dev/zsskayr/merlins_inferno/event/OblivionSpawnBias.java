package dev.zsskayr.merlins_inferno.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.portal.OblivionDimension;

/**
 * Oblivion shares the vanilla Nether biomes, whose spawn lists are dominated by piglins, hoglins and ghasts, and a
 * biome modifier cannot be limited to one dimension. So the vanilla monsters' natural spawns are rejected part of the
 * time here; a rejected pick is simply retried, which leaves the mod's own mobs a far larger share of the cap.
 */
public class OblivionSpawnBias {
    /** Chance that a natural spawn of a vanilla monster is refused in Oblivion. */
    private static final float VANILLA_REJECT_CHANCE = 0.65F;

    @SubscribeEvent
    public void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL
                || event.getLevel().getLevel().dimension() != OblivionDimension.LEVEL) {
            return;
        }
        EntityType<?> type = event.getEntity().getType();
        if (type.getCategory() != MobCategory.MONSTER
                || BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(Merlins_inferno.MODID)) {
            return;
        }
        if (event.getLevel().getRandom().nextFloat() < VANILLA_REJECT_CHANCE) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }
}
