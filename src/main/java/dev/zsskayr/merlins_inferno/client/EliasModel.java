package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.EliasEntity;

/**
 * Elias's GeckoLib model (the "mk2" weary knight): {@code geo/elias.geo.json}, {@code animations/elias.animation.json}
 * (idle, walk, run, hurt, death and six attacks - light right/left/dual, heavy single/dual/spin; all named
 * {@code animation.elias_mk2.*}) and the texture {@code textures/entity/elias/elias.png}. Both swords are part of the
 * model, on their own follow/yaw/pitch/roll bones so the animations can aim them freely.
 */
public class EliasModel extends GeoModel<EliasEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/elias.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/elias/elias.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "animations/elias.animation.json");

    @Override
    public ResourceLocation getModelResource(EliasEntity elias) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(EliasEntity elias) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(EliasEntity elias) {
        return ANIMATION;
    }
}
