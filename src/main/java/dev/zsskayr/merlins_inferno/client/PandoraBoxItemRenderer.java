package dev.zsskayr.merlins_inferno.client;

import software.bernie.geckolib.renderer.GeoItemRenderer;

import dev.zsskayr.merlins_inferno.item.PandoraBoxItem;

/** Draws the Box in the hand, in the inventory and on the ground with the same model as the placed block. */
public class PandoraBoxItemRenderer extends GeoItemRenderer<PandoraBoxItem> {
    public PandoraBoxItemRenderer() {
        super(new PandoraBoxModel<>());
        this.withScale(PandoraBoxRenderer.MODEL_SCALE);
        addRenderLayer(new PandoraBoxGlowLayer<>(this, item -> PandoraBoxItem.hovered));
    }
}
