package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.FleshGolemEntity;

/** The Flesh Golem's Blockbench geometry, animations and texture. */
public class FleshGolemModel extends GeoModel<FleshGolemEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/flesh_golem.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID,
            "textures/entity/flesh_golem/flesh_golem.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID,
            "animations/flesh_golem.animation.json");

    @Override
    public ResourceLocation getModelResource(FleshGolemEntity golem) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(FleshGolemEntity golem) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(FleshGolemEntity golem) {
        return ANIMATION;
    }
}
