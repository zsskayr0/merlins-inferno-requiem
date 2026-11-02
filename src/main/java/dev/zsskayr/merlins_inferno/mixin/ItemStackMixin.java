package dev.zsskayr.merlins_inferno.mixin;

import java.util.function.Consumer;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

import dev.zsskayr.merlins_inferno.event.RouteHome;

/** Where a player's gear takes its wear: lets {@link RouteHome} spare it on its route's home ground. */
@Mixin(ItemStack.class)
public class ItemStackMixin {
    @WrapMethod(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V")
    private void merlins_inferno$homeGroundWear(int amount, ServerLevel level, ServerPlayer player, Consumer<Item> onBreak, Operation<Void> original) {
        original.call(RouteHome.adjustDamage((ItemStack) (Object) this, amount, player), level, player, onBreak);
    }
}
