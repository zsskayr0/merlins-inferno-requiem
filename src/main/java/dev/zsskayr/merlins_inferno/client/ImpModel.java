package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.ImpEntity;

/**
 * The Imp's GeckoLib model: geometry {@code geo/imp.geo.json}, animations
 * {@code animations/imp.animation.json} (ground/air loops, transitions and attacks), texture
 * {@code textures/entity/imp/imp.png}. Bone names are the Portuguese ones from the Blockbench
 * source ({@code cabeca}, {@code braco_direito}, ...).
 */
public class ImpModel extends GeoModel<ImpEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/imp.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/imp/imp.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "animations/imp.animation.json");

    @Override
    public ResourceLocation getModelResource(ImpEntity imp) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ImpEntity imp) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ImpEntity imp) {
        return ANIMATION;
    }

    /** Adds the entity's head look on top of whatever the animation does to the head bone. */
    @Override
    public void setCustomAnimations(ImpEntity imp, long instanceId, AnimationState<ImpEntity> state) {
        super.setCustomAnimations(imp, instanceId, state);
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data == null) {
            return;
        }
        this.getBone("cabeca").ifPresent(head -> {
            head.setRotX(head.getRotX() + Mth.clamp(data.headPitch(), -40.0F, 40.0F) * Mth.DEG_TO_RAD);
            head.setRotY(head.getRotY() + Mth.clamp(data.netHeadYaw(), -60.0F, 60.0F) * Mth.DEG_TO_RAD);
        });
    }
}
