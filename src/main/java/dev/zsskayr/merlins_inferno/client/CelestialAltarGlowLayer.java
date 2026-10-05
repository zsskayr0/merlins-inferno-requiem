package dev.zsskayr.merlins_inferno.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.blockentity.SacredAltarBlockEntity;

/**
 * The altar's glow, drawn full-bright on top of the model with three masks laid out like the atlas: the cyan details
 * (always lit) and the two halves of the crystal, whose brightness alternates (one rises as the other falls) -
 * the resonance. During the activation the alternation speeds up, flashes at the moment Elias is woken and then
 * settles back to where the idle clip starts.
 */
public class CelestialAltarGlowLayer extends GeoRenderLayer<SacredAltarBlockEntity> {
    private static final ResourceLocation STATIC = texture("celestial_altar_glow_static");
    private static final ResourceLocation CORE = texture("celestial_altar_glow_a");
    private static final ResourceLocation SHARDS = texture("celestial_altar_glow_b");

    /** Idle: one resonance every 2.4 s, the same period the model's idle loop divides into. */
    private static final double IDLE_HZ = 1.0 / 2.4;
    private static final double BASE = 0.55;
    private static final double SWING = 0.45;

    public CelestialAltarGlowLayer(GeoRenderer<SacredAltarBlockEntity> renderer) {
        super(renderer);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/block/" + name + ".png");
    }

    private static double smooth(double x) {
        x = Math.max(0.0, Math.min(1.0, x));
        return x * x * (3 - 2 * x);
    }

    @Override
    public void render(PoseStack poseStack, SacredAltarBlockEntity altar, BakedGeoModel bakedModel, @Nullable RenderType renderType,
            MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        double lit = altar.glowScale(partialTick);
        if (lit <= 0.01) {
            return; // cold: no light at all
        }
        double t = altar.clipSeconds(partialTick);
        double swing;
        double burst = 0.0;
        if (altar.isInActivation(partialTick)) {
            // the same acceleration curve the spin uses, so the glow speeds up with it
            double phase = 2 * Math.PI * (IDLE_HZ * t + 0.08 * Math.pow(Math.max(0.0, t - 1.5), 2));
            swing = Math.sin(phase) * SWING * (1 - smooth((t - 8.6) / 0.4));
            burst = SWING * smooth((t - 8.8) / 0.2) * (1 - smooth((t - 9.4) / 1.5));
        } else {
            swing = Math.sin(2 * Math.PI * IDLE_HZ * t) * SWING;
        }
        draw(STATIC, (0.9 + burst * 0.2) * lit, poseStack, altar, bakedModel, bufferSource, partialTick);
        draw(CORE, (BASE + swing + burst) * lit, poseStack, altar, bakedModel, bufferSource, partialTick);
        draw(SHARDS, (BASE - swing + burst) * lit, poseStack, altar, bakedModel, bufferSource, partialTick);
    }

    private void draw(ResourceLocation mask, double brightness, PoseStack poseStack, SacredAltarBlockEntity altar, BakedGeoModel bakedModel,
            MultiBufferSource bufferSource, float partialTick) {
        int level = (int) Math.round(255 * Math.max(0.0, Math.min(1.0, brightness)));
        int colour = 0xFF000000 | level << 16 | level << 8 | level;
        RenderType glow = RenderType.eyes(mask);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, altar, glow, bufferSource.getBuffer(glow), partialTick,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, colour);
    }
}
