package dev.zsskayr.merlins_inferno.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
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
 * Luminite's set bonus, Skybound: the four pieces together grow stronger toward the night and in the dark, and cancel fall damage.
 * <p>
 * Two factors, the larger one wins: the <b>time of day</b> (0 at noon, 1 at midnight, smooth) and the <b>local darkness</b> (0 in full
 * light, up to {@link #LIGHT_WEIGHT} in total darkness - so a cave by day never gives as much as the night itself). The factor scales
 * extra armor and toughness on top of the material's Netherite-level floor; at its peak that is just under Ebony's.
 */
public class SkyboundHandler {
    private static final int CHECK_INTERVAL = 10;
    /** Armor points added at the peak: 20 -> 25, under Ebony's 26. */
    public static final double ARMOR_PEAK = 5.0;
    /** Toughness added at the peak: 3 -> 4.5, under Ebony's 5. */
    public static final double TOUGHNESS_PEAK = 1.5;
    /** How much of the peak the dark alone can give. */
    public static final double LIGHT_WEIGHT = 0.6;

    private static final ResourceLocation ARMOR_ID = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "skybound_armor");
    private static final ResourceLocation TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "skybound_toughness");

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

    /** Applies (or clears) the bonus for this instant. */
    public static void refresh(Player player) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        AttributeInstance toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (armor == null || toughness == null) {
            return;
        }
        if (!wearsFullSet(player)) {
            armor.removeModifier(ARMOR_ID);
            toughness.removeModifier(TOUGHNESS_ID);
            return;
        }
        var level = player.level();
        // Nether and End have a fixed time of day: only the light counts there.
        double time = level.dimensionType().hasFixedTime() ? 0.0 : nightFactor(level.getDayTime());
        double bonus = Math.max(time, darkFactor(level.getMaxLocalRawBrightness(player.blockPosition())));
        armor.addOrUpdateTransientModifier(new AttributeModifier(ARMOR_ID, ARMOR_PEAK * bonus, AttributeModifier.Operation.ADD_VALUE));
        toughness.addOrUpdateTransientModifier(new AttributeModifier(TOUGHNESS_ID, TOUGHNESS_PEAK * bonus, AttributeModifier.Operation.ADD_VALUE));
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.tickCount % CHECK_INTERVAL == 0) {
            refresh(player);
        }
    }

    @SubscribeEvent
    public void onFall(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getSource().is(DamageTypeTags.IS_FALL) && wearsFullSet(player)) {
            event.setCanceled(true);
        }
    }
}
