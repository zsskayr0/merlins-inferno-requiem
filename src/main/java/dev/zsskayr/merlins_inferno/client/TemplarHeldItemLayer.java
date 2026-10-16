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

import dev.zsskayr.merlins_inferno.entity.TemplarEntity;

/**
 * Uses vanilla ItemInHandLayer's grip and third-person display context, after
 * converting the Y-up GeckoLib arm frame to the vanilla humanoid arm frame.
 */
public class TemplarHeldItemLayer extends BlockAndItemGeoLayer<TemplarEntity> {
    private static final String RIGHT_ARM = "bipedRightArm";
    public TemplarHeldItemLayer(GeoRenderer<TemplarEntity> renderer) {
        super(renderer);
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, TemplarEntity templar,
            MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // Put the grip 1.5 model pixels lower inside the custom gauntlet.
        poseStack.translate(0, -1.5 / 16.0, 0);
        // Vanilla's humanoid model uses Y-down/X-mirrored coordinates. Convert
        // this Y-up GeckoLib arm frame, then use ItemInHandLayer's exact grip.
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(1.0 / 16.0, .125, -.625);
        super.renderStackForBone(poseStack, bone, stack, templar, bufferSource, partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, TemplarEntity templar) {
        return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, TemplarEntity templar) {
        return RIGHT_ARM.equals(bone.getName()) ? templar.getMainHandItem() : null;
    }
}
