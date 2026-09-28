package dev.zsskayr.merlins_inferno.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.WitherRoseBlock;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.block.NetherPlants;

/** Lets the Wither Rose be planted on black nylium and Flesh (vanilla only allows dirt, netherrack and soul soil). */
@Mixin(WitherRoseBlock.class)
public abstract class WitherRoseBlockMixin {
    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void merlins_inferno$allowBlackNyliumAndFlesh(BlockState state, BlockGetter level, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if (NetherPlants.canSustainWitherRose(state)) {
            cir.setReturnValue(true);
        }
    }
}
