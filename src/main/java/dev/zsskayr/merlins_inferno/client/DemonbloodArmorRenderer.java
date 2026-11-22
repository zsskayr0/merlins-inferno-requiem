package dev.zsskayr.merlins_inferno.client;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.DemonbloodArmorItem;

/** Player-worn Demonblood armour drawn with the 3D model built from {@code src/Models/Refactored Armors/Demonblood Armor}. */
public class DemonbloodArmorRenderer extends GeoArmorRenderer<DemonbloodArmorItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/demonblood_armour.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/demonblood_armour.png");

    public DemonbloodArmorRenderer() {
        super(new GeoModel<DemonbloodArmorItem>() {
            @Override
            public ResourceLocation getModelResource(DemonbloodArmorItem item) {
                return MODEL;
            }

            @Override
            public ResourceLocation getTextureResource(DemonbloodArmorItem item) {
                return TEXTURE;
            }

            @Override
            public ResourceLocation getAnimationResource(DemonbloodArmorItem item) {
                return null;
            }
        });
    }
}
