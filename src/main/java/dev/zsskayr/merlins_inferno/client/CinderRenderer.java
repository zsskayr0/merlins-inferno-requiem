package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import dev.zsskayr.merlins_inferno.entity.CinderEntity;
import dev.zsskayr.merlins_inferno.entity.cinder.CinderRig;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public final class CinderRenderer extends GeoEntityRenderer<CinderEntity> {
    public CinderRenderer(EntityRendererProvider.Context context) {
        super(context, new CinderModel());
        withScale(CinderRig.scale());
        shadowRadius = .45F;
    }
    @Override public void renderCubesOfBone(PoseStack stack, GeoBone bone, VertexConsumer buffer, int light, int overlay, int colour) {
        if (!bone.isHidden()) CinderMeshes.render(bone.getName(), stack, buffer, light, overlay, colour);
    }
    @Override public Color getRenderColor(CinderEntity entity, float partialTick, int light) {
        float cold = Mth.clamp(entity.coldTicks() / 600F, 0, 1);
        // Warm ivory -> soot. Preserve the base atlas and its alpha.
        float r = gradient(cold, 1, .78F, .43F, .19F);
        float g = gradient(cold, 1, .78F, .44F, .20F);
        float b = gradient(cold, 1, .80F, .47F, .23F);
        return Color.ofRGBA(r, g, b, entity.isInvisible() ? .15F : 1F);
    }
    private static float gradient(float t, float a, float b, float c, float d) {
        float f = t * 3;
        return f < 1 ? Mth.lerp(f, a, b) : f < 2 ? Mth.lerp(f - 1, b, c) : Mth.lerp(f - 2, c, d);
    }
    @Override protected float getDeathMaxRotation(CinderEntity entity) { return 0; }
    @Override protected void applyRotations(CinderEntity entity, PoseStack stack, float age, float bodyYaw, float partialTick, float nativeScale) {
        // The sampled body and its hurtboxes share the parent's heading, rather
        // than the biped body controller's delayed head/body separation.
        super.applyRotations(entity, stack, age, Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()), partialTick, nativeScale);
    }
}
