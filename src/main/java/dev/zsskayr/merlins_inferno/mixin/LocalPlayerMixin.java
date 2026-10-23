package dev.zsskayr.merlins_inferno.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.zsskayr.merlins_inferno.item.WingsItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Lets the in-air jump start a glide with wings in a Curios slot: the client also only looks at the chest slot. */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack merlins_inferno$glideWings(LocalPlayer self, EquipmentSlot slot, Operation<ItemStack> original) {
        return WingsItem.glideStack(self, original.call(self, slot));
    }
}
