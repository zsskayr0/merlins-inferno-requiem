package dev.zsskayr.merlins_inferno.compat.curios;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.model.EntityModel;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/** Draws the Salamander Bracelet as a metal band with a skull plate around the left wrist. Loaded only with Curios. */
public final class CuriosBraceletRenderer implements ICurioRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/curios/salamander_bracelet.png");

    private ModelPart classic;
    private ModelPart slim;

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> CuriosRendererRegistry.register(ModItems.SALAMANDER_BRACELET.get(), CuriosBraceletRenderer::new));
    }

    /** A plain 2px leather band around the wrist; {@code armWidth} is 4 or 3. */
    private static ModelPart bake(float armWidth) {
        float x0 = -1.5F;
        float xEnd = armWidth - 0.5F;
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("bracelet", CubeListBuilder.create()
                .texOffs(0, 0).addBox(x0, 7F, -2.5F, xEnd - x0 + 1F, 2F, 0.5F)
                .texOffs(0, 0).addBox(x0, 7F, 2F, xEnd - x0 + 1F, 2F, 0.5F)
                .texOffs(0, 0).addBox(x0, 7F, -2F, 0.5F, 2F, 4F)
                .texOffs(0, 0).addBox(xEnd + 0.5F, 7F, -2F, 0.5F, 2F, 4F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32).bakeRoot().getChild("bracelet");
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack,
            RenderLayerParent<T, M> parent, MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount,
            float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(parent.getModel() instanceof HumanoidModel<?> humanoid)) return;
        boolean isSlim = slotContext.entity() instanceof AbstractClientPlayer player && player.getSkin().model() == PlayerSkin.Model.SLIM;
        if (classic == null) {
            classic = bake(4F);
            slim = bake(3F);
        }
        poseStack.pushPose();
        humanoid.leftArm.translateAndRotate(poseStack);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        (isSlim ? slim : classic).render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
