package dev.zsskayr.merlins_inferno.client;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.CinderEntity;
import dev.zsskayr.merlins_inferno.entity.cinder.CinderRig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/** Bone poses use the same clock and samples as server collision, including cooling during attacks. */
public final class CinderModel extends GeoModel<CinderEntity> {
    private static ResourceLocation asset(String path) { return ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, path); }
    @Override public ResourceLocation getModelResource(CinderEntity entity) { return asset("geo/cinder.geo.json"); }
    @Override public ResourceLocation getTextureResource(CinderEntity entity) { return asset("textures/entity/cinder/cinder.png"); }
    @Override public ResourceLocation getAnimationResource(CinderEntity entity) { return asset("animations/cinder.animation.json"); }
    @Override public void setCustomAnimations(CinderEntity entity, long id, AnimationState<CinderEntity> state) {
        float[] pose = entity.animationPose(state.getPartialTick());
        var bones = CinderRig.bones();
        for (int i = 0; i < bones.length; i++) {
            int k = i * 6;
            getBone(bones[i].name()).ifPresent(bone -> {
                bone.setPosX(-pose[k]); bone.setPosY(pose[k + 1]); bone.setPosZ(pose[k + 2]);
                bone.setRotX(pose[k + 3] * Mth.DEG_TO_RAD);
                bone.setRotY(pose[k + 4] * Mth.DEG_TO_RAD);
                bone.setRotZ(pose[k + 5] * Mth.DEG_TO_RAD);
            });
        }
    }
}
