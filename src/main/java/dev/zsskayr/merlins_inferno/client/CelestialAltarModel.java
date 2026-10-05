package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.Util;

import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.blockentity.SacredAltarBlockEntity;

/** The Sacred Altar's GeckoLib model: {@code geo/celestial_altar.geo.json}, one atlas texture for altar and crystal. */
public class CelestialAltarModel extends GeoModel<SacredAltarBlockEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "geo/celestial_altar.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/block/celestial_altar.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "animations/celestial_altar.animation.json");

    /** The cross turns to a player this close (blocks), at this speed (radians per second). */
    private static final double CROSS_RANGE = 12.0;
    private static final float CROSS_TURN_SPEED = 3.0F;

    /** The cross faces the player: its flat side is turned towards them, whatever way the altar itself faces. */
    @Override
    public void setCustomAnimations(SacredAltarBlockEntity altar, long instanceId, AnimationState<SacredAltarBlockEntity> state) {
        var cross = getAnimationProcessor().getBone("cross");
        LocalPlayer player = Minecraft.getInstance().player;
        if (cross == null || player == null || altar.getLevel() == null) {
            return;
        }
        double dx = player.getX() - (altar.getBlockPos().getX() + 0.5);
        double dz = player.getZ() - (altar.getBlockPos().getZ() + 0.5);
        float target = 0.0F;
        if (altar.isAwakeClip(state.getPartialTick()) && dx * dx + dz * dz <= CROSS_RANGE * CROSS_RANGE) {
            float blockYaw = (float) Math.toRadians(CelestialAltarRenderer.blockYawDegrees(altar.getBlockState().getValue(dev.zsskayr.merlins_inferno.block.SacredAltarBlock.FACING)));
            target = (float) Math.atan2(dx, dz) - blockYaw;
        }
        long now = Util.getMillis();
        float dt = altar.crossClock == 0L ? 0.0F : Math.min((now - altar.crossClock) / 1000.0F, 0.1F);
        altar.crossClock = now;
        float diff = Mth.wrapDegrees((float) Math.toDegrees(target - altar.crossYaw));
        float step = CROSS_TURN_SPEED * dt;
        altar.crossYaw += Mth.clamp((float) Math.toRadians(diff), -step, step);
        cross.setRotY(altar.crossYaw);
    }

    @Override
    public ResourceLocation getModelResource(SacredAltarBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SacredAltarBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SacredAltarBlockEntity animatable) {
        return ANIMATION;
    }
}
