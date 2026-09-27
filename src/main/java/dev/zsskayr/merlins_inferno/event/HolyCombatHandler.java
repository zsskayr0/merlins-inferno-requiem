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
 * "Holy" - the Angelical mirror of "Evil" (which had no counterpart on that side). A weapon enchanted with it
 * deals percentage damage bonuses/penalties by what it strikes, most reward going to what actually deserves a
 * righteous blade and least to anything that didn't pick the fight:
 * <ul>
 *     <li>+30% against {@link ModTags.EntityTypes#DEMON}s (checked first: several are also {@link Monster}s,
 *     and this takes priority over the generic aggressive bonus below);</li>
 *     <li>+20% against the Undead (checked next, same reason: zombies/skeletons are {@link Monster}s too);</li>
 *     <li>-20% against {@link ModTags.EntityTypes#PASSIVE_MOBS};</li>
 *     <li>-10% against {@link ModTags.EntityTypes#NEUTRAL_MOBS};</li>
 *     <li>+10% against anything else aggressive (a {@link Monster} not already claimed by a rule above).</li>
 * </ul>
 * Unlike Evil/Bane of Humanity, this can't be expressed as vanilla's additive enchantment effect (it's a
 * percentage, not a flat bonus) - see {@code holy.json}, which carries no vanilla effect at all, same as
 * {@code druids_touch}. Single level only (see the same file).
 */
public final class HolyCombatHandler {
    private static final float DEMON_DAMAGE_MULTIPLIER = 1.3F;
    private static final float UNDEAD_DAMAGE_MULTIPLIER = 1.2F;
    private static final float AGGRESSIVE_DAMAGE_MULTIPLIER = 1.1F;
    private static final float NEUTRAL_DAMAGE_MULTIPLIER = 0.9F;
    private static final float PASSIVE_DAMAGE_MULTIPLIER = 0.8F;

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (ModEnchantments.getHolyLevel(attacker.level(), weapon) <= 0) {
            return;
        }

        LivingEntity victim = event.getEntity();
        if (victim.getType().is(ModTags.EntityTypes.DEMON)) {
            event.setAmount(event.getAmount() * DEMON_DAMAGE_MULTIPLIER);
        } else if (victim.getType().is(EntityTypeTags.UNDEAD)) {
            event.setAmount(event.getAmount() * UNDEAD_DAMAGE_MULTIPLIER);
        } else if (victim.getType().is(ModTags.EntityTypes.PASSIVE_MOBS)) {
            event.setAmount(event.getAmount() * PASSIVE_DAMAGE_MULTIPLIER);
        } else if (victim.getType().is(ModTags.EntityTypes.NEUTRAL_MOBS)) {
            event.setAmount(event.getAmount() * NEUTRAL_DAMAGE_MULTIPLIER);
        } else if (victim instanceof Monster) {
            event.setAmount(event.getAmount() * AGGRESSIVE_DAMAGE_MULTIPLIER);
        }
    }
}
