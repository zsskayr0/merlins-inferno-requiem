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
    private static final double ELBOW_TO_SHOULDER = 4.0;

    public DruidHeldItemLayer(GeoRenderer<DruidEntity> renderer) {
        super(renderer);
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, DruidEntity druid, MultiBufferSource bufferSource,
            float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // The bone is the forearm, pivoting at the elbow (y 18 px), 4 px below the shoulder that vanilla's grip offsets
        // start from. Climb back up to the shoulder, then use the vanilla grip with a 1.5 px downward correction for the glove.
        poseStack.translate(0, (ELBOW_TO_SHOULDER - 1.5) / 16.0, 0);
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
        return "right_forearm".equals(bone.getName()) ? druid.getMainHandItem() : null;
    }
}
