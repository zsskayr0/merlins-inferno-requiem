package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import dev.zsskayr.merlins_inferno.entity.TemplarEntity;

/** Renders {@link TemplarEntity} with {@link TemplarModel}, plus its Seraphium sword ({@link TemplarHeldItemLayer}). */
public class TemplarRenderer extends GeoEntityRenderer<TemplarEntity> {
    public TemplarRenderer(EntityRendererProvider.Context context) {
        super(context, new TemplarModel());
        this.shadowRadius = 0.5F;
        this.addRenderLayer(new TemplarHeldItemLayer(this));
    }
}
