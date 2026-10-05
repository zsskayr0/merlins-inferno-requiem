package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import dev.zsskayr.merlins_inferno.entity.OstaraEntity;

/**
 * Renders {@link OstaraEntity} with {@link OstaraModel}: GeckoLib animates the bones, and each bone draws its triangle
 * mesh ({@link OstaraMeshes}) where GeckoLib would draw its cubes.
 */
public class OstaraRenderer extends GeoEntityRenderer<OstaraEntity> {
    /** The model is ~3.4 blocks tall (antlers included); scaled to fit the 2.2-block hitbox. */
    private static final float MODEL_SCALE = 0.65F;

    public OstaraRenderer(EntityRendererProvider.Context context) {
        super(context, new OstaraModel());
        this.withScale(MODEL_SCALE);
        this.shadowRadius = 0.6F;
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay,
            int colour) {
        if (!bone.isHidden()) {
            OstaraMeshes.render(bone.getName(), poseStack, buffer, packedLight, packedOverlay, colour);
        }
    }
}
