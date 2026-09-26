package dev.zsskayr.merlins_inferno.registry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.DruidEntity;
import dev.zsskayr.merlins_inferno.entity.DullahanEntity;
import dev.zsskayr.merlins_inferno.entity.DullahanSteedEntity;
import dev.zsskayr.merlins_inferno.entity.ImpEntity;
import dev.zsskayr.merlins_inferno.entity.PenitentEntity;
import dev.zsskayr.merlins_inferno.entity.WorshipperEntity;

/** Registers the default {@code AttributeSupplier} for every custom {@code LivingEntity} this mod adds. */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class ModEntityAttributes {
    private ModEntityAttributes() {
    }

    @SubscribeEvent
    static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.DRUID.get(), DruidEntity.createAttributes().build());
        event.put(ModEntityTypes.IMP.get(), ImpEntity.createAttributes().build());
        event.put(ModEntityTypes.DULLAHAN.get(), DullahanEntity.createAttributes().build());
        event.put(ModEntityTypes.DULLAHAN_STEED.get(), DullahanSteedEntity.createAttributes().build());
        event.put(ModEntityTypes.PENITENT.get(), PenitentEntity.createAttributes().build());
        event.put(ModEntityTypes.WORSHIPPER.get(), WorshipperEntity.createAttributes().build());
    }
}
