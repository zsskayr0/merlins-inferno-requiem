package dev.zsskayr.merlins_inferno.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.blockentity.GreatDoorBlockEntity;

/** The great doors' GeckoLib model: one clip set, one geo and texture per door (named after the block). */
public class GreatDoorModel extends GeoModel<GreatDoorBlockEntity> {
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "animations/great_door.animation.json");

    private static String name(GreatDoorBlockEntity door) {
        return BuiltInRegistries.BLOCK.getKey(door.getBlockState().getBlock()).getPath();
    }

    @Override
    public ResourceLocation getModelResource(GreatDoorBlockEntity door) {
        return ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/" + name(door) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GreatDoorBlockEntity door) {
        return ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/block/" + name(door) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GreatDoorBlockEntity door) {
        return ANIMATION;
    }
}
