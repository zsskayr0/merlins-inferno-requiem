package dev.zsskayr.merlins_inferno.mixin;

import dev.zsskayr.merlins_inferno.client.LavaSight;
import net.minecraft.client.Camera;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shader packs read the fluid around the camera (Iris' isEyeInWater) to draw their own lava fog, which no fog event can
 * touch. While the Hell Sight is on, the camera reports no fluid when it is in lava, so the pack draws no lava fog.
 */
@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = "getFluidInCamera", at = @At("RETURN"), cancellable = true)
    private void merlins_inferno$clearLava(CallbackInfoReturnable<FogType> cir) {
        if (cir.getReturnValue() == FogType.LAVA && LavaSight.hellSightClearsLava()) {
            cir.setReturnValue(FogType.NONE);
        }
    }
}
