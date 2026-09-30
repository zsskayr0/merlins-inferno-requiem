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
 * its right hand, held like a player holds one.
 * <p>
 * Two things GeckoLib does not do for us. The item is drawn at the bone's pivot, and the model's "Right Arm" pivot sits
 * at hip height rather than at the fist, so the layer hangs on the forearm bone (pivot at the elbow) and drops down to the
 * fist itself. And it applies no rotation, while the item's hand display transform expects the vanilla arm frame, so the
 * sword is turned {@code -90} degrees about X, the adjustment GeckoLib's own examples use, to point forward from the fist.
 */
public class CultistHeldItemLayer extends BlockAndItemGeoLayer<SacredCultistEntity> {
    private static final String RIGHT_HAND = "Forearm_R";
    /** From the elbow pivot (top of the ~7 px forearm) down to the middle of the fist: about 5.5 px. */
    private static final double FIST_DROP = 5.5 / 16.0;

    public CultistHeldItemLayer(GeoRenderer<SacredCultistEntity> renderer) {
        super(renderer);
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, SacredCultistEntity cultist,
            MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0, -FIST_DROP, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        super.renderStackForBone(poseStack, bone, stack, cultist, bufferSource, partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, SacredCultistEntity cultist) {
        return RIGHT_HAND.equals(bone.getName()) ? cultist.getMainHandItem() : null;
    }
}
