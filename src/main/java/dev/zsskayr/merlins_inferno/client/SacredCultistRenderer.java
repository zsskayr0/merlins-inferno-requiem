package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.SacredCultistEntity;

/** Sacred Cultists use vanilla's humanoid layout with their own robe texture (placeholder art). */
public class SacredCultistRenderer extends HumanoidMobRenderer<SacredCultistEntity, HumanoidModel<SacredCultistEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/sacred_cultist/sacred_cultist.png");

    public SacredCultistRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(SacredCultistEntity entity) {
        return TEXTURE;
    }
}
