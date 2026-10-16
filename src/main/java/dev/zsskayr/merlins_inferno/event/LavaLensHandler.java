package dev.zsskayr.merlins_inferno.event;

import dev.zsskayr.merlins_inferno.compat.OptionalCurios;
import dev.zsskayr.merlins_inferno.item.LavaLensItem;
import dev.zsskayr.merlins_inferno.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** One active lens pays for one effect; inventory and Curios do not double-charge. */
@net.neoforged.fml.common.EventBusSubscriber(modid = dev.zsskayr.merlins_inferno.Merlins_inferno.MODID)
public final class LavaLensHandler {
    public static final String ENABLED = "MerlinsLavaLensEnabled";
    private static final String WEAR = "MerlinsLavaLensWearTicks";
    private LavaLensHandler() {}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide) refresh(event.getEntity(), true);
    }
    public static ItemStack findLens(Player player) {
        for (ItemStack stack : player.getInventory().items) if (stack.getItem() instanceof LavaLensItem) return stack;
        for (ItemStack stack : player.getInventory().offhand) if (stack.getItem() instanceof LavaLensItem) return stack;
        for (ItemStack stack : OptionalCurios.equipped(player)) if (stack.getItem() instanceof LavaLensItem) return stack;
        return ItemStack.EMPTY;
    }
    public static void refresh(Player player, boolean tickWear) {
        if (!(player.level() instanceof ServerLevel server)) return;
        var data = player.getPersistentData();
        ItemStack lens = findLens(player);
        if (!data.getBoolean(ENABLED) || lens.isEmpty() || !player.isAlive() || player.isSpectator()) {
            player.removeEffect(ModEffects.LAVA_SIGHT);
            if (lens.isEmpty() || !player.isAlive()) data.putBoolean(ENABLED, false);
            return;
        }
        if (tickWear) {
            int ticks = data.getInt(WEAR) + 1;
            if (ticks >= 20) {
                ticks = 0;
                lens.hurtAndBreak(1, server, player, item -> player.playSound(SoundEvents.ITEM_BREAK, 1, 1));
            }
            data.putInt(WEAR, ticks);
        }
        if (lens.isEmpty()) {
            data.putBoolean(ENABLED, false);
            player.removeEffect(ModEffects.LAVA_SIGHT);
        } else if (!player.hasEffect(ModEffects.LAVA_SIGHT) || player.getEffect(ModEffects.LAVA_SIGHT).getDuration() < 15) {
            player.addEffect(new MobEffectInstance(ModEffects.LAVA_SIGHT, 30, 0, true, false, false));
        }
    }
}
