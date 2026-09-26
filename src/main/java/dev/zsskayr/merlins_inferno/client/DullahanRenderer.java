package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import dev.zsskayr.merlins_inferno.entity.DullahanEntity;

/** Renders {@link DullahanEntity} with {@link DullahanModel}. The model is drawn at its authored size (no extra scale). */
public class DullahanRenderer extends GeoEntityRenderer<DullahanEntity> {
    public DullahanRenderer(EntityRendererProvider.Context context) {
        super(context, new DullahanModel());
        this.shadowRadius = 0.6F;
    }

    /** The death clip already collapses the body; GeckoLib's default extra 90-degree tip-over would stack on it. */
    @Override
    protected float getDeathMaxRotation(DullahanEntity animatable) {
        return 0.0F;
    }
}
