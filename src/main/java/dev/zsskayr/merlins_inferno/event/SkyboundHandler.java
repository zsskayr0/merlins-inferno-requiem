package dev.zsskayr.merlins_inferno.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Luminite's set bonus, Skybound: the four pieces together make you light and let the sky answer - faster on your feet and harder to
 * touch by magic toward the night and in the dark - and cancel fall damage. (The armor itself is already at the attribute's ceiling,
 * dragonsteel's, so the bonus is lightness rather than more armor.)
 * <p>
 * Two factors, the larger one wins: the <b>time of day</b> (0 at noon, 1 at midnight, smooth) and the <b>local darkness</b> (0 in full
 * light, up to {@link #LIGHT_WEIGHT} in total darkness - so a cave by day never gives as much as the night itself). The factor scales
 * a walking-speed bonus up to {@link #SPEED_PEAK} and a cut of up to {@link #MAGIC_SHIELD_PEAK} to magic damage.
 */
public class SkyboundHandler {
    private static final int CHECK_INTERVAL = 10;
    /** Walking speed added at the peak, as a fraction of the base. */
    public static final double SPEED_PEAK = 0.15;
    /** Share of magic damage (harming potions, evoker fangs and the like) cut at the peak. */
    public static final double MAGIC_SHIELD_PEAK = 0.30;
    /** How much of the peak the dark alone can give. */
    public static final double LIGHT_WEIGHT = 0.6;

    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "skybound_speed");

    /** 0 at noon (6000), 1 at midnight (18000), a cosine in between. */
    public static double nightFactor(long dayTime) {
        double t = Math.floorMod(dayTime, 24000L);
        return (1.0 - Math.cos((t - 6000.0) / 24000.0 * Math.PI * 2.0)) / 2.0;
    }

    /** 0 in full light (15), {@link #LIGHT_WEIGHT} in none. */
    public static double darkFactor(int light) {
        return LIGHT_WEIGHT * (15 - Mth.clamp(light, 0, 15)) / 15.0;
    }

    /** The bonus in [0, 1]: the larger of the two factors. */
    public static double bonus(long dayTime, int light) {
        return Math.max(nightFactor(dayTime), darkFactor(light));
    }

    public static boolean wearsFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.LUMINITE_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.LUMINITE_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.LUMINITE_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.LUMINITE_BOOTS.get());
    }

    /** The wearer's bonus at this instant, in [0, 1]. */
    public static double currentBonus(Player player) {
        var level = player.level();
        // Nether and End have a fixed time of day: only the light counts there.
        double time = level.dimensionType().hasFixedTime() ? 0.0 : nightFactor(level.getDayTime());
        return Math.max(time, darkFactor(level.getMaxLocalRawBrightness(player.blockPosition())));
    }

    /** Applies (or clears) the bonus for this instant. */
    public static void refresh(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (!wearsFullSet(player)) {
            speed.removeModifier(SPEED_ID);
            return;
        }
        speed.addOrUpdateTransientModifier(new AttributeModifier(SPEED_ID, SPEED_PEAK * currentBonus(player), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.tickCount % CHECK_INTERVAL == 0) {
            refresh(player);
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !wearsFullSet(player)) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
        } else if (event.getSource().is(DamageTypes.MAGIC) || event.getSource().is(DamageTypes.INDIRECT_MAGIC)) {
            event.setAmount((float) (event.getAmount() * (1.0 - MAGIC_SHIELD_PEAK * currentBonus(player))));
        }
    }
}
