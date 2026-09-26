package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.UndeadHorseRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

/**
 * Draws the Dullahan's steed as vanilla's skeleton horse. {@code UndeadHorseRenderer} picks its texture
 * from a map keyed by the two vanilla entity types (which returns null for ours), so the texture is
 * supplied here - to be replaced by a custom model/texture of its own later.
 */
public class DullahanSteedRenderer extends UndeadHorseRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_skeleton.png");

    public DullahanSteedRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.SKELETON_HORSE);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractHorse entity) {
        return TEXTURE;
    }
}
