package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.WorshipperEntity;

/** Worshippers use vanilla's humanoid layout with their own robe texture (placeholder art). */
public class WorshipperRenderer extends HumanoidMobRenderer<WorshipperEntity, HumanoidModel<WorshipperEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/worshipper/worshipper.png");

    public WorshipperRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(WorshipperEntity entity) {
        return TEXTURE;
    }
}
