package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

import software.bernie.geckolib.model.GeoModel;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.TemplarEntity;

/**
 * The Templar's GeckoLib model: {@code geo/templar.geo.json} (an armor-set biped, from {@code templar-armor.bbmodel}),
 * {@code animations/templar.animation.json} (idle, walk, run, attack, death) and
 * {@code textures/entity/templar/templar.png}. The sword is the held item, drawn by {@link TemplarHeldItemLayer}.
 */
public class TemplarModel extends GeoModel<TemplarEntity> {
    @Override
    public void setCustomAnimations(TemplarEntity templar, long id, AnimationState<TemplarEntity> state) {
        super.setCustomAnimations(templar, id, state);
        if (templar.isDeadOrDying()) return;
        EntityModelData look = state.getData(DataTickets.ENTITY_MODEL_DATA);
        float headPitch = look == null ? 0 : Mth.clamp(look.headPitch(), -60, 60) * Mth.DEG_TO_RAD;
        getBone("bipedHead").ifPresent(b -> {
            b.setRotX(-headPitch);
            b.setRotY(look == null ? 0 : -Mth.clamp(look.netHeadYaw(), -75, 75) * Mth.DEG_TO_RAD);
        });
        float attack = templar.getAttackAnim(state.getPartialTick());
        float bodyYaw = Mth.sin(Mth.sqrt(attack) * Mth.TWO_PI) * .2F;
        float eased = 1 - (float)Math.pow(1 - attack, 4);
        float swing = Mth.sin(eased * Mth.PI) * 1.2F
                + Mth.sin(attack * Mth.PI) * -(headPitch - .7F) * .75F;
        // HumanoidModel's ITEM pose and setupAttackAnimation, converted to Y-up.
        getBone("bipedRightArm").ifPresent(b -> {
            b.setRotX(b.getRotX() * .5F + Mth.PI / 10 + swing);
            b.setRotY(-bodyYaw * 2);
            b.setRotZ(b.getRotZ() - Mth.sin(attack * Mth.PI) * .4F);
            b.setPosX((1 - Mth.cos(bodyYaw)) * 5);
            b.setPosZ(Mth.sin(bodyYaw) * 5);
        });
        getBone("bipedLeftArm").ifPresent(b -> {
            b.setRotY(-bodyYaw); b.setRotX(b.getRotX() - bodyYaw);
            b.setPosX(-(1 - Mth.cos(bodyYaw)) * 5);
            b.setPosZ(-Mth.sin(bodyYaw) * 5);
        });
        getBone("bipedBody").ifPresent(b -> b.setRotY(-bodyYaw));
        getBone("armorWaist").ifPresent(b -> b.setRotY(-bodyYaw));
    }
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/templar.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/templar/templar.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "animations/templar.animation.json");

    @Override
    public ResourceLocation getModelResource(TemplarEntity templar) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TemplarEntity templar) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TemplarEntity templar) {
        return ANIMATION;
    }
}
