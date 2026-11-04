package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import dev.zsskayr.merlins_inferno.entity.ExcaliburAwakening;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * The awakening seen in the world: the sword floating and spinning over its bearer, turning from worn to true as the
 * light sweeps it, inside a burst of golden rays drawn the way the dying Ender Dragon's are.
 */
public class ExcaliburAwakeningRenderer extends EntityRenderer<ExcaliburAwakening> {
    private static final float HALF_SQRT_3 = (float) (Math.sqrt(3.0) / 2.0);
    /** Rays: a warm white core fading to transparent gold at the tips. */
    private static final int RAY_EDGE = 0x00FF9A10;
    private static final float RAY_SCALE = 0.2F;
    private static final int FULL_BRIGHT = 0xF000F0;

    private final ItemRenderer itemRenderer;
    private final ItemStack sword = new ItemStack(ModItems.EXCALIBUR_AWAKENING.get());

    public ExcaliburAwakeningRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(ExcaliburAwakening entity, Frustum frustum, double x, double y, double z) {
        return true; // the rays reach far past the entity's box
    }

    @Override
    public void render(ExcaliburAwakening entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        float age = entity.tickCount + partialTick;

        if (age >= ExcaliburAwakening.RAYS_AT && age < ExcaliburAwakening.DURATION - 4) {
            float completion = Mth.clamp((age - ExcaliburAwakening.RAYS_AT) / (ExcaliburAwakening.CLIMAX_AT + 10 - ExcaliburAwakening.RAYS_AT), 0.0F, 1.0F);
            poseStack.pushPose();
            poseStack.scale(RAY_SCALE, RAY_SCALE, RAY_SCALE);
            renderRays(poseStack, completion, buffer.getBuffer(RenderType.dragonRays()));
            renderRays(poseStack, completion, buffer.getBuffer(RenderType.dragonRaysDepth()));
            poseStack.popPose();
        }

        poseStack.pushPose();
        // A flat sprite seen edge-on is a dark bar: it always faces the camera, blade up, trembling with the quake.
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        float tremble = age < ExcaliburAwakening.CLIMAX_AT ? (age / ExcaliburAwakening.CLIMAX_AT) * 4.0F : 0.0F;
        // the sprite is drawn on the diagonal, point at the top right; after the camera turn and the 180 above, -45 stands it point up
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F + Mth.sin(age * 2.7F) * tremble));
        float pulse = age >= ExcaliburAwakening.CLIMAX_AT && age < ExcaliburAwakening.CLIMAX_AT + 6
                ? 1.0F + 0.25F * Mth.sin((age - ExcaliburAwakening.CLIMAX_AT) / 6.0F * Mth.PI) : 1.0F;
        poseStack.scale(1.8F * pulse, 1.8F * pulse, 1.8F * pulse);
        ExcaliburAwakeningClient.setMorph(Mth.clamp((age - ExcaliburAwakening.RAYS_AT) / (ExcaliburAwakening.CLIMAX_AT - ExcaliburAwakening.RAYS_AT), 0.0F, 1.0F));
        this.itemRenderer.renderStatic(this.sword, ItemDisplayContext.FIXED, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack, buffer,
                entity.level(), entity.getId());
        ExcaliburAwakeningClient.clearMorph();
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, light);
    }

    /** EnderDragonRenderer#renderRays, in gold. */
    private static void renderRays(PoseStack poseStack, float completion, VertexConsumer consumer) {
        poseStack.pushPose();
        float fade = Math.min(completion > 0.8F ? (completion - 0.8F) / 0.2F : 0.0F, 1.0F);
        int core = FastColor.ARGB32.colorFromFloat(1.0F - fade, 1.0F, 0.82F, 0.38F);
        RandomSource random = RandomSource.create(432L);
        Vector3f center = new Vector3f();
        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        Vector3f c = new Vector3f();
        Quaternionf rotation = new Quaternionf();
        int count = Mth.floor((completion + completion * completion) / 2.0F * 60.0F);
        for (int i = 0; i < count; i++) {
            rotation.rotationXYZ(random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI)
                    .rotateXYZ(random.nextFloat() * Mth.TWO_PI, random.nextFloat() * Mth.TWO_PI,
                            random.nextFloat() * Mth.TWO_PI + completion * Mth.HALF_PI);
            poseStack.mulPose(rotation);
            float length = random.nextFloat() * 20.0F + 5.0F + fade * 10.0F;
            float width = random.nextFloat() * 2.0F + 1.0F + fade * 2.0F;
            a.set(-HALF_SQRT_3 * width, length, -0.5F * width);
            b.set(HALF_SQRT_3 * width, length, -0.5F * width);
            c.set(0.0F, length, width);
            PoseStack.Pose pose = poseStack.last();
            consumer.addVertex(pose, center).setColor(core);
            consumer.addVertex(pose, a).setColor(RAY_EDGE);
            consumer.addVertex(pose, b).setColor(RAY_EDGE);
            consumer.addVertex(pose, center).setColor(core);
            consumer.addVertex(pose, b).setColor(RAY_EDGE);
            consumer.addVertex(pose, c).setColor(RAY_EDGE);
            consumer.addVertex(pose, center).setColor(core);
            consumer.addVertex(pose, c).setColor(RAY_EDGE);
            consumer.addVertex(pose, a).setColor(RAY_EDGE);
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ExcaliburAwakening entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
