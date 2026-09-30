package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.EliasEntity;

/**
 * Elias's GeckoLib model: {@code geo/elias.geo.json}, {@code animations/elias.animation.json} (idle,
 * idle_hands_on_hips, walk, run, attack, pray_start/pray/pray_end, death, plus the fury moveset - idle_fury,
 * run_fury, attack_fury) and the texture {@code textures/entity/elias/elias.png}. His Seraphium Sword is baked
 * into the model itself, gripped one-handed normally and two-handed once he is in fury.
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
