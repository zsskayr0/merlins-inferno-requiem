package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.SacredPriestEntity;

/**
 * The Sacred Priest's GeckoLib model: {@code geo/sacred_priest.geo.json}, {@code animations/sacred_priest.animation.json}
 * (idle, idle_hands_on_hips, walk, run, attack, pray_start/pray/pray_end, death) and the texture
 * {@code textures/entity/sacred_priest/sacred_priest.png}. His broadsword is baked into the model itself - it is
 * always part of him, not an equipped item.
 */
public class SacredPriestModel extends GeoModel<SacredPriestEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/sacred_priest.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/sacred_priest/sacred_priest.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "animations/sacred_priest.animation.json");

    @Override
    public ResourceLocation getModelResource(SacredPriestEntity priest) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SacredPriestEntity priest) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SacredPriestEntity priest) {
        return ANIMATION;
    }
}
