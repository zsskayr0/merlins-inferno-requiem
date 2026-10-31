package dev.zsskayr.merlins_inferno.client.elysium;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.block.CloudSeaBlock;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;

/**
 * The sea of clouds under El'ysium's islands: a few stacked, slowly drifting horizontal sheets of a tiling texture, the
 * top one at {@link ElysiumWorld#CLOUD_SEA_Y}, drawn around the camera out to twice the render distance and faded out
 * towards the edge. It replaces vanilla's clouds in this dimension (those used to cut through the islands at y~192).
 *
 * <p>Vertices are camera-relative and drawn with the frustum (rotation) matrix the level renderer hands to cloud
 * rendering, exactly like vanilla clouds, so it works the same from {@code renderClouds} and from a render stage.
 */
@OnlyIn(Dist.CLIENT)
public final class ElysiumCloudSea {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/environment/cloud_sea.png");

    /** One sheet: height, opacity, brightness, blocks per texture repeat, drift in blocks per tick. */
    private record Layer(float y, float alpha, float shade, double texScale, double driftX, double driftZ) {}

    private static final Layer[] LAYERS = {
            new Layer(ElysiumWorld.CLOUD_SEA_Y - 18, 0.95F, 0.82F, 384.0, 0.006, 0.003),
            new Layer(ElysiumWorld.CLOUD_SEA_Y - 8, 0.85F, 0.91F, 320.0, -0.004, 0.007),
            new Layer(ElysiumWorld.CLOUD_SEA_Y, 0.80F, 1.00F, 256.0, 0.012, -0.005),
    };
    /** Grid cell in blocks (vertex spacing: alpha is per vertex, so this sets how smooth the fade is). */
    private static final int CELL = 32;

    private static VertexBuffer buffer;

    private ElysiumCloudSea() {
    }

    /**
     * @param time         game ticks plus partial tick (drift)
     * @param modelView    the frustum/rotation matrix of the level render (no translation)
     */
    public static void render(ClientLevel level, float time, float partialTick, Matrix4f modelView, Matrix4f projection,
                              double camX, double camY, double camZ) {
        ShaderInstance shader = GameRenderer.getPositionTexColorShader();
        if (shader == null || CloudSeaBlock.isShown()) {
            return;
        }
        int renderChunks = Minecraft.getInstance().options.getEffectiveRenderDistance();
        float radius = Mth.clamp(renderChunks * 16 * 2, 192, 1024);
        Vec3 tint = level.getCloudColor(partialTick);

        // far sheets first so the blending reads right from above (and from below, when falling through)
        Layer[] order = LAYERS.clone();
        java.util.Arrays.sort(order, (a, b) -> Double.compare(Math.abs(camY - b.y()), Math.abs(camY - a.y())));

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (Layer layer : order) {
            emitLayer(builder, layer, time, radius, tint, camX, camY, camZ);
        }
        MeshData mesh = builder.build();
        if (mesh == null) {
            return;
        }
        if (buffer == null || buffer.isInvalid()) {
            buffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        buffer.bind();
        buffer.upload(mesh); // closes the mesh
        buffer.drawWithShader(modelView, projection, shader);
        VertexBuffer.unbind();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void emitLayer(BufferBuilder builder, Layer layer, float time, float radius, Vec3 tint,
                                  double camX, double camY, double camZ) {
        double offX = layer.driftX() * time, offZ = layer.driftZ() * time;
        // keep UVs small: subtract the whole texture repeats under the camera
        double baseU = Math.floor((camX + offX) / layer.texScale()), baseV = Math.floor((camZ + offZ) / layer.texScale());
        float relY = (float) (layer.y() - camY);
        int r = (int) Mth.clamp(tint.x * layer.shade() * 255.0, 0, 255);
        int g = (int) Mth.clamp(tint.y * layer.shade() * 255.0, 0, 255);
        int b = (int) Mth.clamp(tint.z * layer.shade() * 255.0, 0, 255);

        int cx = Mth.floor(camX / CELL) * CELL, cz = Mth.floor(camZ / CELL) * CELL;
        int reach = (int) Math.ceil(radius / CELL) * CELL;
        for (int x = cx - reach; x < cx + reach; x += CELL) {
            for (int z = cz - reach; z < cz + reach; z += CELL) {
                float a00 = fade(x - camX, z - camZ, radius), a10 = fade(x + CELL - camX, z - camZ, radius);
                float a11 = fade(x + CELL - camX, z + CELL - camZ, radius), a01 = fade(x - camX, z + CELL - camZ, radius);
                if (a00 + a10 + a11 + a01 <= 0.0F) {
                    continue;
                }
                vertex(builder, layer, x, z, relY, camX, camZ, offX, offZ, baseU, baseV, r, g, b, a00);
                vertex(builder, layer, x, z + CELL, relY, camX, camZ, offX, offZ, baseU, baseV, r, g, b, a01);
                vertex(builder, layer, x + CELL, z + CELL, relY, camX, camZ, offX, offZ, baseU, baseV, r, g, b, a11);
                vertex(builder, layer, x + CELL, z, relY, camX, camZ, offX, offZ, baseU, baseV, r, g, b, a10);
            }
        }
    }

    private static void vertex(BufferBuilder builder, Layer layer, int x, int z, float relY, double camX, double camZ,
                               double offX, double offZ, double baseU, double baseV, int r, int g, int b, float fade) {
        float u = (float) ((x + offX) / layer.texScale() - baseU);
        float v = (float) ((z + offZ) / layer.texScale() - baseV);
        int a = (int) Mth.clamp(layer.alpha() * fade * 255.0F, 0, 255);
        builder.addVertex((float) (x - camX), relY, (float) (z - camZ)).setUv(u, v).setColor(r, g, b, a);
    }

    /** 1 near the camera, easing to 0 at {@code radius}. */
    private static float fade(double dx, double dz, float radius) {
        double d = Math.sqrt(dx * dx + dz * dz);
        double t = Mth.clamp((d - radius * 0.55) / (radius * 0.45), 0.0, 1.0);
        return (float) (1.0 - t * t * (3.0 - 2.0 * t));
    }
}
