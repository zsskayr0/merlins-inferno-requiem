package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * The Pandora Box and Primary Circle Rune return to their owner after death.
 */
public class PandoraHandler {
    /** Cancels the Box and rune drops, remembering what to return on respawn. */
    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        event.getDrops().removeIf(entity -> {
            if (entity.getItem().is(ModItems.PANDORA_BOX.get())) {
                player.setData(ModAttachments.RETURN_PANDORA_BOX, true);
                return true;
            }
            if (entity.getItem().is(ModItems.PRIMARY_CIRCLE_RUNE.get())) {
                player.setData(ModAttachments.AWARD_PRIMARY_CIRCLE_RUNE, true);
                return true;
            }
            return false;
        });
    }

    /** On respawn (never on a dimension change), hands back the Box and rune if owed. */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        net.minecraft.world.entity.player.Player original = event.getOriginal();
        if (original.getData(ModAttachments.RETURN_PANDORA_BOX)) {
            event.getEntity().getInventory().add(new ItemStack(ModItems.PANDORA_BOX.get()));
            event.getEntity().setData(ModAttachments.RETURN_PANDORA_BOX, false);
        }
        if (original.getData(ModAttachments.AWARD_PRIMARY_CIRCLE_RUNE)) {
            ItemStack rune = new ItemStack(ModItems.PRIMARY_CIRCLE_RUNE.get());
            if (!event.getEntity().getInventory().add(rune)) event.getEntity().drop(rune, false);
            event.getEntity().setData(ModAttachments.AWARD_PRIMARY_CIRCLE_RUNE, false);
        }
    }
}
