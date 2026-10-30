package dev.zsskayr.merlins_inferno.mixin;

import dev.zsskayr.merlins_inferno.compat.ae2.Ae2FacadeCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers the facade AE2 is about to draw (see {@link Ae2FacadeCompat}); AE2 builds this view right before its quads. Optional: without AE2 the target never loads. */
@Mixin(targets = "appeng.client.render.cablebus.FacadeBlockAccess", remap = false)
public class Ae2FacadeAccessMixin {
    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void merlins_inferno$remember(BlockAndTintGetter level, BlockPos pos, Direction side, BlockState state, CallbackInfo ci) {
        Ae2FacadeCompat.begin(new Ae2FacadeCompat.Context((BlockAndTintGetter) (Object) this, pos.immutable(), side));
    }
}
