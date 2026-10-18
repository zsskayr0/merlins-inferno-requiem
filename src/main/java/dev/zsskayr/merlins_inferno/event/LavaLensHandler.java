package dev.zsskayr.merlins_inferno.event;

import dev.zsskayr.merlins_inferno.compat.OptionalCurios;
import dev.zsskayr.merlins_inferno.item.LavaLensItem;
import dev.zsskayr.merlins_inferno.registry.ModEffects;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * One active lens pays for one effect; inventory and Curios do not double-charge. The lens counts as carried while it
 * is on the mouse cursor or moving between slots (a short grace covers the creative inventory, which the server cannot
 * see), so handling it never switches it off - only really losing it does.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = dev.zsskayr.merlins_inferno.Merlins_inferno.MODID)
public final class LavaLensHandler {
    public static final String ENABLED = "MerlinsLavaLensEnabled";
    private static final String WEAR = "MerlinsLavaLensWearTicks";
    private static final String MISSING = "MerlinsLavaLensMissingTicks";
    /** Ticks without any lens before the lens switches itself off for good. */
    private static final int GRACE_TICKS = 60;
    private LavaLensHandler() {}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide) refresh(event.getEntity(), true);
    }
    private static void forEachLens(Player player, Consumer<ItemStack> action) {
        for (ItemStack stack : player.getInventory().items) if (stack.getItem() instanceof LavaLensItem) action.accept(stack);
        for (ItemStack stack : player.getInventory().offhand) if (stack.getItem() instanceof LavaLensItem) action.accept(stack);
        for (ItemStack stack : OptionalCurios.equipped(player)) if (stack.getItem() instanceof LavaLensItem) action.accept(stack);
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.getItem() instanceof LavaLensItem) action.accept(carried);
    }
    public static ItemStack findLens(Player player) {
        ItemStack[] found = {ItemStack.EMPTY};
        forEachLens(player, stack -> { if (found[0].isEmpty()) found[0] = stack; });
        return found[0];
    }
    public static void refresh(Player player, boolean tickWear) {
        if (!(player.level() instanceof ServerLevel server)) return;
        var data = player.getPersistentData();
        if (!data.getBoolean(ENABLED) || !player.isAlive() || player.isSpectator()) {
            player.removeEffect(ModEffects.LAVA_SIGHT);
            if (!player.isAlive()) data.putBoolean(ENABLED, false);
            data.putInt(MISSING, 0);
            forEachLens(player, stack -> LavaLensItem.setActive(stack, false));
            return;
        }
        ItemStack lens = findLens(player);
        if (lens.isEmpty()) {
            // Not found right now: wait before giving up, the effect keeps running on its remaining duration.
            int missing = data.getInt(MISSING) + 1;
            data.putInt(MISSING, missing);
            if (missing > GRACE_TICKS) {
                data.putBoolean(ENABLED, false);
                data.putInt(MISSING, 0);
                player.removeEffect(ModEffects.LAVA_SIGHT);
            }
            return;
        }
        data.putInt(MISSING, 0);
        forEachLens(player, stack -> LavaLensItem.setActive(stack, stack == lens));
        if (tickWear) {
            int ticks = data.getInt(WEAR) + 1;
            if (ticks >= 20) {
                ticks = 0;
                lens.hurtAndBreak(1, server, player, item -> player.playSound(SoundEvents.ITEM_BREAK, 1, 1));
            }
            data.putInt(WEAR, ticks);
        }
        if (lens.isEmpty()) {
            // The last point of durability just ran out.
            data.putBoolean(ENABLED, false);
            player.removeEffect(ModEffects.LAVA_SIGHT);
        } else if (!player.hasEffect(ModEffects.LAVA_SIGHT) || player.getEffect(ModEffects.LAVA_SIGHT).getDuration() < 15) {
            player.addEffect(new MobEffectInstance(ModEffects.LAVA_SIGHT, 30, 0, true, false, false));
        }
    }
}
