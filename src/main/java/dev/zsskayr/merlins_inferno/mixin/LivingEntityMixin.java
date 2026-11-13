package dev.zsskayr.merlins_inferno.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.zsskayr.merlins_inferno.item.SalamanderBracelet;
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

    /** Salamander Bracelet: in lava the wearer takes vanilla's water movement instead of lava's. */
    @WrapOperation(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInWater()Z"))
    private boolean merlins_inferno$salamanderSwim(LivingEntity self, Operation<Boolean> original) {
        return original.call(self) || SalamanderBracelet.swimsIn(self);
    }

    /** Ebony is forged for the depths: a full set swims faster through lava with the Salamander Bracelet. */
    @WrapOperation(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"))
    private void merlins_inferno$ebonyLavaSwim(LivingEntity self, float speed, net.minecraft.world.phys.Vec3 input, Operation<Void> original) {
        if (SalamanderBracelet.swimsIn(self) && dev.zsskayr.merlins_inferno.event.EbonyArmorHandler.wearsFullSet(self)) speed *= SalamanderBracelet.EBONY_SPEED;
        original.call(self, speed, input);
    }

    /** Soul Lava moves like lava, not like the water-style movement NeoForge gives other fluids. */
    @WrapOperation(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;getFluidType()Lnet/neoforged/neoforge/fluids/FluidType;"))
    private net.neoforged.neoforge.fluids.FluidType merlins_inferno$soulLavaIsLava(net.minecraft.world.level.material.FluidState state,
            Operation<net.neoforged.neoforge.fluids.FluidType> original) {
        var type = original.call(state);
        return type == dev.zsskayr.merlins_inferno.registry.ModFluids.SOUL_LAVA_TYPE.get() ? net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value() : type;
    }
}
