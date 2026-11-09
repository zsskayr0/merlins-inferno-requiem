package dev.zsskayr.merlins_inferno.event;

import dev.zsskayr.merlins_inferno.attachment.SanctifiedProgress;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Raw red lyrium follows the same contact, hand, and level timing as raw lyrium. */
public final class DemonifiedTickHandler {
    @SubscribeEvent public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        SanctifiedProgress progress = player.getData(ModAttachments.DEMONIFIED_PROGRESS);
        if (player.tickCount % 10 == 0 && player.hasEffect(ModEffects.DEMONIFIED)
                && dev.zsskayr.merlins_inferno.mark.DemonifiedWard.isWarded(player)) {
            player.removeEffect(ModEffects.DEMONIFIED);
        }
        boolean inHand = player.getMainHandItem().is(ModItems.RED_LYRIUM_RAW.get())
                || player.getOffhandItem().is(ModItems.RED_LYRIUM_RAW.get());
        if (player.tickCount % 5 == 0) {
            progress.inventoryContact = false;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(ModItems.RED_LYRIUM_RAW.get())) { progress.inventoryContact = true; break; }
            }
        }
        boolean contact = inHand || progress.inventoryContact;
        int level;
        if (inHand) {
            progress.ticksHeld++;
            level = Math.min(SanctifiedTickHandler.MAX_LEVEL, 3 + progress.ticksHeld / SanctifiedTickHandler.TICKS_PER_LEVEL);
        } else {
            progress.ticksHeld = 0;
            level = contact ? 1 : 0;
        }
        if (contact) {
            progress.ticksSinceContactLost = 0;
            if (!progress.active && ++progress.ticksSinceContactGained >= SanctifiedTickHandler.CONTACT_DELAY_TICKS) progress.active = true;
        } else {
            progress.ticksSinceContactGained = 0;
            if (progress.active && ++progress.ticksSinceContactLost >= SanctifiedTickHandler.CONTACT_DELAY_TICKS) progress.active = false;
        }
        if (!progress.active) {
            if (progress.appliedByLyrium) {
                player.removeEffect(ModEffects.DEMONIFIED);
                progress.appliedByLyrium = false;
            }
            return;
        }
        // Retain the last level during the one-second release grace period.
        MobEffectInstance current = player.getEffect(ModEffects.DEMONIFIED);
        int amplifier = level > 0 ? level - 1 : current != null ? current.getAmplifier() : 0;
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() < 50) {
            player.addEffect(new MobEffectInstance(ModEffects.DEMONIFIED, 100, amplifier, false, true, true));
            progress.appliedByLyrium = true;
        }
    }
}
