package dev.zsskayr.merlins_inferno.mixin;

import java.util.Map;

import dev.zsskayr.merlins_inferno.client.IrisDimensionAliases;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional Iris integration, applied before dimension program sets are requested and cached. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.ShaderPack", remap = false)
public class IrisDimensionAliasesMixin {
    @Shadow private Map<Object, String> dimensionMap;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 0)
    private void merlins_inferno$aliasDimensions(CallbackInfo ci) {
        if (dimensionMap != null) IrisDimensionAliases.add(dimensionMap);
    }
}
