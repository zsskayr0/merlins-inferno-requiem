package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Rowanwood's own intrinsic combat trait - baked into the tool tag
 * ({@link ModTags.Items#ROWANWOOD_TOOLS}), the same way Demonite's anti-"human" bonus and
 * Seraphium's passive/neutral/aggressive split are. A druidic, nature-attuned material: it bites
 * harder against anything magical/otherworldly ({@link ModTags.EntityTypes#MAGICAL_MOBS}), but
 * struggles against mundane, entirely non-magical fauna ({@link ModTags.EntityTypes#RIGID_MOBS}) -
 * there's nothing supernatural there for the wood's own attunement to latch onto.
 */
public final class RowanwoodCombatHandler {
    private static final float MAGICAL_DAMAGE_MULTIPLIER = 1.3F;
    private static final float RIGID_DAMAGE_MULTIPLIER = 0.7F;

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(ModTags.Items.ROWANWOOD_TOOLS)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        if (victim.getType().is(ModTags.EntityTypes.MAGICAL_MOBS)) {
            event.setAmount(event.getAmount() * MAGICAL_DAMAGE_MULTIPLIER);
        } else if (victim.getType().is(ModTags.EntityTypes.RIGID_MOBS)) {
            event.setAmount(event.getAmount() * RIGID_DAMAGE_MULTIPLIER);
        }
    }
}
