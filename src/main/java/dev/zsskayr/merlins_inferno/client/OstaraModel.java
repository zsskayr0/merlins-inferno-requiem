package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.OstaraEntity;

/**
 * Ostara's GeckoLib model: {@code geo/ostara.geo.json} (the bone hierarchy only), {@code animations/ostara.animation.json}
 * (idle, idle_risadinha, attack, plus talk, fly, fly_attack, pousar, land and risadinha for later) and the texture atlas
 * {@code textures/entity/ostara/ostara.png}. Her body is made of triangle meshes, which GeckoLib's cube format cannot
 * hold: {@link OstaraMeshes} carries them and {@link OstaraRenderer} draws them on GeckoLib's animated bones.
 * Both files are baked from the .bbmodel by {@code tools/bake_ostara.py}.
 */
public class OstaraModel extends GeoModel<OstaraEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/ostara.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/ostara/ostara.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "animations/ostara.animation.json");

    @Override
    public ResourceLocation getModelResource(OstaraEntity ostara) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(OstaraEntity ostara) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(OstaraEntity ostara) {
        return ANIMATION;
    }

    /** Adds the entity's head look on top of whatever the animation does to the head bone. */
    @Override
    public void setCustomAnimations(OstaraEntity ostara, long instanceId, AnimationState<OstaraEntity> state) {
        super.setCustomAnimations(ostara, instanceId, state);
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data == null) {
            return;
        }
        this.getBone("head").ifPresent(head -> {
            head.setRotX(head.getRotX() + Mth.clamp(data.headPitch(), -35.0F, 35.0F) * Mth.DEG_TO_RAD);
            head.setRotY(head.getRotY() + Mth.clamp(data.netHeadYaw(), -50.0F, 50.0F) * Mth.DEG_TO_RAD);
        });
    }
}
