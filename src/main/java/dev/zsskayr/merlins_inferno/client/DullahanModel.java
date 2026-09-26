package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.DullahanEntity;

/**
 * The Dullahan: a plain biped whose own head is never drawn. Instead the left arm carries a
 * smaller copy of the head tucked at the hip, and the right hand holds a whip built from a few
 * short bone-coloured segments (a spine). Riding on the vanilla {@link HumanoidModel} keeps
 * walking, head-look and arm-swing animation for free - the trade-off is that the whip is a rigid
 * chain of boxes, not a flexing one, until a proper GeckoLib model replaces this.
 * <p>
 * Texture layout is vanilla's 64x64 skin; the whip segments read from the unused patch at
 * {@code (56, 16)}, the carried head from the normal head region.
 */
public class DullahanModel extends HumanoidModel<DullahanEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "dullahan"), "main");

    public DullahanModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // Carried head: a 6x6x6 shrink of the 8x8x8 head cube (deformation -1 on every side),
        // hanging at hip height in front of the left side of the body.
        PartDefinition leftArm = root.getChild("left_arm");
        leftArm.addOrReplaceChild("carried_head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(-1.0F)),
                PartPose.offsetAndRotation(-1.0F, 13.0F, -1.0F, 0.0F, 0.3F, 0.0F));

        // The whip: four 1x1x3 segments stepping forward and drooping from the right hand.
        PartDefinition rightArm = root.getChild("right_arm");
        float[][] segments = {{-1.0F, 10.0F, -1.0F}, {-1.0F, 10.6F, -4.0F}, {-1.0F, 11.6F, -7.0F}, {-1.0F, 13.0F, -10.0F}};
        for (int i = 0; i < segments.length; i++) {
            rightArm.addOrReplaceChild("whip_" + i,
                    CubeListBuilder.create().texOffs(56, 16).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F),
                    PartPose.offset(segments[i][0], segments[i][1], segments[i][2]));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(DullahanEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // Headless: nothing to draw for the real head (or its hat layer).
        this.head.visible = false;
        this.hat.visible = false;
        // The left arm stays clamped around the carried head instead of swinging.
        this.leftArm.xRot = -0.45F;
        this.leftArm.zRot = -0.05F;
        if (entity.isParalyzed()) {
            // Gold-struck: doubled over, whip arm limp.
            this.body.xRot = 0.35F;
            this.rightArm.xRot = 0.6F;
        }
    }
}
