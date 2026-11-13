package dev.zsskayr.merlins_inferno.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.zsskayr.merlins_inferno.item.SalamanderBracelet;
import dev.zsskayr.merlins_inferno.item.WingsItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public class PlayerMixin {
    /** Lets the server start a glide with wings in a Curios slot: it only looks at the chest slot. */
    @WrapOperation(method = "tryToStartFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack merlins_inferno$glideWings(Player self, EquipmentSlot slot, Operation<ItemStack> original) {
        return WingsItem.glideStack(self, original.call(self, slot));
    }

    /** Salamander Bracelet: lava counts as a swimmable fluid (swim pose, sprint-swim, sneaking to dive). */
    public boolean canSwimInFluidType(FluidType type) {
        Player self = (Player) (Object) this;
        if (SalamanderBracelet.lava(type) && SalamanderBracelet.wearing(self)) return true;
        return type.canSwim(self);
    }
}
