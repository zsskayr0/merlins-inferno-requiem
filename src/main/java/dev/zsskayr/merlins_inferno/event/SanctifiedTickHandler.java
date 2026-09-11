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
 * Applies/scales Sanctified based on where Lyrium Bruto sits, every server tick:
 * <ul>
 *     <li>anywhere in the inventory (not held) - level 1;</li>
 *     <li>moved into either hand - jumps straight to level 3;</li>
 *     <li>held continuously - +1 level every {@link #TICKS_PER_LEVEL} ticks, up to
 *     {@link #MAX_LEVEL}.</li>
 * </ul>
 * The continuous-hold counter lives in {@link SanctifiedProgress}, a non-persistent data
 * attachment (see {@code registry.ModAttachments}) - first tick-driven, per-item-location status
 * effect in the mod, so this (and the attachment it uses) is new infrastructure rather than a
 * reuse of {@code DemoniteCombatHandler}'s damage-event-only pattern.
 */
public final class SanctifiedTickHandler {
    public static final int MAX_LEVEL = 5;
    public static final int TICKS_PER_LEVEL = 1200; // 1 minute per level past the level-3 jump

    private static final int HAND_LEVEL = 3;
    private static final int INVENTORY_LEVEL = 1;
    private static final int REFRESH_DURATION = 100; // 5s - just needs to outlast one tick's gap

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        boolean inHand = player.getMainHandItem().is(ModItems.LYRIUM_BRUTO.get())
                || player.getOffhandItem().is(ModItems.LYRIUM_BRUTO.get());

        SanctifiedProgress progress = player.getData(ModAttachments.SANCTIFIED_PROGRESS);

        int targetLevel;
        if (inHand) {
            progress.ticksHeld++;
            targetLevel = Math.min(MAX_LEVEL, HAND_LEVEL + progress.ticksHeld / TICKS_PER_LEVEL);
        } else {
            progress.ticksHeld = 0;
            targetLevel = hasLyriumBrutoInInventory(player) ? INVENTORY_LEVEL : 0;
        }

        if (targetLevel <= 0) {
            if (player.hasEffect(ModEffects.SANCTIFIED)) {
                player.removeEffect(ModEffects.SANCTIFIED);
            }
            return;
        }

        int amplifier = targetLevel - 1;
        MobEffectInstance current = player.getEffect(ModEffects.SANCTIFIED);
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() < REFRESH_DURATION / 2) {
            player.addEffect(new MobEffectInstance(ModEffects.SANCTIFIED, REFRESH_DURATION, amplifier, false, true, true));
        }
    }

    private static boolean hasLyriumBrutoInInventory(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.LYRIUM_BRUTO.get())) {
                return true;
            }
        }
        return false;
    }
}
