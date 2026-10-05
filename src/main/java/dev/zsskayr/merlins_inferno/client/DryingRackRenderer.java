package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import dev.zsskayr.merlins_inferno.block.DryingRackBlock;
import dev.zsskayr.merlins_inferno.blockentity.DryingRackBlockEntity;

/** Draws the rack's three items flat on its front face, side by side like item frames. */
public class DryingRackRenderer implements BlockEntityRenderer<DryingRackBlockEntity> {
    /** Slot centres across the face, as offsets from the block centre (left to right). */
    private static final double[] SLOT_X = { -1.0 / 3.0, 0.0, 1.0 / 3.0 };
    private static final float ITEM_SCALE = 0.32F;

    private final BlockEntityRendererProvider.Context context;

    public DryingRackRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public void render(DryingRackBlockEntity rack, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        if (rack.getLevel() == null) {
            return;
        }
        Direction out = rack.getBlockState().getValue(DryingRackBlock.FACING).getOpposite();
        int light = LevelRenderer.getLightColor(rack.getLevel(), rack.getBlockPos().relative(out));
        Direction right = out.getCounterClockWise();
        for (int slot = 0; slot < DryingRackBlockEntity.SLOTS; slot++) {
            ItemStack stack = rack.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            // The front face is the block centre plane; nudge the item just off it, in the slab's upper half.
            poseStack.translate(0.5 + out.getStepX() * 0.03 + right.getStepX() * SLOT_X[slot], 0.75,
                    0.5 + out.getStepZ() * 0.03 + right.getStepZ() * SLOT_X[slot]);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - out.toYRot()));
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            this.context.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    poseStack, bufferSource, rack.getLevel(), (int) rack.getBlockPos().asLong() + slot);
            poseStack.popPose();
        }
    }
}
