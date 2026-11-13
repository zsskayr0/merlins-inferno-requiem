package dev.zsskayr.merlins_inferno.item;

import java.util.function.Supplier;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A bow forged from one of the mod's materials: sturdier than vanilla and adds flat damage to every arrow it looses. */
public class MaterialBowItem extends BowItem {
    private final double bonusDamage;
    private final Supplier<? extends Item> repairItem;

    public MaterialBowItem(Properties properties, double bonusDamage, Supplier<? extends Item> repairItem) {
        super(properties);
        this.bonusDamage = bonusDamage;
        this.repairItem = repairItem;
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
        arrow.setBaseDamage(arrow.getBaseDamage() + bonusDamage);
        return arrow;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(repairItem.get());
    }
}
