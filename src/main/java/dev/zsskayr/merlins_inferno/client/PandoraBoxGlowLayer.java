package dev.zsskayr.merlins_inferno.client;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * The Box's purple glow, drawn full-bright on top of the model like the Grymn's eyes, but only while it is awake
 * (player near, hovered in a slot, or its screen open). The overlay texture holds nothing but the glowing pixels.
 */
public class PandoraBoxGlowLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID,
            "textures/block/pandora_box_glowmask.png");

    private final Predicate<T> awake;

    public PandoraBoxGlowLayer(GeoRenderer<T> renderer, Predicate<T> awake) {
        super(renderer);
        this.awake = awake;
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, @Nullable RenderType renderType,
            MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!this.awake.test(animatable)) {
            return;
        }
        RenderType glow = RenderType.eyes(GLOW);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow, bufferSource.getBuffer(glow), partialTick,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
    }
}
