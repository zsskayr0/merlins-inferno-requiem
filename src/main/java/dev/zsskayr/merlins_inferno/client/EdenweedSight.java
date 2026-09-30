package dev.zsskayr.merlins_inferno.client;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

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
 * creeps in from the edges of the screen. The Grove's creatures are outlined here too, through {@code mixin.MinecraftMixin},
 * so only the entranced player sees them glow.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class EdenweedSight {
    private static final int SCAN_RADIUS = 14;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int MAX_MARKED = 256;
    private static final double SIGHT_ENTITY_RADIUS = 32.0;
    private static final ResourceLocation HAZE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/gui/druidic_haze.png");

    private static final List<BlockPos> MARKED = new ArrayList<>();

    private EdenweedSight() {
    }

    private static boolean entranced(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.hasEffect(ModEffects.DRUIDIC_TRANCE);
    }

    /** Whether The Sight outlines this entity for the local player (called from {@code mixin.MinecraftMixin}). */
    public static boolean revealsEntity(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        return entranced(minecraft) && entity.getType().is(ModTags.EntityTypes.SIGHT_REVEALED)
                && entity.distanceToSqr(minecraft.player) <= SIGHT_ENTITY_RADIUS * SIGHT_ENTITY_RADIUS;
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
            LevelRenderer.renderLineBox(poseStack, lines, box, 0.35F, 1.0F, 0.3F, 0.9F);
        }
        buffers.endBatch(SightLines.TYPE);
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "druidic_haze"), (graphics, deltaTracker) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (!entranced(minecraft) || minecraft.options.hideGui) {
                return;
            }
            // A slow breathing pulse on the haze.
            float pulse = 0.55F + 0.15F * (float) Math.sin((minecraft.player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(false)) * 0.06);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.setColor(1.0F, 1.0F, 1.0F, pulse);
            graphics.blit(HAZE, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0.0F, 0.0F, 256, 256, 256, 256);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        });
    }

    /** Line boxes that ignore the depth buffer, so they show through walls. */
    private static final class SightLines extends RenderType {
        static final RenderType TYPE = RenderType.create("merlins_inferno_sight_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES, 1536, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.of(2.0)))
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
