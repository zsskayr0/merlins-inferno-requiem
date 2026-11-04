package dev.zsskayr.merlins_inferno.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import dev.zsskayr.merlins_inferno.registry.ModAttachments;

/**
 * The faint outline left in the slot Excalibur flew out of during its awakening. It keeps the slot, so the sword always
 * has its place to come back to (it replaces this, wherever in the inventory it was moved). It cannot outlive the
 * ritual: with no sword held for the player it disappears, and thrown on the ground it vanishes at once.
 */
public class ExcaliburSlotItem extends Item {
    public ExcaliburSlotItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && (!(entity instanceof Player player) || player.getData(ModAttachments.EXCALIBUR_HELD).isEmpty())) {
            stack.setCount(0);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide()) {
            entity.discard();
        }
        return true;
    }
}
