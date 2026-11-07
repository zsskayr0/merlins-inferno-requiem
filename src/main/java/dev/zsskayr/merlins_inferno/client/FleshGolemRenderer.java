package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import dev.zsskayr.merlins_inferno.entity.FleshGolemEntity;

/** Renders the Flesh Golem's animated Blockbench model and its glow mask. */
public class FleshGolemRenderer extends GeoEntityRenderer<FleshGolemEntity> {
    public FleshGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new FleshGolemModel());
        this.withScale(0.6F);
        this.shadowRadius = 0.9F;
        this.addRenderLayer(new FleshGolemGlowLayer(this));
    }

    @Override
    protected float getDeathMaxRotation(FleshGolemEntity animatable) {
        return 0.0F;
    }
}
