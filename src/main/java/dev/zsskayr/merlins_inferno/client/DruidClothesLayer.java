package dev.zsskayr.merlins_inferno.client;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import dev.zsskayr.merlins_inferno.entity.DruidEntity;

/**
 * Draws the druids' own clothes ({@code druid_armour}: a different geometry on a different 128x128 texture than the 64x64
 * skin) over the body. GeckoLib animates only the skin model, so each armour bone copies the pose of the skin bone it
 * dresses - both skeletons share the same hierarchy and pivots (see {@code src/Models/druid/build_druid.py}), which makes
 * the copy exact. Native leather/Rowanwood equipment keeps these clothes; other armour replaces the occupied slot.
 */
public class DruidClothesLayer extends GeoRenderLayer<DruidEntity> {
    /** Armour bone -> the skin bone whose pose it copies (armour bones not listed inherit from their parent). */
    private static final Map<String, String> FOLLOWS = Map.ofEntries(
            Map.entry("root", "root"), Map.entry("hips", "hips"), Map.entry("torso", "torso"),
            Map.entry("armorHead", "head"), Map.entry("armorBody", "body"),
            Map.entry("armorRightArm", "right_arm"), Map.entry("armorLeftArm", "left_arm"),
            Map.entry("armorRightLeg", "right_leg"), Map.entry("armorRightBoot", "right_leg"),
            Map.entry("armorLeftLeg", "left_leg"), Map.entry("armorLeftBoot", "left_leg"));

    public DruidClothesLayer(GeoRenderer<DruidEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, DruidEntity druid, BakedGeoModel skin, RenderType renderType, MultiBufferSource bufferSource,
            VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (buffer == null || druid.isInvisible()) {
            return;
        }
        BakedGeoModel clothes = getGeoModel().getBakedModel(DruidModel.ARMOUR_MODEL);
        FOLLOWS.forEach((clothesBone, skinBone) -> {
            GeoBone from = skin.getBone(skinBone).orElse(null);
            GeoBone to = clothes.getBone(clothesBone).orElse(null);
            if (from != null && to != null) {
                to.updateRotation(from.getRotX(), from.getRotY(), from.getRotZ());
                to.updatePosition(from.getPosX(), from.getPosY(), from.getPosZ());
                to.updateScale(from.getScaleX(), from.getScaleY(), from.getScaleZ());
            }
        });
        boolean head = !druid.usesNativeClothing(EquipmentSlot.HEAD);
        boolean chest = !druid.usesNativeClothing(EquipmentSlot.CHEST);
        boolean legs = !druid.usesNativeClothing(EquipmentSlot.LEGS);
        boolean feet = !druid.usesNativeClothing(EquipmentSlot.FEET);
        hide(clothes, "armorHead", head);
        hide(clothes, "armorBody", chest);
        hide(clothes, "armorRightArm", chest);
        hide(clothes, "armorLeftArm", chest);
        hide(clothes, "armorWaist", legs);
        hide(clothes, "armorRightLeg", legs);
        hide(clothes, "armorLeftLeg", legs);
        hide(clothes, "armorRightBoot", feet);
        hide(clothes, "armorLeftBoot", feet);

        RenderType type = RenderType.entityCutoutNoCull(DruidModel.ARMOUR_TEXTURE);
        getRenderer().reRender(clothes, poseStack, bufferSource, druid, type, bufferSource.getBuffer(type), partialTick,
                packedLight, packedOverlay, -1);
    }

    private static void hide(BakedGeoModel clothes, String bone, boolean hidden) {
        clothes.getBone(bone).ifPresent(b -> {
            b.setHidden(hidden);
            b.setChildrenHidden(hidden);
        });
    }
}
