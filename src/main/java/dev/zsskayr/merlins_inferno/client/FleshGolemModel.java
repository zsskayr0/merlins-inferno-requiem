package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.FleshGolemEntity;

/**
 * The Flesh Golem's model, ported 1:1 from the original MCreator export ({@code Modelfleshgolem2}, 81 cubes: horned
 * head, jaw, a ribbed torso and long clawed arms). Only its walk cycle is driven in code, like the original; the model
 * is about 3 blocks tall, hence the 1.2 x 3.0 hitbox in {@code ModEntityTypes}.
 */
public class FleshGolemModel extends EntityModel<FleshGolemEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "flesh_golem"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public FleshGolemModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4F, -29.4833F, -2.35F, 8F, 6F, 8F, new CubeDeformation(0F))
                .texOffs(0, 5).addBox(-4.1F, -25.2333F, -3.6F, 1F, 1F, 0F, new CubeDeformation(0F))
                .texOffs(0, 4).addBox(2.9F, -25.2333F, -3.6F, 1F, 1F, 0F, new CubeDeformation(0F))
                .texOffs(18, 14).addBox(-4F, -23.2833F, -1.35F, 8F, 0F, 6F, new CubeDeformation(0F)),
                PartPose.offset(0F, 6.4833F, -2.95F));
        PartDefinition hornR = head.addOrReplaceChild("hornR",
                CubeListBuilder.create()
                .texOffs(40, 46).addBox(-2F, -2F, -2F, 2F, 2F, 2F, new CubeDeformation(0.0002F)),
                PartPose.offsetAndRotation(-4F, -27.9833F, 2.45F, 0F, -0.1745F, 0.3054F));
        PartDefinition hornR2 = hornR.addOrReplaceChild("hornR2",
                CubeListBuilder.create()
                .texOffs(44, 34).addBox(-2F, -2F, -2F, 2F, 2F, 2F, new CubeDeformation(0.0001F)),
                PartPose.offsetAndRotation(-2F, 0F, 0F, 0F, -0.3491F, 0.2618F));
        PartDefinition hornR6 = hornR2.addOrReplaceChild("hornR6",
                CubeListBuilder.create()
                .texOffs(34, 44).addBox(-2F, -2F, -2F, 2F, 2F, 2F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(-2F, 0F, 0F, 0F, -0.3927F, 0.2618F));
        PartDefinition hornR3 = hornR6.addOrReplaceChild("hornR3",
                CubeListBuilder.create()
                .texOffs(50, 33).addBox(-2F, -1F, -1F, 2F, 1F, 1F, new CubeDeformation(0.0004F)),
                PartPose.offsetAndRotation(-2F, -0.5F, -0.5F, 0F, -0.3491F, 0.2618F));
        PartDefinition hornR4 = hornR3.addOrReplaceChild("hornR4",
                CubeListBuilder.create()
                .texOffs(50, 31).addBox(-2F, -1F, -1F, 2F, 1F, 1F, new CubeDeformation(0.0003F)),
                PartPose.offsetAndRotation(-2F, 0F, 0F, 0F, -0.3927F, 0.3054F));
        PartDefinition hornR5 = hornR4.addOrReplaceChild("hornR5",
                CubeListBuilder.create()
                .texOffs(50, 29).addBox(-2F, -1F, 0F, 2F, 1F, 1F, new CubeDeformation(0.0002F)),
                PartPose.offsetAndRotation(-2F, 0F, -1F, 0F, 0.3927F, 0.3054F));
        PartDefinition hornR7 = hornR5.addOrReplaceChild("hornR7",
                CubeListBuilder.create()
                .texOffs(50, 27).addBox(-2F, -1F, 0F, 2F, 1F, 1F, new CubeDeformation(0.0001F)),
                PartPose.offsetAndRotation(-2F, 0F, 0F, 0F, 0.3491F, 0.2618F));
        PartDefinition hornR8 = hornR7.addOrReplaceChild("hornR8",
                CubeListBuilder.create()
                .texOffs(50, 25).addBox(-2F, -1F, 0F, 2F, 1F, 1F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(-2F, 0F, 0F, 0F, 0.3927F, 0.2182F));
        PartDefinition hornL = head.addOrReplaceChild("hornL",
                CubeListBuilder.create()
                .texOffs(32, 20).addBox(6.5662F, -22.0281F, -2.8731F, 2F, 2F, 2F, new CubeDeformation(0.0002F)),
                PartPose.offsetAndRotation(4F, -6.9833F, 4.45F, 0F, 0.1745F, -0.3054F));
        PartDefinition hornL2 = hornL.addOrReplaceChild("hornL2",
                CubeListBuilder.create()
                .texOffs(0, 18).addBox(11.1296F, -19.6462F, 1.1218F, 2F, 2F, 2F, new CubeDeformation(0.0001F)),
                PartPose.offsetAndRotation(2F, 0F, 0F, 0F, 0.3491F, -0.2618F));
        PartDefinition hornL3 = hornL2.addOrReplaceChild("hornL3",
                CubeListBuilder.create()
                .texOffs(0, 14).addBox(12.9569F, -16.1643F, 6.7459F, 2F, 2F, 2F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(2F, 0F, 0F, 0F, 0.3927F, -0.2618F));
        PartDefinition hornR12 = hornL3.addOrReplaceChild("hornR12",
                CubeListBuilder.create()
                .texOffs(48, 42).addBox(12.2143F, -11.3282F, 12.7528F, 2F, 1F, 1F, new CubeDeformation(0.0004F)),
                PartPose.offsetAndRotation(2F, -0.5F, -0.5F, 0F, 0.3491F, -0.2618F));
        PartDefinition hornR9 = hornR12.addOrReplaceChild("hornR9",
                CubeListBuilder.create()
                .texOffs(48, 23).addBox(8.3686F, -7.1773F, 17.3523F, 2F, 1F, 1F, new CubeDeformation(0.0003F)),
                PartPose.offsetAndRotation(2F, 0F, 0F, 0F, 0.3927F, -0.3054F));
        PartDefinition hornR10 = hornR9.addOrReplaceChild("hornR10",
                CubeListBuilder.create()
                .texOffs(32, 11).addBox(16.113F, -4.3749F, 13.1902F, 2F, 1F, 1F, new CubeDeformation(0.0002F)),
                PartPose.offsetAndRotation(2F, 0F, -1F, 0F, -0.3927F, -0.3054F));
        PartDefinition hornR11 = hornR10.addOrReplaceChild("hornR11",
                CubeListBuilder.create()
                .texOffs(24, 20).addBox(19.9575F, -0.0895F, 6.7728F, 2F, 1F, 1F, new CubeDeformation(0.0001F)),
                PartPose.offsetAndRotation(2F, 0F, 0F, 0F, -0.3491F, -0.2618F));
        PartDefinition hornR13 = hornR11.addOrReplaceChild("hornR13",
                CubeListBuilder.create()
                .texOffs(24, 6).addBox(20.411F, 4.2085F, -1.1237F, 2F, 1F, 1F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(2F, 0F, 0F, 0F, -0.3927F, -0.2182F));
        PartDefinition jaw = head.addOrReplaceChild("jaw",
                CubeListBuilder.create()
                .texOffs(0, 14).addBox(-4.1F, -59.6704F, 3.0754F, 8F, 2F, 8F, new CubeDeformation(0F))
                .texOffs(32, 6).addBox(-4.1F, -61.6704F, 8.0754F, 8F, 2F, 3F, new CubeDeformation(0F))
                .texOffs(0, 1).addBox(-4.1F, -61.1704F, 3.0754F, 1F, 1F, 0F, new CubeDeformation(0F))
                .texOffs(0, 0).addBox(-2.6F, -60.1704F, 3.0754F, 1F, 0F, 0F, new CubeDeformation(0F))
                .texOffs(0, 0).addBox(-1.2F, -60.1704F, 3.0754F, 1F, 0F, 0F, new CubeDeformation(0F))
                .texOffs(0, 0).addBox(-0.2F, -60.1704F, 3.0754F, 1F, 0F, 0F, new CubeDeformation(0F))
                .texOffs(0, 0).addBox(1.4F, -60.1704F, 3.0754F, 1F, 0F, 0F, new CubeDeformation(0F))
                .texOffs(0, 0).addBox(2.9F, -61.1704F, 3.0754F, 1F, 1F, 0F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(0F, 36.5167F, 6.65F, 0.2182F, 0F, 0F));
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                .texOffs(16, 32).addBox(-3F, 6F, -2F, 6F, 6F, 4F, new CubeDeformation(0.001F))
                .texOffs(24, 0).addBox(-6F, 0F, -2F, 12F, 2F, 4F, new CubeDeformation(0.001F)),
                PartPose.offset(0F, -15F, 0F));
        PartDefinition bone3 = body.addOrReplaceChild("bone3",
                CubeListBuilder.create()
                .texOffs(16, 24).addBox(-4F, 2F, -2F, 8F, 4F, 4F, new CubeDeformation(0F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                .texOffs(40, 11).addBox(-11F, -1F, -1F, 2F, 10F, 2F, new CubeDeformation(0F))
                .texOffs(0, 4).addBox(-11F, 8F, -1F, 2F, 2F, 2F, new CubeDeformation(0.25F)),
                PartPose.offset(5F, -13F, 0F));
        PartDefinition bone2 = rightarm.addOrReplaceChild("bone2",
                CubeListBuilder.create()
                .texOffs(42, 23).addBox(-1F, 0F, -1F, 2F, 9F, 2F, new CubeDeformation(0F)),
                PartPose.offset(-10F, 9F, 0F));
        PartDefinition hand2 = bone2.addOrReplaceChild("hand2",
                CubeListBuilder.create(),
                PartPose.offset(-1F, 9F, 0.4F));
        PartDefinition rightarmsub1 = hand2.addOrReplaceChild("right_arm_sub_1",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarmsub2 = rightarmsub1.addOrReplaceChild("right_arm_sub_2",
                CubeListBuilder.create()
                .texOffs(24, 42).addBox(0F, 0F, -1.9F, 2F, 2F, 3F, new CubeDeformation(0.003F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone14 = hand2.addOrReplaceChild("bone14",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1F, 2F, 0.6F, 0F, 0F, 0.1745F));
        PartDefinition rightarmsub3 = bone14.addOrReplaceChild("right_arm_sub_3",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarmsub4 = rightarmsub3.addOrReplaceChild("right_arm_sub_4",
                CubeListBuilder.create()
                .texOffs(48, 46).addBox(-1F, 0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.001F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone15 = bone14.addOrReplaceChild("bone15",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-1F, 3F, 0F, 0F, 0F, -0.48F));
        PartDefinition rightarmsub5 = bone15.addOrReplaceChild("right_arm_sub_5",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarmsub8 = rightarmsub5.addOrReplaceChild("right_arm_sub_8",
                CubeListBuilder.create()
                .texOffs(48, 38).addBox(0F, 0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone16 = hand2.addOrReplaceChild("bone16",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1F, 2F, -0.4F, 0F, 0F, 0.2182F));
        PartDefinition rightarmsub7 = bone16.addOrReplaceChild("right_arm_sub_7",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarmsub6 = rightarmsub7.addOrReplaceChild("right_arm_sub_6",
                CubeListBuilder.create()
                .texOffs(36, 48).addBox(-1F, 0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.002F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone17 = bone16.addOrReplaceChild("bone17",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-1F, 3F, 0F, 0F, 0F, -0.3927F));
        PartDefinition rightarmsub9 = bone17.addOrReplaceChild("right_arm_sub_9",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarmsub14 = rightarmsub9.addOrReplaceChild("right_arm_sub_14",
                CubeListBuilder.create()
                .texOffs(32, 48).addBox(0F, 0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone18 = hand2.addOrReplaceChild("bone18",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1F, 2F, -1.4F, 0F, 0F, 0.2618F));
        PartDefinition rightarmsub11 = bone18.addOrReplaceChild("right_arm_sub_11",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarmsub10 = rightarmsub11.addOrReplaceChild("right_arm_sub_10",
                CubeListBuilder.create()
                .texOffs(48, 19).addBox(-1F, 0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.001F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone19 = bone18.addOrReplaceChild("bone19",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-1F, 3F, 0F, 0F, 0F, -0.3927F));
        PartDefinition rightarmsub13 = bone19.addOrReplaceChild("right_arm_sub_13",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarmsub20 = rightarmsub13.addOrReplaceChild("right_arm_sub_20",
                CubeListBuilder.create()
                .texOffs(48, 15).addBox(0F, 0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone20 = hand2.addOrReplaceChild("bone20",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.5F, 2F, -0.9F, -0.3054F, 0F, 0F));
        PartDefinition rightarmsub15 = bone20.addOrReplaceChild("right_arm_sub_15",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarmsub23 = rightarmsub15.addOrReplaceChild("right_arm_sub_23",
                CubeListBuilder.create()
                .texOffs(48, 11).addBox(0.5F, 0F, -1F, 1F, 3F, 1F, new CubeDeformation(0.002F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone21 = bone20.addOrReplaceChild("bone21",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1F, 3F, -1F, 0.4363F, 0F, 0F));
        PartDefinition rightarmsub17 = bone21.addOrReplaceChild("right_arm_sub_17",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightarmsub26 = rightarmsub17.addOrReplaceChild("right_arm_sub_26",
                CubeListBuilder.create()
                .texOffs(44, 38).addBox(-0.5F, 0F, 0F, 1F, 2F, 1F, new CubeDeformation(0F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition leftarm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                .texOffs(36, 32).addBox(9F, -1F, -1F, 2F, 10F, 2F, new CubeDeformation(0F))
                .texOffs(0, 0).addBox(9F, 8F, -1F, 2F, 2F, 2F, new CubeDeformation(0.25F)),
                PartPose.offset(-5F, -13F, 0F));
        PartDefinition bone4 = leftarm.addOrReplaceChild("bone4",
                CubeListBuilder.create(),
                PartPose.offset(10F, 9F, 0F));
        PartDefinition leftarmsub1 = bone4.addOrReplaceChild("left_arm_sub_1",
                CubeListBuilder.create()
                .texOffs(16, 42).addBox(-1F, 0F, -1F, 2F, 9F, 2F, new CubeDeformation(0.003F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition hand = bone4.addOrReplaceChild("hand",
                CubeListBuilder.create(),
                PartPose.offset(-1F, 9F, 0.4F));
        PartDefinition leftarmsub4 = hand.addOrReplaceChild("left_arm_sub_4",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition leftarmsub2 = leftarmsub4.addOrReplaceChild("left_arm_sub_2",
                CubeListBuilder.create()
                .texOffs(41, 41).addBox(0F, 0F, -1.9F, 2F, 2F, 3F, new CubeDeformation(0.003F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone9 = hand.addOrReplaceChild("bone9",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1F, 2F, 0.6F, 0F, 0F, -0.0873F));
        PartDefinition leftarmsub6 = bone9.addOrReplaceChild("left_arm_sub_6",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition leftarmsub11 = leftarmsub6.addOrReplaceChild("left_arm_sub_11",
                CubeListBuilder.create()
                .texOffs(28, 47).addBox(0F, 0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.001F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone11 = bone9.addOrReplaceChild("bone11",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1F, 3F, 0F, 0F, 0F, -0.1745F));
        PartDefinition leftarmsub8 = bone11.addOrReplaceChild("left_arm_sub_8",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition leftarmsub3 = leftarmsub8.addOrReplaceChild("left_arm_sub_3",
                CubeListBuilder.create()
                .texOffs(24, 47).addBox(-1F, 0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0F)),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.3927F));
        PartDefinition bone7 = hand.addOrReplaceChild("bone7",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1F, 2F, -0.4F, 0F, 0F, -0.1309F));
        PartDefinition leftarmsub10 = bone7.addOrReplaceChild("left_arm_sub_10",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition leftarmsub17 = leftarmsub10.addOrReplaceChild("left_arm_sub_17",
                CubeListBuilder.create()
                .texOffs(36, 24).addBox(0F, 0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.002F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone10 = bone7.addOrReplaceChild("bone10",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1F, 3F, 0F, 0F, 0F, 0.3054F));
        PartDefinition leftarmsub12 = bone10.addOrReplaceChild("left_arm_sub_12",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition leftarmsub5 = leftarmsub12.addOrReplaceChild("left_arm_sub_5",
                CubeListBuilder.create()
                .texOffs(32, 32).addBox(-1F, 0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone6 = hand.addOrReplaceChild("bone6",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1F, 2F, -1.4F, -0.0436F, 0F, -0.1745F));
        PartDefinition leftarmsub14 = bone6.addOrReplaceChild("left_arm_sub_14",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition leftarmsub23 = leftarmsub14.addOrReplaceChild("left_arm_sub_23",
                CubeListBuilder.create()
                .texOffs(16, 32).addBox(0F, 0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.001F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone8 = bone6.addOrReplaceChild("bone8",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1F, 3F, 0F, 0F, 0F, 0.1745F));
        PartDefinition leftarmsub16 = bone8.addOrReplaceChild("left_arm_sub_16",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition leftarmsub26 = leftarmsub16.addOrReplaceChild("left_arm_sub_26",
                CubeListBuilder.create()
                .texOffs(16, 24).addBox(-1F, 0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone12 = hand.addOrReplaceChild("bone12",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.5F, 2F, -0.9F, -0.0873F, 0F, 0F));
        PartDefinition leftarmsub18 = bone12.addOrReplaceChild("left_arm_sub_18",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition leftarmsub29 = leftarmsub18.addOrReplaceChild("left_arm_sub_29",
                CubeListBuilder.create()
                .texOffs(24, 0).addBox(-0.5F, 0F, -1F, 1F, 3F, 1F, new CubeDeformation(0.002F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition bone13 = bone12.addOrReplaceChild("bone13",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0F, 3F, -1F, 0.3491F, 0F, 0F));
        PartDefinition leftarmsub20 = bone13.addOrReplaceChild("left_arm_sub_20",
                CubeListBuilder.create(),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition leftarmsub32 = leftarmsub20.addOrReplaceChild("left_arm_sub_32",
                CubeListBuilder.create()
                .texOffs(31, 42).addBox(-0.5F, 0F, 0F, 1F, 2F, 1F, new CubeDeformation(0F)),
                PartPose.offset(0F, 0F, 0F));
        PartDefinition rightleg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                .texOffs(8, 24).addBox(-1F, 0F, -1F, 2F, 30F, 2F, new CubeDeformation(0F)),
                PartPose.offset(2F, -6F, 0F));
        PartDefinition leftleg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                .texOffs(0, 24).addBox(-1F, 0F, -1F, 2F, 30F, 2F, new CubeDeformation(0F)),
                PartPose.offset(-2F, -6F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(FleshGolemEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        this.rightLeg.xRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 1.0F) * -1.0F * limbSwingAmount;
        this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
        this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
