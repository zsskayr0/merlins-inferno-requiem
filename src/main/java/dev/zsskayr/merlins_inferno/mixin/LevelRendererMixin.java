package dev.zsskayr.merlins_inferno.mixin;

import dev.zsskayr.merlins_inferno.client.TiledLines;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A block changed on the client: the cached halves of Tiled Porcelain's lines may be wrong now (see {@link TiledLines}). */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "blockChanged", at = @At("HEAD"))
    private void merlins_inferno$forgetTiledLines(CallbackInfo ci) {
        TiledLines.invalidate();
    }
}
