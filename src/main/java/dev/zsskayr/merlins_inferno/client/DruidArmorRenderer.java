package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.RenderUtil;

import dev.zsskayr.merlins_inferno.item.DruidArmorItem;

/**
 * Player-worn Rowanwood armour drawn with the druids' clothes model. The clothes' extra {@code armorWaist} bone (the belt and
 * tabard under the hips) is not one of GeckoLib's eight armour bones, so it is shown only with the leggings and follows the body.
 */
public class DruidArmorRenderer extends GeoArmorRenderer<DruidArmorItem> {
    public DruidArmorRenderer() {
        super(new GeoModel<DruidArmorItem>() {
            @Override
            public ResourceLocation getModelResource(DruidArmorItem item) {
                return DruidModel.ARMOUR_MODEL;
            }

            @Override
            public ResourceLocation getTextureResource(DruidArmorItem item) {
                return DruidModel.ARMOUR_TEXTURE;
            }

            @Override
            public ResourceLocation getAnimationResource(DruidArmorItem item) {
                return null;
            }
        });
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        super.applyBoneVisibilityBySlot(slot);
        waist().ifPresent(bone -> setBoneVisible(bone, slot == EquipmentSlot.LEGS));
    }

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        super.applyBaseTransformations(baseModel);
        waist().ifPresent(bone -> RenderUtil.matchModelPartRot(baseModel.body, bone));
    }

    private java.util.Optional<GeoBone> waist() {
        return getGeoModel().getBone("armorWaist");
    }
}
