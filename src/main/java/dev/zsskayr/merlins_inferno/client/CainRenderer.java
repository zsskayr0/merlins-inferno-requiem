package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.CainEntity;

/**
 * Renders {@link CainEntity}. His original texture is laid out exactly like a player skin (wide arms, jacket and sleeve
 * overlays), so the vanilla player mesh draws it as-is; {@code HumanoidMobRenderer} adds the held item, which is how the
 * Dark Coven shows in his hand. Drawn 1.2x as big as a player, matching his hitbox.
 */
public class CainRenderer extends HumanoidMobRenderer<CainEntity, PlayerModel<CainEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/cain/cain.png");
    private static final float SCALE = 1.2F;

    public CainRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.6F);
    }

    @Override
    protected void scale(CainEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(CainEntity entity) {
        return TEXTURE;
    }
}
