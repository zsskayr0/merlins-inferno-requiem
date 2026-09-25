package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import dev.zsskayr.merlins_inferno.attachment.SanctifiedProgress;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Applies/scales Sanctified based on where Raw Lyrium sits, every server tick:
 * <ul>
 *     <li>anywhere in the inventory (not held) - level 1;</li>
 *     <li>moved into either hand - jumps straight to level 3;</li>
 *     <li>held continuously - +1 level every {@link #TICKS_PER_LEVEL} ticks, up to
 *     {@link #MAX_LEVEL}.</li>
 * </ul>
 * Turning on and off both lag behind actual contact by {@link #CONTACT_DELAY_TICKS} - touching
 * Raw Lyrium doesn't sanctify you instantly, and letting go doesn't purge it instantly either,
 * per project decision. Once active, the level itself still updates immediately (only the
 * on/off transition is debounced) - see {@link SanctifiedProgress#active}.
 * <p>
 * The continuous-hold counter and the on/off debounce state both live in
 * {@link SanctifiedProgress}, a non-persistent data attachment (see {@code registry.ModAttachments})
 * - first tick-driven, per-item-location status effect in the mod, so this (and the attachment it
 * uses) is new infrastructure rather than a reuse of {@code DemonbloodCombatHandler}'s
 * damage-event-only pattern.
 */
public final class SanctifiedTickHandler {
    public static final int MAX_LEVEL = 5;
    public static final int TICKS_PER_LEVEL = 1200; // 1 minute per level past the level-3 jump

    /** Delay before Sanctified turns on after first contact, and the grace period before it turns off after contact ends. */
    public static final int CONTACT_DELAY_TICKS = 20; // 1s

    private static final int HAND_LEVEL = 3;
    private static final int INVENTORY_LEVEL = 1;
    private static final int REFRESH_DURATION = 100; // 5s - just needs to outlast one tick's gap

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        boolean inHand = player.getMainHandItem().is(ModItems.LYRIUM_RAW.get())
                || player.getOffhandItem().is(ModItems.LYRIUM_RAW.get());
        boolean hasContact = inHand || hasLyriumRawInInventory(player);

        SanctifiedProgress progress = player.getData(ModAttachments.SANCTIFIED_PROGRESS);

        int rawLevel;
        if (inHand) {
            progress.ticksHeld++;
            rawLevel = Math.min(MAX_LEVEL, HAND_LEVEL + progress.ticksHeld / TICKS_PER_LEVEL);
        } else {
            progress.ticksHeld = 0;
            rawLevel = hasContact ? INVENTORY_LEVEL : 0;
        }

        if (hasContact) {
            progress.ticksSinceContactLost = 0;
            if (!progress.active) {
                progress.ticksSinceContactGained++;
                if (progress.ticksSinceContactGained >= CONTACT_DELAY_TICKS) {
                    progress.active = true;
                }
            }
        } else {
            progress.ticksSinceContactGained = 0;
            if (progress.active) {
                progress.ticksSinceContactLost++;
                if (progress.ticksSinceContactLost >= CONTACT_DELAY_TICKS) {
                    progress.active = false;
                }
            }
        }

        if (!progress.active) {
            if (player.hasEffect(ModEffects.SANCTIFIED)) {
                player.removeEffect(ModEffects.SANCTIFIED);
            }
            return;
        }

        int amplifier = rawLevel - 1;
        MobEffectInstance current = player.getEffect(ModEffects.SANCTIFIED);
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() < REFRESH_DURATION / 2) {
            player.addEffect(new MobEffectInstance(ModEffects.SANCTIFIED, REFRESH_DURATION, amplifier, false, true, true));
        }
    }

    private static boolean hasLyriumRawInInventory(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.LYRIUM_RAW.get())) {
                return true;
            }
        }
        return false;
    }
}
