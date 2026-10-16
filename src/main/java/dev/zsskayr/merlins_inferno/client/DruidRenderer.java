package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import dev.zsskayr.merlins_inferno.entity.DruidEntity;

/**
 * Renders {@link DruidEntity} with {@link DruidModel}. Layers, in order: their own clothes ({@link DruidClothesLayer}),
 * vanilla armour on top ({@link DruidArmorLayer}) and the held weapon ({@link DruidHeldItemLayer}).
 */
public class DruidRenderer extends GeoEntityRenderer<DruidEntity> {
    /** The model is 32 px (2 blocks) tall; vanilla humanoids render at 15/16 of that. */
    private static final float MODEL_SCALE = 0.9375F;

    public DruidRenderer(EntityRendererProvider.Context context) {
        super(context, new DruidModel());
        this.withScale(MODEL_SCALE);
        this.shadowRadius = 0.5F;
        this.addRenderLayer(new DruidClothesLayer(this));
        this.addRenderLayer(new DruidArmorLayer(this));
        this.addRenderLayer(new DruidHeldItemLayer(this));
    }

    /** The death clip already lays the body down; GeckoLib's default extra tip-over would stack on it. */
    @Override
    protected float getDeathMaxRotation(DruidEntity animatable) {
        return 0.0F;
    }
}
