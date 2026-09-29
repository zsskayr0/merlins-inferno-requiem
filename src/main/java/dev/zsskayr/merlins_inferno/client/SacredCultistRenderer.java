package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import dev.zsskayr.merlins_inferno.entity.SacredCultistEntity;

/** Renders {@link SacredCultistEntity} with {@link CultistModel}, plus its held sword ({@link CultistHeldItemLayer}). */
public class SacredCultistRenderer extends GeoEntityRenderer<SacredCultistEntity> {
    public SacredCultistRenderer(EntityRendererProvider.Context context) {
        super(context, new CultistModel());
        this.shadowRadius = 0.5F;
        this.addRenderLayer(new CultistHeldItemLayer(this));
    }

    /** The death clip already collapses the body; GeckoLib's default extra tip-over would stack on it. */
    @Override
    protected float getDeathMaxRotation(SacredCultistEntity animatable) {
        return 0.0F;
    }
}
