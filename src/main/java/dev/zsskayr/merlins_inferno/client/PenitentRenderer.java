package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.PenitentEntity;

/** The Penitent, drawn as an enlarged vanilla zombie skeleton with its own texture until it gets a proper model. */
public class PenitentRenderer extends HumanoidMobRenderer<PenitentEntity, HumanoidModel<PenitentEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/penitent/penitent.png");

    public PenitentRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.6F);
    }

    @Override
    protected void scale(PenitentEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.15F, 1.15F, 1.15F);
    }

    @Override
    public ResourceLocation getTextureLocation(PenitentEntity entity) {
        return TEXTURE;
    }
}
