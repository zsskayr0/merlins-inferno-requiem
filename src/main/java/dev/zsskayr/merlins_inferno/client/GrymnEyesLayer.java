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
import dev.zsskayr.merlins_inferno.entity.GrymnEntity;

/**
 * The Grymn's eyes: drawn full-bright on top of the model (ignoring light level), white while it lurks and red once it
 * is hunting a player, with a red circle that grows across each eye (and shrinks back) in between. The overlay textures
 * contain nothing but the eye pixels.
 */
public class GrymnEyesLayer extends GeoRenderLayer<GrymnEntity> {
    /** Frame 0 is all white, the last is all red; in between a red circle grows out of each eye's centre. */
    private static final int FRAMES = 8;
    private static final ResourceLocation[] EYES = new ResourceLocation[FRAMES + 1];

    static {
        for (int i = 0; i <= FRAMES; i++) {
            EYES[i] = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/grymn/grymn_eyes_" + i + ".png");
        }
    }

    public GrymnEyesLayer(GeoRenderer<GrymnEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, GrymnEntity grymn, BakedGeoModel bakedModel, @Nullable RenderType renderType,
            MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (grymn.isInvisible()) {
            return;
        }
        RenderType eyes = RenderType.eyes(EYES[Math.round(grymn.getEyeRed(partialTick) * FRAMES)]);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, grymn, eyes, bufferSource.getBuffer(eyes), partialTick,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
    }
}
