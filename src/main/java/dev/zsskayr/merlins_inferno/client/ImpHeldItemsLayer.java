package dev.zsskayr.merlins_inferno.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import dev.zsskayr.merlins_inferno.entity.ImpEntity;

/**
 * Shows what the imp holds: the birth sword in the right hand ({@code braco_direito}), stolen
 * loot in the left ({@code braco_esquerdo}) - the same equipment slots the entity drops from.
 * The hand offset is measured from the hand cube in the model file; tweak {@link #HAND_OFFSET_Y}
 * and friends if the items look off in game.
 */
public class ImpHeldItemsLayer extends BlockAndItemGeoLayer<ImpEntity> {
    private static final String RIGHT_ARM = "braco_direito";
    private static final String LEFT_ARM = "braco_esquerdo";
    /** From the arm pivot down to the hand, in blocks (model units / 16, x mirrored by GeckoLib). */
    private static final float HAND_OFFSET_X = 0.075F;
    private static final float HAND_OFFSET_Y = -0.45F;
    private static final float HAND_OFFSET_Z = 0.04F;
    private static final float ITEM_SCALE = 0.75F;

    public ImpHeldItemsLayer(GeoRenderer<ImpEntity> renderer) {
        super(renderer);
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, ImpEntity imp) {
        return switch (bone.getName()) {
            case RIGHT_ARM -> imp.getMainHandItem();
            case LEFT_ARM -> imp.getOffhandItem();
            default -> null;
        };
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, ImpEntity imp, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        float side = RIGHT_ARM.equals(bone.getName()) ? -1.0F : 1.0F;
        poseStack.translate(side * HAND_OFFSET_X, HAND_OFFSET_Y, HAND_OFFSET_Z);
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        super.renderStackForBone(poseStack, bone, stack, imp, bufferSource, partialTick, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
