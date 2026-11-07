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
import dev.zsskayr.merlins_inferno.entity.FleshGolemEntity;

/** Draws the eyes and exposed heart at full brightness over the base texture. */
public class FleshGolemGlowLayer extends GeoRenderLayer<FleshGolemEntity> {
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID,
            "textures/entity/flesh_golem/flesh_golem_glow.png");

    public FleshGolemGlowLayer(GeoRenderer<FleshGolemEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, FleshGolemEntity golem, BakedGeoModel bakedModel, @Nullable RenderType renderType,
            MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (golem.isInvisible()) {
            return;
        }
        RenderType glow = RenderType.eyes(GLOW);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, golem, glow, bufferSource.getBuffer(glow), partialTick,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
    }
}
