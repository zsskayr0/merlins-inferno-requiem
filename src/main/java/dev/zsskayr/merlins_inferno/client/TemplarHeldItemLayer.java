package dev.zsskayr.merlins_inferno.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import dev.zsskayr.merlins_inferno.entity.TemplarEntity;

/**
 * Draws the Templar's Seraphium sword in its right hand. Same trick as {@link CultistHeldItemLayer}: the item is drawn at the
 * bone's pivot - here the shoulder of {@code bipedRightArm}, 12 px above the fist - so it is dropped down to the hand and turned
 * -90 degrees about X to point forward like a player's sword.
 */
public class TemplarHeldItemLayer extends BlockAndItemGeoLayer<TemplarEntity> {
    private static final String RIGHT_ARM = "bipedRightArm";
    /** From the shoulder pivot down to the middle of the fist. */
    private static final double FIST_DROP = 10.0 / 16.0;

    public TemplarHeldItemLayer(GeoRenderer<TemplarEntity> renderer) {
        super(renderer);
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, TemplarEntity templar,
            MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0, -FIST_DROP, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        super.renderStackForBone(poseStack, bone, stack, templar, bufferSource, partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, TemplarEntity templar) {
        return RIGHT_ARM.equals(bone.getName()) ? templar.getMainHandItem() : null;
    }
}
