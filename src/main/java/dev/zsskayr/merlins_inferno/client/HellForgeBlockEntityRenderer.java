package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity;

/**
 * Draws {@link HellForgeModel} centered on the block. The model's own coordinates already put the
 * floor at y=0 and center the footprint on x=0/z=0 (see {@link HellForgeModel}'s javadoc), so this
 * only needs a translate-to-center + the standard 1/16 model-unit-to-block-unit scale - no manual
 * recentering math.
 * <p>
 * {@link #getRenderBoundingBox} is widened well past the single-block default because the model
 * is ~2.5x2.5x2 blocks - without this, the renderer stops drawing as soon as the anchor block
 * itself scrolls out of the camera frustum, even while most of the (larger) model is still
 * on-screen.
 */
public class HellForgeBlockEntityRenderer implements BlockEntityRenderer<HellForgeBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/block/hell_forge.png");

    private final HellForgeModel model;

    public HellForgeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new HellForgeModel(context.bakeLayer(HellForgeModel.LAYER_LOCATION));
    }

    @Override
    public void render(HellForgeBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // No extra poseStack.scale(1/16,...) here on purpose - ModelPart already divides both its
        // own PartPose offsets and every cube vertex by 16 internally (see ModelPart.render()/
        // translateAndRotate(), literally "this.x / 16.0F" etc.), the same way every vanilla block
        // entity renderer relies on. An earlier version of this method added a manual 1/16 scale
        // on top of that, which is why the model rendered at 1/256 size (1/16 squared) instead of
        // the correct ~2.5 blocks wide (confirmed against a plain .obj export of the same model,
        // which stores vertices in unambiguous real block-space units).
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        // Same rotation idiom ChestRenderer uses, plus a 180 - the model's own front is actually
        // authored facing north (yaw 180), not south like the chest model, confirmed after the
        // structure came out facing away from whoever placed it (FACING itself, and therefore the
        // hitbox layout tied to it, was already correct - only this visual offset was wrong).
        if (blockEntity.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)) {
            float yRot = blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING).toYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(180 - yRot));
        }
        this.model.renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, packedOverlay, -1);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HellForgeBlockEntity blockEntity) {
        // Widened to match SCALE - roughly 2.5 blocks wide/deep, ~2.5 blocks tall.
        return new AABB(blockEntity.getBlockPos()).inflate(1.5, 0.0, 1.5).expandTowards(0.0, 3.0, 0.0);
    }
}
