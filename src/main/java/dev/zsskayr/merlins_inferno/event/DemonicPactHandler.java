package dev.zsskayr.merlins_inferno.event;

import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.effect.DemonicPactAura;
import dev.zsskayr.merlins_inferno.item.DarkCovenItem;
import dev.zsskayr.merlins_inferno.registry.ModEffects;

/** The Demonic Pact's own aura and Resistance (what Resistance II gave, 40% off): only at the last stage, where the coven becomes a pact. */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class DemonicPactHandler {
    private static final float PACT_DAMAGE_FACTOR = 0.6F;

    private DemonicPactHandler() {
    }

    /** The aura others see while a coven lasts, with or without the sword in hand. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level && player.tickCount % 5 == 0) {
            var pact = player.getEffect(ModEffects.DEMONIC_PACT);
            if (pact != null) {
                DemonicPactAura.emit(level, player, pact.getAmplifier());
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        var pact = event.getEntity().getEffect(ModEffects.DEMONIC_PACT);
        if (pact != null && pact.getAmplifier() == DarkCovenItem.MAX_STAGE && !event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            event.setAmount(event.getAmount() * PACT_DAMAGE_FACTOR);
        }
    }
}
