package dev.zsskayr.merlins_inferno.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.zsskayr.merlins_inferno.client.EdenweedSight;

/**
 * Lets The Sight of the Druidic Trance outline the Grove's creatures for the entranced player only. A server-side Glowing
 * effect would be visible to everybody, so the outline is decided here, on the local client, instead.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void merlins_inferno$sightOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && EdenweedSight.revealsEntity(entity)) {
            cir.setReturnValue(true);
        }
    }
}
