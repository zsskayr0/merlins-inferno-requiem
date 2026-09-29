package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

import dev.zsskayr.merlins_inferno.entity.EliasEntity;

/** Renders {@link EliasEntity} with {@link EliasModel}. His Seraphium Sword is baked into the model, not a held item. */
public class EliasRenderer extends GeoEntityRenderer<EliasEntity> {
    public EliasRenderer(EntityRendererProvider.Context context) {
        super(context, new EliasModel());
        this.shadowRadius = 1.0F;
    }

    /** The death clip already collapses the body; GeckoLib's default extra tip-over would stack on it. */
    @Override
    protected float getDeathMaxRotation(EliasEntity animatable) {
        return 0.0F;
    }
}
