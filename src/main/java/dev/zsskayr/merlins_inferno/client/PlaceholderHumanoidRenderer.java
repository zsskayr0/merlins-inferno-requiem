package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * Placeholder look for mobs that have no model yet (Andras, Ostara, the Sacred Priest): vanilla's zombie layout,
 * optionally enlarged, with a tinted texture at {@code textures/entity/<name>/<name>.png}. Swap for a GeckoLib
 * renderer once the real model exists.
 */
public class PlaceholderHumanoidRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;
    private final float scale;

    public PlaceholderHumanoidRenderer(EntityRendererProvider.Context context, String name, float scale) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F * scale);
        this.texture = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/" + name + "/" + name + ".png");
        this.scale = scale;
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(this.scale, this.scale, this.scale);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}
