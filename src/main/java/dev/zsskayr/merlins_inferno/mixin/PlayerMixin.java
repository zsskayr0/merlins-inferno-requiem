package dev.zsskayr.merlins_inferno.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.zsskayr.merlins_inferno.item.WingsItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Lets the server start a glide with wings in a Curios slot: it only looks at the chest slot. */
@Mixin(Player.class)
public class PlayerMixin {
    @WrapOperation(method = "tryToStartFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack merlins_inferno$glideWings(Player self, EquipmentSlot slot, Operation<ItemStack> original) {
        return WingsItem.glideStack(self, original.call(self, slot));
    }
}
