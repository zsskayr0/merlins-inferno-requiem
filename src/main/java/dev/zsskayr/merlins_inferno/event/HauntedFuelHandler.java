package dev.zsskayr.merlins_inferno.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * Haunted wood is fireproof, so no furnace burns it: every Haunted item has no burn time (planks, doors and the
 * rest would otherwise inherit vanilla's tag-based fuel values). The Haunted log still fuels the Hell Forge,
 * which has its own fuel table (see HellForgeBlockEntity).
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class HauntedFuelHandler {
    private HauntedFuelHandler() {
    }

    @SubscribeEvent
    static void onBurnTime(FurnaceFuelBurnTimeEvent event) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (id.getNamespace().equals(Merlins_inferno.MODID) && id.getPath().contains("haunted")) {
            event.setBurnTime(0);
        }
    }
}
