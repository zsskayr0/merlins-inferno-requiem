package dev.zsskayr.merlins_inferno.client.elysium;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * El'ysium's sky: the normal sun and blue sky, no vanilla clouds (cloud height NaN, and {@link #renderClouds} draws
 * {@link ElysiumCloudSea} instead), and a bright, slightly whitened fog colour so the horizon and the space below it
 * read as luminous air rather than a dark void. Dimension type JSON: {@code "effects": "merlins_inferno:elysium"}.
 *
 * <p>The vanilla sky darkens below the horizon only when the camera is under {@code ClientLevelData.getHorizonHeight}
 * (63 for non-flat worlds); every island stands far above that and the cloud sea covers the view down, so looking down
 * from an island shows white clouds, then this bright fog colour, never black.
 */
public final class ElysiumSkyEffects extends DimensionSpecialEffects {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "elysium");

    /** The pale sky-blue white the fog is pulled towards. */
    private static final Vec3 AIR_WHITE = new Vec3(0.86, 0.92, 1.0);

    public ElysiumSkyEffects() {
        super(Float.NaN, false, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        Vec3 lit = fogColor.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
        return lit.lerp(AIR_WHITE.scale(Math.max(brightness, 0.6F)), 0.45);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY,
                                double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        ElysiumCloudSea.render(level, ticks + partialTick, partialTick, modelViewMatrix, projectionMatrix, camX, camY, camZ);
        return true;
    }
}
