package dev.zsskayr.merlins_inferno.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import dev.zsskayr.merlins_inferno.entity.DruidEntity;

/** The druid's main-hand weapon in the right fist, held like a player holds one (same adjustment as {@link CultistHeldItemLayer}). */
public class DruidHeldItemLayer extends BlockAndItemGeoLayer<DruidEntity> {
    /** From the shoulder pivot (22 px up) down to the fist (about 13 px). */
    private static final double FIST_DROP = 9.0 / 16.0;

    public DruidHeldItemLayer(GeoRenderer<DruidEntity> renderer) {
        super(renderer);
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, DruidEntity druid, MultiBufferSource bufferSource,
            float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0, -FIST_DROP, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        super.renderStackForBone(poseStack, bone, stack, druid, bufferSource, partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, DruidEntity druid) {
        return "right_arm".equals(bone.getName()) ? druid.getMainHandItem() : null;
    }
}
