package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.ZaeldrisSpear;

/**
 * Draws a spear of light as a projection of the lance: its silhouette with a plain glowing inside and a dark heart in the star
 * ({@code textures/entity/zaeldris_spear.png}), full bright and a little see-through. The sprite runs corner to corner (butt
 * bottom-left, tip top-right), so it is turned 45 degrees onto the flight axis; two crossed planes, like an arrow, keep it
 * visible from every side.
 */
public class ZaeldrisSpearRenderer extends EntityRenderer<ZaeldrisSpear> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID,
            "textures/entity/zaeldris_spear.png");
    private static final float SIZE = 1.3F;
    private static final int ALPHA = 225;

    public ZaeldrisSpearRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ZaeldrisSpear spear, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, spear.yRotO, spear.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, spear.xRotO, spear.getXRot())));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        float half = SIZE / 2.0F;
        for (int plane = 0; plane < 2; plane++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F * plane));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
            // Pull the sprite back along its diagonal so the tip, not the middle, sits where the spear is.
            poseStack.translate(-0.25F * SIZE, -0.25F * SIZE, 0.0F);
            PoseStack.Pose pose = poseStack.last();
            vertex(consumer, pose, -half, -half, 0.0F, 1.0F);
            vertex(consumer, pose, half, -half, 1.0F, 1.0F);
            vertex(consumer, pose, half, half, 1.0F, 0.0F);
            vertex(consumer, pose, -half, half, 0.0F, 0.0F);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(spear, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v) {
        consumer.addVertex(pose, x, y, 0.0F).setColor(255, 255, 255, ALPHA).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ZaeldrisSpear spear) {
        return TEXTURE;
    }
}
