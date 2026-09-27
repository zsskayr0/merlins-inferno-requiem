package dev.zsskayr.merlins_inferno.registry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.AndrasEntity;
import dev.zsskayr.merlins_inferno.entity.DruidEntity;
import dev.zsskayr.merlins_inferno.entity.DullahanEntity;
import dev.zsskayr.merlins_inferno.entity.DullahanSteedEntity;
import dev.zsskayr.merlins_inferno.entity.ImpEntity;
import dev.zsskayr.merlins_inferno.entity.EliasEntity;
import dev.zsskayr.merlins_inferno.entity.OstaraEntity;
import dev.zsskayr.merlins_inferno.entity.SacredPriestEntity;
import dev.zsskayr.merlins_inferno.entity.StarvedEntity;
import dev.zsskayr.merlins_inferno.entity.SacredCultistEntity;

/** Registers the default {@code AttributeSupplier} for every custom {@code LivingEntity} this mod adds. */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class ModEntityAttributes {
    private ModEntityAttributes() {
    }

    @SubscribeEvent
    static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.DRUID.get(), DruidEntity.createAttributes().build());
        event.put(ModEntityTypes.IMP.get(), ImpEntity.createAttributes().build());
        event.put(ModEntityTypes.STARVED.get(), StarvedEntity.createAttributes().build());
        event.put(ModEntityTypes.DULLAHAN.get(), DullahanEntity.createAttributes().build());
        event.put(ModEntityTypes.DULLAHAN_STEED.get(), DullahanSteedEntity.createAttributes().build());
        event.put(ModEntityTypes.ELIAS.get(), EliasEntity.createAttributes().build());
        event.put(ModEntityTypes.SACRED_CULTIST.get(), SacredCultistEntity.createAttributes().build());
        event.put(ModEntityTypes.SACRED_PRIEST.get(), SacredPriestEntity.createAttributes().build());
        event.put(ModEntityTypes.ANDRAS.get(), AndrasEntity.createAttributes().build());
        event.put(ModEntityTypes.OSTARA.get(), OstaraEntity.createAttributes().build());
    }
}
