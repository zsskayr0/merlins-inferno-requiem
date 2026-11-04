package dev.zsskayr.merlins_inferno.mixin;

import java.util.List;

import dev.zsskayr.merlins_inferno.client.IrisGrassAliases;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional Iris integration: use whatever wind groups the selected shader uses for vanilla grass. */
@Mixin(targets = "net.irisshaders.iris.shaderpack.IdMap", remap = false)
public class IrisGrassAliasesMixin {
    @Shadow private Int2ObjectLinkedOpenHashMap<List<?>> blockPropertiesMap;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 0)
    private void merlins_inferno$aliasGrass(CallbackInfo ci) {
        if (blockPropertiesMap != null) IrisGrassAliases.add(blockPropertiesMap);
    }
}
