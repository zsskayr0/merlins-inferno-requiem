package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import dev.zsskayr.merlins_inferno.block.HellForgeControllerBlock;
import dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity;

/** Draws the stored fuel as a glowing liquid pooled in the open interior of an expanded Hell Forge. */
public class HellForgeFuelRenderer implements BlockEntityRenderer<HellForgeBlockEntity> {
    private static final float INSET = 0.02F;
    private static final int LIGHT = 0xF000F0;

    @Override
    public void render(HellForgeBlockEntity forge, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        int r = forge.renderRadius();
        if (r < 2 || forge.getLevel() == null) return;
        float fill = forge.animatedFill(forge.renderFill() / 128.0F);
        if (fill < 0.004F) return;
        Direction facing = forge.getBlockState().getValue(HellForgeControllerBlock.FACING);
        // The Controller sits on the front face's bottom row; the interior spans (r - 1) around the Core.
        int span = r - 1;
        float cx = -facing.getStepX() * r, cz = -facing.getStepZ() * r;
        float time = forge.getLevel().getGameTime() + partialTick;
        draw(bufferSource, poseStack.last(), cx - span, cx + span + 1, 1, cz - span, cz + span + 1,
                fill, time, forge.renderKind());
    }

    /** Draws the pool inside the box [x0,x1] x [y0,y0+depth] x [z0,z1]; kind 2 is the darker ember tint. */
    public static void draw(MultiBufferSource bufferSource, PoseStack.Pose pose, float x0, float x1, float y0,
            float z0, float z1, float fill, float time, int kind) {
        // The side walls sit on the shell's inner plane (slightly past it), so they stay in front of the
        // catalyst blocks in the end layers and the pool covers them instead of hiding behind them.
        float minX = x0 - 0.01F, maxX = x1 + 0.01F, minZ = z0 - 0.01F, maxZ = z1 + 0.01F;
        float minY = y0 + INSET;
        float depth = x1 - x0;
        float topY = minY + (depth - 2 * INSET) * fill;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.withDefaultNamespace("block/lava_still"));
        VertexConsumer buf = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        float red = 1, green = 1, blue = 1;
        if (kind == 2) { red = 0.85F; green = 0.45F; blue = 0.3F; }
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        float vTop = v0 + (v1 - v0) * Math.min(1, (topY - minY) / depth);
        surface(buf, pose, minX, maxX, minZ, maxZ, topY, time, u0, u1, v0, v1, red, green, blue);
        wall(buf, pose, maxX, minZ, minX, minZ, minY, topY, 0, 0, -1, u0, u1, v0, v1, red, green, blue);
        wall(buf, pose, minX, maxZ, maxX, maxZ, minY, topY, 0, 0, 1, u0, u1, v0, v1, red, green, blue);
        wall(buf, pose, minX, minZ, minX, maxZ, minY, topY, -1, 0, 0, u0, u1, v0, v1, red, green, blue);
        wall(buf, pose, maxX, maxZ, maxX, minZ, minY, topY, 1, 0, 0, u0, u1, v0, v1, red, green, blue);
    }

    /** A wall from (px,pz) to (qx,qz) cut into one-block tiles so the lava texture keeps its pixel size. */
    private static void wall(VertexConsumer b, PoseStack.Pose pose, float px, float pz, float qx, float qz, float minY, float topY,
            float nx, float ny, float nz, float u0, float u1, float v0, float v1, float r, float g, float bl) {
        float len = Math.abs(qx - px) + Math.abs(qz - pz);
        int n = Math.max(1, (int) Math.ceil(len - 1.0E-3F));
        for (int k = 0; k < n; k++) {
            float ta = (float) k / n, tb = (float) (k + 1) / n;
            float ax = px + (qx - px) * ta, az = pz + (qz - pz) * ta, bx = px + (qx - px) * tb, bz = pz + (qz - pz) * tb;
            for (float y = minY; y < topY - 1.0E-4F; y += 1) {
                float y2 = Math.min(topY, y + 1);
                float vt = v1 - (v1 - v0) * (y2 - y);
                quad(b, pose, ax, y, az, bx, y, bz, bx, y2, bz, ax, y2, az, nx, ny, nz, u0, u1, vt, v1, r, g, bl);
            }
        }
    }

    /** A rippled top made of quarter-block cells; ripples fade out at the walls so the edges stay sealed. */
    private static void surface(VertexConsumer b, PoseStack.Pose pose, float minX, float maxX, float minZ, float maxZ,
            float topY, float time, float u0, float u1, float v0, float v1, float r, float g, float bl) {
        int nx = Math.max(1, Math.round((maxX - minX) * 4)), nz = Math.max(1, Math.round((maxZ - minZ) * 4));
        for (int i = 0; i < nx; i++) for (int j = 0; j < nz; j++) {
            float xa = minX + (maxX - minX) * i / nx, xb = minX + (maxX - minX) * (i + 1) / nx;
            float za = minZ + (maxZ - minZ) * j / nz, zb = minZ + (maxZ - minZ) * (j + 1) / nz;
            float ya = ripple(xa, za, minX, maxX, minZ, maxZ, time) + topY, yb = ripple(xb, za, minX, maxX, minZ, maxZ, time) + topY;
            float yc = ripple(xb, zb, minX, maxX, minZ, maxZ, time) + topY, yd = ripple(xa, zb, minX, maxX, minZ, maxZ, time) + topY;
            float ua = u0 + (u1 - u0) * (i % 4) / 4F, ub = u0 + (u1 - u0) * (i % 4 + 1) / 4F;
            float va = v0 + (v1 - v0) * (j % 4) / 4F, vb = v0 + (v1 - v0) * (j % 4 + 1) / 4F;
            vertex(b, pose, xa, ya, za, ua, va, 0, 1, 0, r, g, bl);
            vertex(b, pose, xa, yd, zb, ua, vb, 0, 1, 0, r, g, bl);
            vertex(b, pose, xb, yc, zb, ub, vb, 0, 1, 0, r, g, bl);
            vertex(b, pose, xb, yb, za, ub, va, 0, 1, 0, r, g, bl);
            vertex(b, pose, xa, ya, za, ua, va, 0, 1, 0, r, g, bl);
            vertex(b, pose, xb, yb, za, ub, va, 0, 1, 0, r, g, bl);
            vertex(b, pose, xb, yc, zb, ub, vb, 0, 1, 0, r, g, bl);
            vertex(b, pose, xa, yd, zb, ua, vb, 0, 1, 0, r, g, bl);
        }
    }

    private static float ripple(float x, float z, float minX, float maxX, float minZ, float maxZ, float time) {
        float edge = Math.min(Math.min(x - minX, maxX - x), Math.min(z - minZ, maxZ - z));
        float fade = Mth.clamp(edge / 0.5F, 0, 1);
        float w = Mth.sin(x * 1.4F + time * 0.07F) + Mth.sin(z * 1.2F - time * 0.06F) + 0.5F * Mth.sin((x + z) * 2.2F + time * 0.1F);
        return (w - 1.3F) * 0.014F * fade;
    }

    private static void quad(VertexConsumer b, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz,
            float u0, float u1, float v0, float v1, float r, float g, float bl) {
        vertex(b, pose, ax, ay, az, u0, v1, nx, ny, nz, r, g, bl);
        vertex(b, pose, bx, by, bz, u1, v1, nx, ny, nz, r, g, bl);
        vertex(b, pose, cx, cy, cz, u1, v0, nx, ny, nz, r, g, bl);
        vertex(b, pose, dx, dy, dz, u0, v0, nx, ny, nz, r, g, bl);
    }

    private static void vertex(VertexConsumer b, PoseStack.Pose pose, float x, float y, float z, float u, float v,
            float nx, float ny, float nz, float r, float g, float bl) {
        b.addVertex(pose, x, y, z).setColor(r, g, bl, 0.95F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LIGHT).setNormal(pose, nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(HellForgeBlockEntity forge) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(HellForgeBlockEntity forge) {
        return new AABB(forge.getBlockPos()).inflate(10);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
