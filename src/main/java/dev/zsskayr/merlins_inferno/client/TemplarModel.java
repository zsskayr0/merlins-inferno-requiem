package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.TemplarEntity;

/**
 * The Templar's GeckoLib model: {@code geo/templar.geo.json} (an armor-set biped, from {@code templar-armor.bbmodel}),
 * {@code animations/templar.animation.json} (idle, walk, run, attack, death) and
 * {@code textures/entity/templar/templar.png}. The sword is the held item, drawn by {@link TemplarHeldItemLayer}.
 */
public class TemplarModel extends GeoModel<TemplarEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/templar.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/templar/templar.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "animations/templar.animation.json");

    @Override
    public ResourceLocation getModelResource(TemplarEntity templar) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TemplarEntity templar) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TemplarEntity templar) {
        return ANIMATION;
    }
}
