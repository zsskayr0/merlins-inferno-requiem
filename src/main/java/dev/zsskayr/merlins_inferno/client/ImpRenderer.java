package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import dev.zsskayr.merlins_inferno.entity.ImpEntity;

/** Renders {@link ImpEntity} with {@link ImpModel}; {@link ImpHeldItemsLayer} draws the birth sword and any stolen loot. */
public class ImpRenderer extends GeoEntityRenderer<ImpEntity> {
    /** The Blockbench model is ~1.3 blocks tall with wings; scaled down to match the small hitbox. */
    private static final float MODEL_SCALE = 0.6F;

    public ImpRenderer(EntityRendererProvider.Context context) {
        super(context, new ImpModel());
        this.withScale(MODEL_SCALE);
        this.shadowRadius = 0.3F;
        this.addRenderLayer(new ImpHeldItemsLayer(this));
    }
}
