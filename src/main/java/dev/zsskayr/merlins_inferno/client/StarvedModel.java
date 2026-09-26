package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.StarvedEntity;

/**
 * The Starved's GeckoLib model: {@code geo/starved.geo.json}, {@code animations/starved.animation.json}
 * (idle, walk, run, attack, hungry, ...) and the single-atlas texture {@code textures/entity/starved/starved.png}.
 */
public class StarvedModel extends GeoModel<StarvedEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/starved.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/starved/starved.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "animations/starved.animation.json");

    @Override
    public ResourceLocation getModelResource(StarvedEntity starved) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(StarvedEntity starved) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(StarvedEntity starved) {
        return ANIMATION;
    }
}
