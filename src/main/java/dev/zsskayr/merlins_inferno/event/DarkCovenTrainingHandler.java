package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.DarkCovenItem;

/**
 * Training: every kill a player makes while a Dark Coven is invoked counts toward that sword's quota for its stage. The
 * sword in hand takes the credit; failing that, the invoked one carried anywhere (the Demonic Pact needs no sword in hand).
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class DarkCovenTrainingHandler {
    private DarkCovenTrainingHandler() {
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob)
                || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        ItemStack sword = invokedCoven(player);
        if (!sword.isEmpty()) {
            DarkCovenItem.creditKill(player, sword);
        }
    }

    /** The invoked coven that gets the credit: main hand, then off hand, then anywhere in the inventory. */
    public static ItemStack invokedCoven(Player player) {
        for (ItemStack held : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (held.getItem() instanceof DarkCovenItem && DarkCovenItem.isInvoked(held)) {
                return held;
            }
        }
        for (ItemStack carried : player.getInventory().items) {
            if (carried.getItem() instanceof DarkCovenItem && DarkCovenItem.isInvoked(carried)) {
                return carried;
            }
        }
        return ItemStack.EMPTY;
    }
}
