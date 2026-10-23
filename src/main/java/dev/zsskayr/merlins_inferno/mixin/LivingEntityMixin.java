package dev.zsskayr.merlins_inferno.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.zsskayr.merlins_inferno.item.WingsItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps a glide going while the wings sit in a Curios slot: the per-tick check only looks at the chest slot. */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack merlins_inferno$glideWings(LivingEntity self, EquipmentSlot slot, Operation<ItemStack> original) {
        return WingsItem.glideStack(self, original.call(self, slot));
    }
}
