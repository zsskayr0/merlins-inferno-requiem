package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.DruidEntity;

/**
 * The Druid's GeckoLib model: {@code geo/druid_skin.geo.json} (a vanilla player-layout body, so any 64x64 Minecraft skin
 * fits {@code textures/entity/druid/druid_skin_N.png}) and {@code animations/druid.animation.json}. The clothing is a second
 * model ({@code druid_armour}) drawn by {@link DruidClothesLayer}. Sources and build scripts: {@code src/Models/druid}.
 */
public class DruidModel extends GeoModel<DruidEntity> {
    public static final ResourceLocation ARMOUR_MODEL = id("geo/druid_armour.geo.json");
    public static final ResourceLocation ARMOUR_TEXTURE = id("textures/entity/druid/druid_armour.png");
    private static final ResourceLocation MODEL = id("geo/druid_skin.geo.json");
    private static final ResourceLocation ANIMATION = id("animations/druid.animation.json");
    private static final ResourceLocation[] SKINS = {
            id("textures/entity/druid/druid_skin_0.png"),
            id("textures/entity/druid/druid_skin_1.png"),
            id("textures/entity/druid/druid_skin_2.png"),
    };

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, path);
    }

    @Override
    public ResourceLocation getModelResource(DruidEntity druid) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DruidEntity druid) {
        return SKINS[Mth.clamp(druid.getVariant(), 0, SKINS.length - 1)];
    }

    @Override
    public ResourceLocation getAnimationResource(DruidEntity druid) {
        return ANIMATION;
    }

    /** Adds the entity's head look on top of whatever the animation does to the head bone. */
    @Override
    public void setCustomAnimations(DruidEntity druid, long instanceId, AnimationState<DruidEntity> state) {
        super.setCustomAnimations(druid, instanceId, state);
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data == null) {
            return;
        }
        this.getBone("head").ifPresent(head -> {
            head.setRotX(head.getRotX() + Mth.clamp(data.headPitch(), -35.0F, 35.0F) * Mth.DEG_TO_RAD);
            head.setRotY(head.getRotY() + Mth.clamp(data.netHeadYaw(), -50.0F, 50.0F) * Mth.DEG_TO_RAD);
        });
        if (druid.isDeadOrDying() || druid.isTradingSynced() || druid.getMainHandItem().isEmpty()) return;
        var manager = druid.getAnimatableInstanceCache().getManagerForId(instanceId);
        var gesture = manager.getAnimationControllers().get("gesture");
        if (gesture != null && gesture.getAnimationState() != software.bernie.geckolib.animation.AnimationController.State.STOPPED) return;
        float attack = druid.getAttackAnim(state.getPartialTick());
        float yaw = Mth.sin(Mth.sqrt(attack) * Mth.TWO_PI) * .2F;
        float pitch = data.headPitch() * Mth.DEG_TO_RAD;
        float eased = 1 - (float)Math.pow(1 - attack, 4);
        float swing = Mth.sin(eased * Mth.PI) * 1.2F + Mth.sin(attack * Mth.PI) * -(pitch - .7F) * .75F;
        // Unlike the Templar, these arms inherit torso yaw and shoulder motion.
        getBone("torso").ifPresent(b -> b.setRotY(b.getRotY() - yaw));
        getBone("head").ifPresent(b -> b.setRotY(b.getRotY() + yaw));
        getBone("right_arm").ifPresent(b -> {
            b.setRotX(b.getRotX() * .5F + Mth.PI / 10 + swing);
            b.setRotY(-yaw);
            b.setRotZ(b.getRotZ() - Mth.sin(attack * Mth.PI) * .4F);
        });
        getBone("left_arm").ifPresent(b -> b.setRotX(b.getRotX() - yaw));
    }
}
