package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.FleshGolemEntity;

/** Renders {@link FleshGolemEntity} with {@link FleshGolemModel} and its original texture. */
public class FleshGolemRenderer extends MobRenderer<FleshGolemEntity, FleshGolemModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/flesh_golem/flesh_golem.png");

    public FleshGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new FleshGolemModel(context.bakeLayer(FleshGolemModel.LAYER_LOCATION)), 0.9F);
    }

    @Override
    public ResourceLocation getTextureLocation(FleshGolemEntity entity) {
        return TEXTURE;
    }
}
