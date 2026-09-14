package dev.zsskayr.merlins_inferno.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModMenuTypes;
import dev.zsskayr.merlins_inferno.registry.ModParticles;

/** Client-only: model layers + renderers for every custom entity/block entity this mod adds. */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class ModEntityRenderers {
    private ModEntityRenderers() {
    }

    @SubscribeEvent
    static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DruidModel.LAYER_LOCATION, DruidModel::createBodyLayer);
        event.registerLayerDefinition(HellForgeModel.LAYER_LOCATION, HellForgeModel::createBodyLayer);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.DRUID.get(), DruidRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.HELL_FORGE.get(), HellForgeBlockEntityRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.HELL_FORGE.get(), HellForgeScreen::new);
    }

    @SubscribeEvent
    static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SANCTIFIED_TYPE, SanctifiedParticle.Provider::new);
    }
}
