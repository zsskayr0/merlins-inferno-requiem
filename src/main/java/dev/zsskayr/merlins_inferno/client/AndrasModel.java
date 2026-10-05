package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.AndrasEntity;

/**
 * Andras' GeckoLib model: {@code geo/andras.geo.json}, {@code animations/andras.animation.json} and the texture
 * {@code textures/entity/andras/andras.png}.
 */
public class AndrasModel extends GeoModel<AndrasEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/andras.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/andras/andras.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "animations/andras.animation.json");

    @Override
    public ResourceLocation getModelResource(AndrasEntity andras) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AndrasEntity andras) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AndrasEntity andras) {
        return ANIMATION;
    }
}
