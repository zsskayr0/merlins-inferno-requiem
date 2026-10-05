package dev.zsskayr.merlins_inferno.client;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

import dev.zsskayr.merlins_inferno.blockentity.PandoraBoxBlockEntity;

/** Renders the placed Box. The model is 2.5 blocks wide as authored, so it is scaled to half size. */
public class PandoraBoxRenderer extends GeoBlockRenderer<PandoraBoxBlockEntity> {
    static final float MODEL_SCALE = 0.5F;

    public PandoraBoxRenderer() {
        super(new PandoraBoxModel<>());
        this.withScale(MODEL_SCALE);
        // textures/block/pandora_box_glowmask.png: the purple glow around the clasps.
        addRenderLayer(new PandoraBoxGlowLayer<>(this, PandoraBoxBlockEntity::isAwake));
    }
}
