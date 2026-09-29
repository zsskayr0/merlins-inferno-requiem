package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import dev.zsskayr.merlins_inferno.entity.GrymnEntity;

/** Renders {@link GrymnEntity} with {@link GrymnModel}, plus the glowing eyes ({@link GrymnEyesLayer}). */
public class GrymnRenderer extends GeoEntityRenderer<GrymnEntity> {
    public GrymnRenderer(EntityRendererProvider.Context context) {
        super(context, new GrymnModel());
        this.shadowRadius = 0.6F;
        this.addRenderLayer(new GrymnEyesLayer(this));
    }
}
