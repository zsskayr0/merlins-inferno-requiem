package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.DruidEntity;

/**
 * Renders {@link DruidEntity} with {@link DruidModel} and picks one of the 3 skin variant texture
 * paths. {@code HumanoidMobRenderer} (rather than a plain {@code MobRenderer}) already adds its
 * own {@code ItemInHandLayer} (held weapon visible for free); the {@code HumanoidArmorLayer} below
 * is added on top for equipped armor - see {@link DruidModel}'s javadoc for why the model had to
 * become a real {@code HumanoidModel} first for either of these to work at all.
 */
public class DruidRenderer extends HumanoidMobRenderer<DruidEntity, DruidModel> {
    private static final ResourceLocation[] TEXTURES = {
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/druid/druid_0.png"),
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/druid/druid_1.png"),
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/druid/druid_2.png"),
    };

    public DruidRenderer(EntityRendererProvider.Context context) {
        super(context, new DruidModel(context.bakeLayer(DruidModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(DruidEntity entity) {
        return TEXTURES[entity.getVariant()];
    }
}
