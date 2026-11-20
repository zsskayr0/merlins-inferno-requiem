package dev.zsskayr.merlins_inferno.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.world.item.ItemStack;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import dev.zsskayr.merlins_inferno.entity.SacredCultistEntity;

/**
 * Shows the sword some cultists spawn with (30% iron, 8% Seraphium; see {@code SacredCultistEntity#finalizeSpawn}) in
 * its right hand, held exactly like a player holds one.
 * <p>
 * GeckoLib draws the item at the bone's pivot with no rotation, while the item's hand display transform expects vanilla's
 * arm frame: y pointing down the arm (the renderer scales by -1,-1,1) and the {@code ItemInHandLayer} adjustments applied
 * on top. This layer rebuilds that chain: back up from the elbow bone to the shoulder, turn into vanilla's frame
 * ({@code Rz 180}), then the same {@code Rx -90}, {@code Ry 180} and {@code (1/16, 1/8, -5/8)} translation vanilla uses.
 */
public class CultistHeldItemLayer extends BlockAndItemGeoLayer<SacredCultistEntity> {
    private static final String RIGHT_HAND = "Forearm_R";
    /** The Forearm_R pivot (elbow, y 18) sits 4 px below the Arm_R pivot (shoulder, y 22) that vanilla's item offsets start from. */
    private static final double ELBOW_TO_SHOULDER = 4.0 / 16.0;

    public CultistHeldItemLayer(GeoRenderer<SacredCultistEntity> renderer) {
        super(renderer);
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, SacredCultistEntity cultist,
            MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0, ELBOW_TO_SHOULDER, 0.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(1.0 / 16.0, 0.125, -0.625);
        super.renderStackForBone(poseStack, bone, stack, cultist, bufferSource, partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, SacredCultistEntity cultist) {
        return RIGHT_HAND.equals(bone.getName()) ? cultist.getMainHandItem() : null;
    }
}
