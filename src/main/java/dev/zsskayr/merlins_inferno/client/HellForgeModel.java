package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * Converted from a raw Blockbench project file (a GeckoLib-format {@code .bbmodel}, never
 * exported to Java) rather than a clean Blockbench export - 38 cubes, translated by reading the
 * project JSON directly. Two things worth knowing if this ever needs re-generating from an
 * updated {@code .bbmodel}:
 * <ul>
 *     <li>{@code texOffs(u, v)} takes the RAW pixel coordinates from each cube's own
 *     {@code uv_offset}, unscaled - Java entity-model UV is always relative to whatever texture
 *     size is passed to {@link LayerDefinition#create}, here 256x256 to match the source texture,
 *     not the usual 64x64/normalized-to-16 assumption. (First pass at this divided by 16, which
 *     produced fractional {@code texOffs} values - that method only takes integers, which is what
 *     gave away the mistake.)</li>
 *     <li>The model's own coordinates already put the floor at y=0 and center the footprint on
 *     x=0/z=0, so {@link HellForgeBlockEntityRenderer} only needs to translate to the block's
 *     center and scale by 1/16 - no manual recentering.</li>
 * </ul>
 * All geometry is one flat "main" part plus 2 small rotated corner braces (the only cubes the
 * source file gave a rotation to) - nothing here is meant to animate.
 */
public class HellForgeModel extends Model {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "hell_forge"), "main");

    private final ModelPart root;

    public HellForgeModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        CubeListBuilder mainCubes = CubeListBuilder.create();
        mainCubes.texOffs(0, 36).addBox(6.0F, 2.0F, -13.0F, 12.0F, 20.0F, 25.0F);
        mainCubes.texOffs(74, 36).addBox(-18.0F, 2.0F, -13.0F, 12.0F, 20.0F, 25.0F);
        mainCubes.texOffs(0, 106).addBox(-6.0F, 2.0F, -8.0F, 12.0F, 17.0F, 20.0F);
        mainCubes.texOffs(148, 56).addBox(-9.0F, 2.0F, -16.0F, 3.0F, 11.0F, 3.0F);
        mainCubes.texOffs(52, 176).addBox(6.0F, 2.0F, -16.0F, 3.0F, 11.0F, 3.0F);
        mainCubes.texOffs(164, 168).addBox(-6.0F, 13.0F, -12.0F, 12.0F, 6.0F, 4.0F);
        mainCubes.texOffs(148, 70).addBox(-8.0F, 17.0F, -16.0F, 2.0F, 8.0F, 3.0F);
        mainCubes.texOffs(12, 185).addBox(6.0F, 17.0F, -16.0F, 2.0F, 8.0F, 3.0F);
        mainCubes.texOffs(0, 171).addBox(-6.0F, 17.0F, -15.0F, 12.0F, 2.0F, 3.0F);
        mainCubes.texOffs(160, 102).addBox(-6.0F, 11.0F, -15.0F, 12.0F, 2.0F, 7.0F);
        mainCubes.texOffs(30, 171).addBox(-6.0F, 25.0F, -16.0F, 12.0F, 2.0F, 3.0F);
        mainCubes.texOffs(64, 168).addBox(-6.0F, 19.0F, -2.0F, 12.0F, 6.0F, 14.0F);
        mainCubes.texOffs(84, 81).addBox(-6.0F, 25.0F, -13.0F, 12.0F, 5.0F, 26.0F);
        mainCubes.texOffs(64, 140).addBox(-14.0F, 24.0F, -13.0F, 8.0F, 2.0F, 26.0F);
        mainCubes.texOffs(0, 143).addBox(-12.0F, 26.0F, -13.0F, 6.0F, 2.0F, 26.0F);
        mainCubes.texOffs(148, 28).addBox(-10.0F, 28.0F, -13.0F, 4.0F, 2.0F, 26.0F);
        mainCubes.texOffs(160, 56).addBox(6.0F, 28.0F, -13.0F, 4.0F, 2.0F, 26.0F);
        mainCubes.texOffs(148, 0).addBox(6.0F, 26.0F, -13.0F, 6.0F, 2.0F, 26.0F);
        mainCubes.texOffs(132, 140).addBox(6.0F, 24.0F, -13.0F, 8.0F, 2.0F, 26.0F);
        mainCubes.texOffs(64, 112).addBox(6.0F, 22.0F, -13.0F, 10.0F, 2.0F, 26.0F);
        mainCubes.texOffs(0, 0).addBox(-20.0F, 0.0F, -17.0F, 40.0F, 2.0F, 34.0F);
        mainCubes.texOffs(0, 81).addBox(-9.0F, 30.0F, -12.0F, 18.0F, 1.0F, 24.0F);
        mainCubes.texOffs(116, 168).addBox(-6.0F, 31.0F, -2.0F, 12.0F, 8.0F, 12.0F);
        mainCubes.texOffs(160, 84).addBox(-7.0F, 35.0F, -3.0F, 14.0F, 4.0F, 14.0F);
        mainCubes.texOffs(0, 176).addBox(-6.0F, 18.0F, 12.0F, 12.0F, 5.0F, 1.0F);
        mainCubes.texOffs(26, 176).addBox(-6.0F, 11.0F, 12.0F, 12.0F, 5.0F, 1.0F);
        mainCubes.texOffs(164, 178).addBox(-6.0F, 4.0F, 12.0F, 12.0F, 5.0F, 1.0F);
        mainCubes.texOffs(164, 184).addBox(6.0F, 2.0F, 12.0F, 2.0F, 18.0F, 1.0F);
        mainCubes.texOffs(170, 184).addBox(16.0F, 2.0F, 12.0F, 2.0F, 18.0F, 1.0F);
        mainCubes.texOffs(176, 184).addBox(11.0F, 2.0F, 12.0F, 2.0F, 18.0F, 1.0F);
        mainCubes.texOffs(182, 184).addBox(-8.0F, 2.0F, 12.0F, 2.0F, 18.0F, 1.0F);
        mainCubes.texOffs(0, 185).addBox(-13.0F, 2.0F, 12.0F, 2.0F, 18.0F, 1.0F);
        mainCubes.texOffs(6, 185).addBox(-18.0F, 2.0F, 12.0F, 2.0F, 18.0F, 1.0F);
        mainCubes.texOffs(136, 112).addBox(-16.0F, 22.0F, -13.0F, 10.0F, 2.0F, 26.0F);
        mainCubes.texOffs(0, 182).addBox(-18.0F, 20.0F, 12.0F, 12.0F, 2.0F, 1.0F);
        mainCubes.texOffs(26, 182).addBox(6.0F, 20.0F, 12.0F, 12.0F, 2.0F, 1.0F);
        root.addOrReplaceChild("main", mainCubes, PartPose.ZERO);

        root.addOrReplaceChild("detail0", CubeListBuilder.create()
                        .texOffs(64, 106).addBox(-13.0F, -10.0F, -15.0F, 2.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(8.75F, 39.8F, -1.0F, 0.0F, 0.0F, 0.785398F));
        root.addOrReplaceChild("detail1", CubeListBuilder.create()
                        .texOffs(74, 106).addBox(-13.0F, -10.0F, -15.0F, 2.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(8.25F, 22.8F, -1.0F, 0.0F, 0.0F, -0.785398F));

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
