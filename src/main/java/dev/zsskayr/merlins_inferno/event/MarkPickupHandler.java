package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import dev.zsskayr.merlins_inferno.item.MarkItem;
import dev.zsskayr.merlins_inferno.mark.MarkHelper;

/**
 * The danger of a Mark lying around: a player who bears none and picks one up off the ground has a
 * {@link MarkHelper#AUTO_EQUIP_CHANCE} chance of being bound to it on the spot, without a say. Taking it out of a chest is
 * not a pickup, so that stays a choice.
 */
public class MarkPickupHandler {
    @SubscribeEvent
    public void onPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
        ItemStack stack = entity.getItem();
        if (player.level().isClientSide || !(stack.getItem() instanceof MarkItem mark) || MarkHelper.hasMark(player)) {
            return;
        }
        if (player.getRandom().nextFloat() < MarkHelper.AUTO_EQUIP_CHANCE) {
            event.setCanPickup(TriState.FALSE);
            MarkHelper.bind(player, mark);
            stack.shrink(1);
            if (stack.isEmpty()) {
                entity.discard();
            }
        }
    }
}
