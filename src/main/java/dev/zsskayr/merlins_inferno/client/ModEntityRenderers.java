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
    @SubscribeEvent
    static void onCinderReload(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> CinderMeshes.invalidate());
    }
    private ModEntityRenderers() {
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.BENCH_SEAT.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.HAUNTED_BOAT.get(), HauntedBoatRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DRUID.get(), DruidRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.IMP.get(), ImpRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.STARVED.get(), StarvedRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.GRYMN.get(), GrymnRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DULLAHAN.get(), DullahanRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.ELIAS.get(), EliasRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.SACRED_CULTIST.get(), SacredCultistRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.TEMPLAR.get(), TemplarRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.CINDER.get(), CinderRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.SACRED_PRIEST.get(), SacredPriestRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.ANDRAS.get(), AndrasRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.OSTARA.get(), OstaraRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.CAIN.get(), CainRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FLESH_GOLEM.get(), FleshGolemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.ZAELDRIS_SPEAR.get(), ZaeldrisSpearRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.EXCALIBUR_AWAKENING.get(), ExcaliburAwakeningRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.DRYING_RACK.get(), DryingRackRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.HELL_FORGE.get(), context -> new HellForgeFuelRenderer());
        event.registerBlockEntityRenderer(ModBlockEntityTypes.PANDORA_BOX.get(), context -> new PandoraBoxRenderer());
        event.registerBlockEntityRenderer(ModBlockEntityTypes.SACRED_ALTAR.get(), context -> new CelestialAltarRenderer());
        event.registerBlockEntityRenderer(ModBlockEntityTypes.GREAT_DOOR.get(), context -> new GreatDoorRenderer());
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
        event.registerItem(ZaeldrisPose.INSTANCE, ModItems.ZAELDRIS.get());
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
        // before Epic Fight builds its patched renderers (its AddLayers listener runs after client setup)
        if (net.neoforged.fml.ModList.get().isLoaded("epicfight")) {
            dev.zsskayr.merlins_inferno.compat.epicfight.EpicFightWingsCompat.register();
        }
        event.enqueueWork(() -> {
            Sheets.addWoodType(ModWoodTypes.ASHWOOD);
            Sheets.addWoodType(ModWoodTypes.HAUNTED);
            for (var bow : java.util.List.of(ModItems.ROWANWOOD_BOW, ModItems.DEMONBLOOD_BOW, ModItems.SERAPHIUM_BOW, ModItems.LUMINITE_BOW, ModItems.EBONY_BOW)) {
                ItemProperties.register(bow.get(), ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
                        entity == null || entity.getUseItem() != stack ? 0.0F : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F);
                ItemProperties.register(bow.get(), ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            }
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
            // Zaeldris mid-barrage swaps to a model turned end for end (models/item/zaeldris_throw.json): the trident throw pose
            // raises the arm overhead, which would otherwise point the blade backward.
            ItemProperties.register(ModItems.ZAELDRIS.get(), ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "barrage"),
                    (stack, level, entity, seed) -> entity != null && net.minecraft.world.item.ItemStack.matches(entity.getMainHandItem(), stack) && ZaeldrisPose.isBarraging(entity) ? 1.0F : 0.0F);
            // Excalibur's awakening: the floating sword turns from worn to true, frame by frame.
            ItemProperties.register(ModItems.EXCALIBUR_AWAKENING.get(), ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "awakening"),
                    (stack, level, entity, seed) -> ExcaliburAwakeningClient.morph());
            // The Star Seeker: the server writes the meteor into the stack's lodestone tracker; the pupil just follows it.
            ItemProperties.register(ModItems.STAR_SEEKER.get(), ResourceLocation.withDefaultNamespace("angle"),
                    (stack, level, entity, seed) -> {
                        if (!(entity instanceof LivingEntity holder)) {
                            return 0.0F;
                        }
                        var tracker = stack.get(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER);
                        var target = tracker == null ? java.util.Optional.<net.minecraft.core.GlobalPos>empty() : tracker.target();
                        if (target.isEmpty() || target.get().dimension() != holder.level().dimension()) {
                            return (holder.level().getGameTime() % 360L) / 360.0F;
                        }
                        var pos = target.get().pos();
                        return ChampionSeekerItem.getAngle(holder, new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
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
        // A boss shows its bar the moment the client starts tracking it, so with no target yet we
        // rescan every tick: the needle locks on as soon as the bar appears.
        boolean searching = cachedTarget == null && tick != lastScanTick;
        if (staleCache || searching || tick - lastScanTick >= RESCAN_INTERVAL_TICKS || tick < lastScanTick) {
            cachedTarget = ChampionSeekerItem.findNearestBoss(holder, SEARCH_RADIUS);
            lastScanTick = tick;
        }
        return cachedTarget;
    }
}
