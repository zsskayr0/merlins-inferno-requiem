package dev.zsskayr.merlins_inferno.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

import dev.zsskayr.merlins_inferno.entity.DruidEntity;

/**
 * Other equipped armour on the druid's skin-model bones; native leather/Rowanwood uses DruidClothesLayer. GeckoLib's own layer is built for bones modelled as
 * armour-sized cubes and rescales the vanilla piece to them; the druid's bones are the plain 8x12x4 body, so the piece keeps
 * vanilla's size and is only moved to the bone's pivot (the bone's pose is already on the matrix stack here). Body and legs
 * carry two slots (chest + leggings belt, leggings + boots), so those bones render twice.
 */
public class DruidArmorLayer extends ItemArmorGeoLayer<DruidEntity> {
    @Nullable
    private ItemStack current;

    public DruidArmorLayer(GeoRenderer<DruidEntity> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(PoseStack poseStack, DruidEntity druid, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
            VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        for (EquipmentSlot slot : slotsFor(bone.getName())) {
            ItemStack stack = druid.getItemBySlot(slot);
            if (!stack.isEmpty() && !druid.usesNativeClothing(slot)) {
                this.current = stack;
                super.renderForBone(poseStack, druid, bone, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            }
        }
        this.current = null;
    }

    private static EquipmentSlot[] slotsFor(String bone) {
        return switch (bone) {
            case "head" -> new EquipmentSlot[] {EquipmentSlot.HEAD};
            case "body" -> new EquipmentSlot[] {EquipmentSlot.CHEST, EquipmentSlot.LEGS};
            case "right_arm", "left_arm" -> new EquipmentSlot[] {EquipmentSlot.CHEST};
            case "right_leg", "left_leg" -> new EquipmentSlot[] {EquipmentSlot.LEGS, EquipmentSlot.FEET};
            default -> new EquipmentSlot[0];
        };
    }

    @Nullable
    @Override
    protected ItemStack getArmorItemForBone(GeoBone bone, DruidEntity druid) {
        return this.current;
    }

    @Override
    protected ModelPart getModelPartForBone(GeoBone bone, EquipmentSlot slot, ItemStack stack, DruidEntity druid, HumanoidModel<?> model) {
        return switch (bone.getName()) {
            case "head" -> model.head;
            case "right_arm" -> model.rightArm;
            case "left_arm" -> model.leftArm;
            case "right_leg" -> model.rightLeg;
            case "left_leg" -> model.leftLeg;
            default -> model.body;
        };
    }

    @Override
    protected void prepModelPartForRender(PoseStack poseStack, GeoBone bone, ModelPart part) {
        part.setPos(-bone.getPivotX(), -bone.getPivotY(), bone.getPivotZ());
        part.setRotation(0.0F, 0.0F, 0.0F);
    }
}
