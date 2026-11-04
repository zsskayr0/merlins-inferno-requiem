package dev.zsskayr.merlins_inferno.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.entity.ExcaliburAwakening;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Excalibur, the mundane route's mythic. A mythic is never upgraded by hand: when the world reaches the Circle above, the
 * sword a player carries becomes its next form, keeping everything on the stack (enchantments, name, repair cost, any
 * other data) - like a diamond sword taken to netherite. Excalibur (Circle 2) becomes
 * {@code merlins_inferno:excalibur_true} in Circle 3: same name, its own texture and more damage.
 * <p>
 * Only a sword in a player's inventory changes; one in a chest, an AE2 network or on the ground stays as it is until a
 * player picks it up. The change is a ritual, an earthquake around the bearer (entity.ExcaliburAwakening): the sword leaves
 * the inventory, awakens, and comes back to its slot.
 */
public class ExcaliburItem extends SwordItem {
    public ExcaliburItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer player && stack.is(ModItems.EXCALIBUR.get())
                && ProgressionHelper.hasReached(server, ProgressionHelper.THIRD_CIRCLE)) {
            ExcaliburAwakening.start(player);
        }
    }

}
