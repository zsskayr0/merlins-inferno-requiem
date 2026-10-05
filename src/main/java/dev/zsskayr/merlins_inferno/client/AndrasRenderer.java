package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import dev.zsskayr.merlins_inferno.entity.AndrasEntity;

/** Renders {@link AndrasEntity} with {@link AndrasModel}. */
public class AndrasRenderer extends GeoEntityRenderer<AndrasEntity> {
    /** The Blockbench model is ~10.5 blocks tall with its horns; scaled down to ~3 blocks to match the hitbox. */
    private static final float MODEL_SCALE = 0.3F;

    public AndrasRenderer(EntityRendererProvider.Context context) {
        super(context, new AndrasModel());
        this.withScale(MODEL_SCALE);
        this.shadowRadius = 1.0F;
    }

    /** The death clip already collapses the body; GeckoLib's default extra 90-degree tip-over would stack on it. */
    @Override
    protected float getDeathMaxRotation(AndrasEntity animatable) {
        return 0.0F;
    }
}
