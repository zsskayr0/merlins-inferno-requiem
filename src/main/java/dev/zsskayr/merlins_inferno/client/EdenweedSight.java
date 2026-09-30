package dev.zsskayr.merlins_inferno.client;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * The Druidic Trance, client half: The Sight outlines hidden magical ores and chests through walls, and a green haze
 * creeps in from the edges of the screen. Every living creature around is outlined the same way (the outline is drawn here,
 * not through the entity glow, which shader packs tend to swallow), so only the entranced player sees anything.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class EdenweedSight {
    private static final int SCAN_RADIUS = 14;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int MAX_MARKED = 256;
    private static final double SIGHT_ENTITY_RADIUS = 32.0;
    /** Line thickness in pixels: thick enough to read under shader packs and at a distance. */
    private static final double LINE_WIDTH = 4.5;
    private static final ResourceLocation HAZE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/gui/druidic_haze.png");

    private static final List<BlockPos> MARKED = new ArrayList<>();

    private EdenweedSight() {
    }

    private static boolean entranced(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.hasEffect(ModEffects.DRUIDIC_TRANCE);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!entranced(minecraft) || minecraft.level == null) {
            MARKED.clear();
            return;
        }
        if (minecraft.player.tickCount % SCAN_INTERVAL_TICKS != 0) {
            return;
        }
        MARKED.clear();
        BlockPos centre = minecraft.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS), centre.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS))) {
            if (MARKED.size() >= MAX_MARKED) {
                break;
            }
            if (minecraft.level.getBlockState(pos).is(ModTags.Blocks.SIGHT_REVEALED)) {
                MARKED.add(pos.immutable());
            }
        }
    }

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || MARKED.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!entranced(minecraft)) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(SightLines.TYPE);
        for (BlockPos pos : MARKED) {
            AABB box = new AABB(pos).inflate(0.002).move(-camera.x, -camera.y, -camera.z);
            outline(poseStack, lines, box, 0.45F, 1.0F, 0.35F);
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double radiusSqr = SIGHT_ENTITY_RADIUS * SIGHT_ENTITY_RADIUS;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == minecraft.player || !living.isAlive() || living.isSpectator()
                    || entity.distanceToSqr(minecraft.player) > radiusSqr) {
                continue;
            }
            // Hostile things burn orange-red, everything else stays sacred green.
            boolean hostile = living instanceof Enemy;
            AABB box = living.getBoundingBox().move(living.getPosition(partial).subtract(living.position())).inflate(0.03)
                    .move(-camera.x, -camera.y, -camera.z);
            outline(poseStack, lines, box, hostile ? 1.0F : 0.45F, hostile ? 0.4F : 1.0F, hostile ? 0.2F : 0.35F);
        }
        buffers.endBatch(SightLines.TYPE);
    }

    /** A box drawn twice, the second a hair larger, so the line reads as one thick, solid stroke. */
    private static void outline(PoseStack poseStack, VertexConsumer lines, AABB box, float red, float green, float blue) {
        LevelRenderer.renderLineBox(poseStack, lines, box, red, green, blue, 1.0F);
        LevelRenderer.renderLineBox(poseStack, lines, box.inflate(0.012), red, green, blue, 1.0F);
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "druidic_haze"), (graphics, deltaTracker) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (!entranced(minecraft) || minecraft.options.hideGui) {
                return;
            }
            // A slow breathing pulse on the haze.
            float pulse = 0.8F + 0.2F * (float) Math.sin((minecraft.player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(false)) * 0.06);
            RenderSystem.enableBlend();
            // Additive, so the haze lightens the edges instead of dimming them - on a dark screen or under a shader pack it must glow, not smudge.
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            graphics.setColor(1.0F, 1.0F, 1.0F, pulse);
            graphics.blit(HAZE, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0.0F, 0.0F, 256, 256, 256, 256);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        });
    }

    /** Line boxes that ignore the depth buffer, so they show through walls. */
    private static final class SightLines extends RenderType {
        static final RenderType TYPE = RenderType.create("merlins_inferno_sight_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES, 1536, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.of(LINE_WIDTH)))
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setWriteMaskState(COLOR_WRITE)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .createCompositeState(false));

        private SightLines(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
                boolean sortOnUpload, Runnable setup, Runnable clear) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
        }
    }
}
