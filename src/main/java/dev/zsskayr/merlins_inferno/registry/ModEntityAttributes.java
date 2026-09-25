package dev.zsskayr.merlins_inferno.registry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.DruidEntity;
import dev.zsskayr.merlins_inferno.entity.ImpEntity;

/** Registers the default {@code AttributeSupplier} for every custom {@code LivingEntity} this mod adds. */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class ModEntityAttributes {
    private ModEntityAttributes() {
    }

    @SubscribeEvent
    static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.DRUID.get(), DruidEntity.createAttributes().build());
        event.put(ModEntityTypes.IMP.get(), ImpEntity.createAttributes().build());
    }
}
