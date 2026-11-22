package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.SeraphiumArmorItem;

/** Player-worn Seraphium armour drawn with the 3D model built from {@code src/Models/Refactored Armors/Seraphium Armor}. */
public class SeraphiumArmorRenderer extends GeoArmorRenderer<SeraphiumArmorItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/seraphium_armour.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/seraphium_armour.png");

    public SeraphiumArmorRenderer() {
        super(new GeoModel<SeraphiumArmorItem>() {
            @Override
            public ResourceLocation getModelResource(SeraphiumArmorItem item) {
                return MODEL;
            }

            @Override
            public ResourceLocation getTextureResource(SeraphiumArmorItem item) {
                return TEXTURE;
            }

            @Override
            public ResourceLocation getAnimationResource(SeraphiumArmorItem item) {
                return null;
            }
        });
    }
}
