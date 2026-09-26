package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.DullahanEntity;

/**
 * The Dullahan's GeckoLib model: geometry {@code geo/dullahan.geo.json}, animations
 * {@code animations/dullahan.animation.json}, texture {@code textures/entity/dullahan/dullahan.png}
 * (128x128). The geometry is converted from the Blockbench project (bone names are the Portuguese ones
 * from it: {@code cabeca_carregada}, {@code espada_preview}, ...). It has no head bone - the Dullahan's
 * head is the one he carries - so there is no head-look to add on top of the animation.
 */
public class DullahanModel extends GeoModel<DullahanEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/dullahan.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/dullahan/dullahan.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "animations/dullahan.animation.json");

    @Override
    public ResourceLocation getModelResource(DullahanEntity dullahan) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DullahanEntity dullahan) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DullahanEntity dullahan) {
        return ANIMATION;
    }
}
