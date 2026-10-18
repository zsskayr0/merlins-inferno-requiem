package dev.zsskayr.merlins_inferno.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import dev.zsskayr.merlins_inferno.client.HellSightGlint;
import dev.zsskayr.merlins_inferno.item.LavaLensItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws the enchantment glint of an active Hell Sight in red. The stack being rendered is remembered for the duration of
 * ItemRenderer.render, and the two glint buffer helpers hand out the red render types instead of the purple ones.
 * Every injector is optional so a changed ItemRenderer only loses the colour.
 */
@Mixin(value = ItemRenderer.class, priority = 500)
public class ItemRendererMixin {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static boolean merlins_inferno$loggedRemember, merlins_inferno$loggedFoil;

    @Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At("HEAD"), require = 0)
    private void merlins_inferno$remember(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack,
                                          MultiBufferSource buffer, int light, int overlay, BakedModel model, CallbackInfo ci) {
        boolean active = stack.getItem() instanceof LavaLensItem && LavaLensItem.isActive(stack);
        if (active && !merlins_inferno$loggedRemember) {
            merlins_inferno$loggedRemember = true;
            LOGGER.info("Hell Sight glint: an active lens is being rendered (foil={})", stack.hasFoil());
        }
        HellSightGlint.ACTIVE.set(active);
    }

    @Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At("RETURN"), require = 0)
    private void merlins_inferno$forget(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack,
                                        MultiBufferSource buffer, int light, int overlay, BakedModel model, CallbackInfo ci) {
        HellSightGlint.ACTIVE.set(false);
    }

    @Inject(method = "getFoilBuffer", at = @At("HEAD"), cancellable = true, require = 0)
    private static void merlins_inferno$redFoil(MultiBufferSource buffer, RenderType renderType, boolean isItem, boolean glint,
                                                CallbackInfoReturnable<VertexConsumer> cir) {
        if (!glint || !HellSightGlint.ACTIVE.get()) return;
        if (!merlins_inferno$loggedFoil) {
            merlins_inferno$loggedFoil = true;
            LOGGER.info("Hell Sight glint: red glint buffer handed out (getFoilBuffer)");
        }
        RenderType red = Minecraft.useShaderTransparency() && renderType == Sheets.translucentItemSheet()
                ? HellSightGlint.GLINT_TRANSLUCENT : isItem ? HellSightGlint.GLINT : HellSightGlint.ENTITY_GLINT;
        cir.setReturnValue(VertexMultiConsumer.create(buffer.getBuffer(red), buffer.getBuffer(renderType)));
    }

    @Inject(method = "getFoilBufferDirect", at = @At("HEAD"), cancellable = true, require = 0)
    private static void merlins_inferno$redFoilDirect(MultiBufferSource buffer, RenderType renderType, boolean noEntity, boolean withGlint,
                                                      CallbackInfoReturnable<VertexConsumer> cir) {
        if (!withGlint || !HellSightGlint.ACTIVE.get()) return;
        if (!merlins_inferno$loggedFoil) {
            merlins_inferno$loggedFoil = true;
            LOGGER.info("Hell Sight glint: red glint buffer handed out (getFoilBufferDirect)");
        }
        RenderType red = noEntity ? HellSightGlint.GLINT : HellSightGlint.ENTITY_GLINT;
        cir.setReturnValue(VertexMultiConsumer.create(buffer.getBuffer(red), buffer.getBuffer(renderType)));
    }
}
