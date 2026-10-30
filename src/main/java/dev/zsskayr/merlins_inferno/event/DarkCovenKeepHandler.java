package dev.zsskayr.merlins_inferno.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.DarkCovenItem;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;

/**
 * No stage of the Dark Coven can be thrown away: it moves between inventory slots and chests, nothing more. A toss is put
 * back in the inventory (if it is full the throw is allowed, rather than losing the sword). Death does not drop it either:
 * it comes back on respawn. Trying to throw it away is answered with a short Wither.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class DarkCovenKeepHandler {
    /** How long the Wither of an attempted throw lasts (Wither I): a warning, not a punishment. */
    private static final int WARNING_TICKS = 120;

    private DarkCovenKeepHandler() {
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        var stack = event.getEntity().getItem();
        if (stack.getItem() instanceof DarkCovenItem && !event.getPlayer().level().isClientSide
                && event.getPlayer().getInventory().add(stack)) {
            event.setCanceled(true);
            warn(event.getPlayer());
        }
    }

    /** The sword takes offence: a short Wither, a groan and a word on the action bar. */
    private static void warn(net.minecraft.world.entity.player.Player player) {
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, WARNING_TICKS, 0));
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_AMBIENT,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 0.6F);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("item.merlins_inferno.dark_coven.disrespect")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED), true);
    }

    /** Takes the coven out of a player's death drops and remembers it for the respawn. */
    @SubscribeEvent
    public static void onDeathDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            event.getDrops().removeIf(drop -> {
                if (drop.getItem().getItem() instanceof DarkCovenItem) {
                    player.getData(ModAttachments.KEPT_COVENS).add(drop.getItem().copy());
                    return true;
                }
                return false;
            });
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            for (var stack : event.getOriginal().getData(ModAttachments.KEPT_COVENS)) {
                DarkCovenItem.setInvoked(stack, false);
                if (!event.getEntity().getInventory().add(stack)) {
                    event.getEntity().drop(stack, false);
                }
            }
        }
    }
}
