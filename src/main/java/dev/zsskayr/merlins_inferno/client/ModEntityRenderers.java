package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.event.RegisterRecipeBookCategoriesEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.ChampionSeekerItem;
import dev.zsskayr.merlins_inferno.item.PandoraBoxItem;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import dev.zsskayr.merlins_inferno.registry.ModMenuTypes;
import dev.zsskayr.merlins_inferno.menu.PandoraBoxMenu;
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
        event.registerEntityRenderer(ModEntityTypes.STARVED.get(), StarvedRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.GRYMN.get(), GrymnRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DULLAHAN.get(), DullahanRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.ELIAS.get(), EliasRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.SACRED_CULTIST.get(), SacredCultistRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.SACRED_PRIEST.get(), SacredPriestRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.ANDRAS.get(), AndrasRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.OSTARA.get(), OstaraRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.HELL_FORGE.get(), HellForgeBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.DRYING_RACK.get(), DryingRackRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.PANDORA_BOX.get(), context -> new PandoraBoxRenderer());
        event.registerBlockEntityRenderer(ModBlockEntityTypes.SACRED_ALTAR.get(), context -> new CelestialAltarRenderer());
    }

    /** The Box item plays its idle clip only while the cursor is over it in an inventory screen. */
    @SubscribeEvent
    static void onScreenRender(ScreenEvent.Render.Pre event) {
        PandoraBoxItem.hovered = event.getScreen() instanceof AbstractContainerScreen<?> screen
                && screen.getSlotUnderMouse() != null
                && screen.getSlotUnderMouse().getItem().is(ModItems.PANDORA_BOX.get());
    }

    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private PandoraBoxItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new PandoraBoxItemRenderer();
                }
                return this.renderer;
            }
        }, ModItems.PANDORA_BOX.get());
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.HELL_FORGE.get(), HellForgeScreen::new);
        event.register(ModMenuTypes.PANDORA_BOX.get(), PandoraBoxScreen::new);
    }

    @SubscribeEvent
    static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SANCTIFIED_TYPE, SanctifiedParticle.Provider::new);
        event.registerSpriteSet(ModParticles.OBLIVION_RUNE.get(), RuneParticle.OblivionProvider::new);
        event.registerSpriteSet(ModParticles.PANDORA_RUNE.get(), RuneParticle.PandoraProvider::new);
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
        event.enqueueWork(() -> {
            Sheets.addWoodType(ModWoodTypes.ASHWOOD);
            ItemProperties.register(ModItems.CHAMPION_SEEKER.get(), ResourceLocation.withDefaultNamespace("angle"),
                    (stack, level, entity, seed) -> {
                        if (!(entity instanceof LivingEntity holder)) {
                            return 0.0F;
                        }
                        LivingEntity target = getOrRefreshTarget(holder);
                        if (target == null) {
                            // No boss loaded nearby: spin slowly instead of freezing on a stale heading.
                            return (holder.level().getGameTime() % 360L) / 360.0F;
                        }
                        return ChampionSeekerItem.getAngle(holder, new Vec3(target.getX(), target.getY(), target.getZ()));
                    });
        });
    }

    /**
     * How often (in ticks) to re-scan for the nearest boss. The property function above is called
     * every frame the item is drawn (held, hotbar, inventory...), and {@code findNearestBoss} is an
     * entity-list scan - throttling it here keeps the Champion Seeker from costing noticeable client
     * FPS while its needle still updates every frame using the cached target's live position.
     */
    private static final int RESCAN_INTERVAL_TICKS = 10;
    private static final double SEARCH_RADIUS = 200.0D;

    private static long lastScanTick = Long.MIN_VALUE;
    private static LivingEntity cachedTarget;

    private static LivingEntity getOrRefreshTarget(LivingEntity holder) {
        long tick = holder.level().getGameTime();
        boolean staleCache = cachedTarget != null && (!cachedTarget.isAlive() || cachedTarget.level() != holder.level());
        if (staleCache || tick - lastScanTick >= RESCAN_INTERVAL_TICKS) {
            cachedTarget = ChampionSeekerItem.findNearestBoss(holder, SEARCH_RADIUS);
            lastScanTick = tick;
        }
        return cachedTarget;
    }
}
