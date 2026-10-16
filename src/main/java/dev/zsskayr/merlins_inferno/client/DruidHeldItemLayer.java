package dev.zsskayr.merlins_inferno.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import dev.zsskayr.merlins_inferno.entity.DruidEntity;

/** The druid's main-hand weapon in the right fist, held like a player holds one (same adjustment as {@link CultistHeldItemLayer}). */
public class DruidHeldItemLayer extends BlockAndItemGeoLayer<DruidEntity> {
    public DruidHeldItemLayer(GeoRenderer<DruidEntity> renderer) {
        super(renderer);
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, DruidEntity druid, MultiBufferSource bufferSource,
            float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // This skin's shoulder is at 22 px, with a full 12 px arm, like the
        // vanilla player. Preserve its parent torso transform, then use the
        // vanilla grip with a 1.5 px downward correction for the glove.
        poseStack.translate(0, -1.5 / 16.0, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(1.0 / 16.0, .125, -.625);
        super.renderStackForBone(poseStack, bone, stack, druid, bufferSource, partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }
    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, DruidEntity druid) {
        return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, DruidEntity druid) {
        return "right_arm".equals(bone.getName()) ? druid.getMainHandItem() : null;
    }
}
