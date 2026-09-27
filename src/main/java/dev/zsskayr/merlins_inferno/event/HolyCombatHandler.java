package dev.zsskayr.merlins_inferno.event;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import dev.zsskayr.merlins_inferno.registry.ModEnchantments;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * "Holy"'s one tier that vanilla's data-driven enchantment effects can't express: a bonus against
 * aggressive mobs in general, not just the specific tagged rosters ({@code holy.json} already covers
 * {@link ModTags.EntityTypes#DEMON}, the Undead, {@link ModTags.EntityTypes#NEUTRAL_MOBS} and
 * {@link ModTags.EntityTypes#PASSIVE_MOBS} entirely through vanilla's own effect system - no event
 * needed for those). There is no tag - vanilla or NeoForge's {@code c:} conventions - that enumerates
 * "every hostile mob", modded ones included; the one thing that reliably does is the {@link Monster}
 * base class every hostile mob (vanilla or modded) conventionally extends, and that can only be
 * checked from code.
 * <p>
 * Kept deliberately minimal for performance: one enchantment-level lookup and up to four tag checks,
 * only on a hit landed with a Holy weapon, only ever adding the flat, level-scaled bonus below when
 * none of {@code holy.json}'s own tiers already fired (checked in the same order, so nothing here can
 * double-count a demon or an undead that also happens to be a {@link Monster}).
 */
public final class HolyCombatHandler {
    private static final float BASE_BONUS = 1.0F;
    private static final float BONUS_PER_LEVEL_ABOVE_FIRST = 0.5F;

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        if (!(victim instanceof Monster) || victim.getType().is(ModTags.EntityTypes.DEMON)
                || victim.getType().is(EntityTypeTags.UNDEAD) || victim.getType().is(ModTags.EntityTypes.NEUTRAL_MOBS)
                || victim.getType().is(ModTags.EntityTypes.PASSIVE_MOBS)) {
            return; // not aggressive, or already handled by holy.json's own tagged tiers
        }

        ItemStack weapon = attacker.getMainHandItem();
        int level = ModEnchantments.getHolyLevel(attacker.level(), weapon);
        if (level > 0) {
            event.setAmount(event.getAmount() + BASE_BONUS + BONUS_PER_LEVEL_ABOVE_FIRST * (level - 1));
        }
    }
}
