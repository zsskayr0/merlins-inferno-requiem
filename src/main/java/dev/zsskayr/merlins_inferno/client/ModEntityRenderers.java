package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.renderer.Sheets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRecipeBookCategoriesEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModMenuTypes;
import dev.zsskayr.merlins_inferno.registry.ModParticles;
import dev.zsskayr.merlins_inferno.registry.ModRecipeTypes;
import dev.zsskayr.merlins_inferno.registry.ModWoodTypes;

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
        event.registerEntityRenderer(ModEntityTypes.IMP.get(), ImpRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DULLAHAN.get(), DullahanRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DULLAHAN_STEED.get(), DullahanSteedRenderer::new);
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

    /**
     * Without this the client logs "Unknown recipe category" for every Hell Forge recipe: vanilla
     * only maps its own cooking recipe types to recipe-book tabs.
     */
    @SubscribeEvent
    static void onRegisterRecipeBookCategories(RegisterRecipeBookCategoriesEvent event) {
        event.registerRecipeCategoryFinder(ModRecipeTypes.HELL_FORGE_SMELTING.get(), holder -> RecipeBookCategories.FURNACE_MISC);
    }

    /** Adds Ashwood's sign textures to the sign atlas; {@code Sheets.addWoodType} isn't thread-safe, hence enqueueWork. */
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> Sheets.addWoodType(ModWoodTypes.ASHWOOD));
    }
}
