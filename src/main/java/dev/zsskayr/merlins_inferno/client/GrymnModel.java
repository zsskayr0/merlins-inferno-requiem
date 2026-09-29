package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.GrymnEntity;

/**
 * The Grymn's GeckoLib model: {@code geo/grymn.geo.json}, {@code animations/grymn.animation.json}
 * (idle, fly, attack) and the texture {@code textures/entity/grymn/grymn.png}.
 */
public class GrymnModel extends GeoModel<GrymnEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/grymn.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/grymn/grymn.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "animations/grymn.animation.json");

    @Override
    public ResourceLocation getModelResource(GrymnEntity grymn) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GrymnEntity grymn) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GrymnEntity grymn) {
        return ANIMATION;
    }
}
