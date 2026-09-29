package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import dev.zsskayr.merlins_inferno.entity.SacredPriestEntity;

/** Renders {@link SacredPriestEntity} with {@link SacredPriestModel}. */
public class SacredPriestRenderer extends GeoEntityRenderer<SacredPriestEntity> {
    public SacredPriestRenderer(EntityRendererProvider.Context context) {
        super(context, new SacredPriestModel());
        this.shadowRadius = 0.6F;
    }

    /** The death clip already collapses the body; GeckoLib's default extra tip-over would stack on it. */
    @Override
    protected float getDeathMaxRotation(SacredPriestEntity animatable) {
        return 0.0F;
    }
}
