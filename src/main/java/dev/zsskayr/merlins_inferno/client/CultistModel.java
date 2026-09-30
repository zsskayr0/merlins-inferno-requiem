package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.SacredCultistEntity;

/**
 * The Sacred Cultist's GeckoLib model: {@code geo/cultist.geo.json}, {@code animations/cultist.animation.json}
 * (idle, walk, run, attack, pray_start/pray/pray_end, death) and the texture {@code textures/entity/sacred_cultist/sacred_cultist.png}.
 */
public class CultistModel extends GeoModel<SacredCultistEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/cultist.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/sacred_cultist/sacred_cultist.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "animations/cultist.animation.json");

    @Override
    public ResourceLocation getModelResource(SacredCultistEntity cultist) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SacredCultistEntity cultist) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SacredCultistEntity cultist) {
        return ANIMATION;
    }
}
