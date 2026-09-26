package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import dev.zsskayr.merlins_inferno.entity.StarvedEntity;

/** Renders {@link StarvedEntity} with {@link StarvedModel}. */
public class StarvedRenderer extends GeoEntityRenderer<StarvedEntity> {
    public StarvedRenderer(EntityRendererProvider.Context context) {
        super(context, new StarvedModel());
        this.shadowRadius = 1.0F;
    }
}
