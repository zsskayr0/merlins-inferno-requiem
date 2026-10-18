package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

/** Client side of the Hell Sight: clearer lava, thinner Nether/Oblivion haze, and a lower, fainter burning overlay. */
@EventBusSubscriber(modid=Merlins_inferno.MODID,value=Dist.CLIENT)
public final class LavaSight {
    private static final float FIRE_DROP = 0.38F;
    private static final float FIRE_ALPHA = 0.4F;
    private static final float HAZE_FAR_FACTOR = 2.4F;
    private static final float HAZE_FAR_CAP = 320F;

    private static boolean hasLens() {
        var mc=Minecraft.getInstance();
        return mc.player!=null && mc.player.hasEffect(ModEffects.LAVA_SIGHT);
    }
    /** Read by CameraMixin: the camera stops reporting lava so shader packs draw no lava fog either. */
    public static boolean hellSightClearsLava() {
        var mc=Minecraft.getInstance();
        return hasLens() && mc.player.isEyeInFluid(net.minecraft.tags.FluidTags.LAVA);
    }
    private static boolean active() {
        return hellSightClearsLava();
    }
    /** Nether-style dimensions (the Nether and the Oblivion) carry the thick terrain haze. */
    private static boolean netherHaze() {
        var mc=Minecraft.getInstance();
        return mc.level!=null && mc.level.dimensionType().effectsLocation().equals(BuiltinDimensionTypes.NETHER_EFFECTS);
    }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event) {
        if (!hasLens()) return;
        if (event.getType()==FogType.LAVA) {
            if (!active()) return;
            event.setNearPlaneDistance(.25F);
            event.setFarPlaneDistance(24);
            event.setFogShape(FogShape.SPHERE);
            event.setCanceled(true);
        } else if (event.getType()==FogType.NONE && event.getMode()==net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN && netherHaze()) {
            var player=Minecraft.getInstance().player;
            if (player.hasEffect(MobEffects.BLINDNESS) || player.hasEffect(MobEffects.DARKNESS)) return;
            float far=Math.min(event.getFarPlaneDistance()*HAZE_FAR_FACTOR, Math.max(HAZE_FAR_CAP, event.getFarPlaneDistance()));
            event.setFarPlaneDistance(far);
            event.setNearPlaneDistance(far*.35F);
            event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void overlay(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType()!=RenderBlockScreenEffectEvent.OverlayType.FIRE || !hasLens()) return;
        event.setCanceled(true);
        renderFire(Minecraft.getInstance(), event.getPoseStack());
    }

    /** Vanilla's burning overlay, pushed down the screen and drawn translucent. */
    private static void renderFire(Minecraft mc, PoseStack poseStack) {
        RenderSystem.depthFunc(519);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        TextureAtlasSprite sprite=ModelBakery.FIRE_1.sprite();
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        float u0=sprite.getU0(), u1=sprite.getU1(), v0=sprite.getV0(), v1=sprite.getV1();
        var buffer=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i=0;i<2;i++) {
            poseStack.pushPose();
            poseStack.translate(-(i*2-1)*.24F, -.3F-FIRE_DROP, 0);
            poseStack.mulPose(Axis.YP.rotationDegrees((i*2-1)*10F));
            Matrix4f m=poseStack.last().pose();
            buffer.addVertex(m,-.5F,-.5F,-.5F).setUv(u1,v1).setColor(1F,1F,1F,FIRE_ALPHA);
            buffer.addVertex(m,.5F,-.5F,-.5F).setUv(u0,v1).setColor(1F,1F,1F,FIRE_ALPHA);
            buffer.addVertex(m,.5F,.5F,-.5F).setUv(u0,v0).setColor(1F,1F,1F,FIRE_ALPHA);
            buffer.addVertex(m,-.5F,.5F,-.5F).setUv(u1,v0).setColor(1F,1F,1F,FIRE_ALPHA);
            poseStack.popPose();
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(515);
    }
}
