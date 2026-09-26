package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.DullahanEntity;

/** Renders {@link DullahanEntity} with {@link DullahanModel}, drawn a bit taller than a player - "tall and dark". */
public class DullahanRenderer extends HumanoidMobRenderer<DullahanEntity, DullahanModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/dullahan/dullahan.png");
    private static final float SCALE = 1.2F;

    public DullahanRenderer(EntityRendererProvider.Context context) {
        super(context, new DullahanModel(context.bakeLayer(DullahanModel.LAYER_LOCATION)), 0.6F);
    }

    @Override
    protected void scale(DullahanEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(DullahanEntity entity) {
        return TEXTURE;
    }
}
