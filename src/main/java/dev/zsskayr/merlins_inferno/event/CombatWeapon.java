package dev.zsskayr.merlins_inferno.event;

import dev.zsskayr.merlins_inferno.item.MaterialBowItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;

/** The weapon behind a hit: the held item for melee, the bow an arrow was loosed from for shots. */
public final class CombatWeapon {
    private CombatWeapon() {
    }

    public static ItemStack of(DamageSource source) {
        if (source.getDirectEntity() instanceof AbstractArrow arrow) {
            ItemStack bow = arrow.getWeaponItem();
            return bow != null && bow.getItem() instanceof MaterialBowItem ? bow : ItemStack.EMPTY;
        }
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker) {
            ItemStack held = attacker.getMainHandItem();
            // Bashing someone with a bow is not a shot: the material's power rides the arrow.
            return held.getItem() instanceof MaterialBowItem ? ItemStack.EMPTY : held;
        }
        return ItemStack.EMPTY;
    }
}
