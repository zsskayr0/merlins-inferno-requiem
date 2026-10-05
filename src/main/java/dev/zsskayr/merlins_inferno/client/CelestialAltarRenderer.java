package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

import dev.zsskayr.merlins_inferno.blockentity.SacredAltarBlockEntity;

/** Renders the Sacred Altar at the size it was authored (about 3.5 blocks wide and 6 tall), with the crystal's glow on top. */
public class CelestialAltarRenderer extends GeoBlockRenderer<SacredAltarBlockEntity> {
    public CelestialAltarRenderer() {
        super(new CelestialAltarModel());
        addRenderLayer(new CelestialAltarGlowLayer(this));
    }

    /** The model's own front is a quarter turn off the block's facing, so the usual rotation is followed by this one. */
    static final float MODEL_OFFSET_DEGREES = 270.0F;

    /** The turn applied to the whole model for a block facing {@code facing}, in degrees. */
    public static float blockYawDegrees(Direction facing) {
        float facingDegrees = switch (facing) {
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
        return facingDegrees + MODEL_OFFSET_DEGREES;
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(blockYawDegrees(facing)));
    }

    /** The altar and its crystal are far bigger than the block they stand in: do not cull it with the block's own box. */
    @Override
    public AABB getRenderBoundingBox(SacredAltarBlockEntity altar) {
        return new AABB(altar.getBlockPos()).inflate(3.0, 0.0, 3.0).expandTowards(0.0, 7.0, 0.0);
    }
}
