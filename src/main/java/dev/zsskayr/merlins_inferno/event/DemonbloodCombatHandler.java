package dev.zsskayr.merlins_inferno.event;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Demonblood combat traits:
 * <ul>
 *   <li><b>Tools/Weapons:</b> Hatred of "humankind" - any tool/weapon in {@link ModTags.Items#DEMONBLOOD_TOOLS} deals
 *       10% extra damage to mobs in {@link ModTags.EntityTypes#HUMANS}.</li>
 *   <li><b>Armor ("Fúria do Demônio"):</b> Takes less damage from aggressive mobs (25% reduction with full set,
 *       5% per piece) and fire (50% reduction with full set, 5% per piece), but takes increased damage from
 *       neutral mobs (25% increase with full set, 5% per piece).</li>
 * </ul>
 */
public final class DemonbloodCombatHandler {

    private static final float HUMAN_DAMAGE_MULTIPLIER = 1.1F;

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        handleWeaponBonus(event);
        handleArmorDefense(event);
    }

    private void handleWeaponBonus(LivingIncomingDamageEvent event) {
        if (!event.getEntity().getType().is(ModTags.EntityTypes.HUMANS)) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            return;
        }

        ItemStack weapon = livingAttacker.getMainHandItem();
        if (weapon.is(ModTags.Items.DEMONBLOOD_TOOLS)) {
            event.setAmount(event.getAmount() * HUMAN_DAMAGE_MULTIPLIER);
        }
    }

    private void handleArmorDefense(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        int pieces = countDemonbloodArmorPieces(victim);
        if (pieces <= 0) {
            return;
        }

        // Fire damage reduction: full set provides heavy fire protection (50% reduction)
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            float fireMultiplier = (pieces == 4) ? 0.50F : (1.0F - (pieces * 0.05F));
            event.setAmount(event.getAmount() * fireMultiplier);
        }

        // Mob damage modifiers: reduced damage from aggressive monsters, increased damage from neutral mobs
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) {
            attacker = event.getSource().getDirectEntity();
        }

        if (attacker instanceof LivingEntity livingAttacker) {
            if (livingAttacker.getType().is(ModTags.EntityTypes.NEUTRAL_MOBS)) {
                float neutralMultiplier = (pieces == 4) ? 1.25F : (1.0F + (pieces * 0.05F));
                event.setAmount(event.getAmount() * neutralMultiplier);
            } else if (livingAttacker instanceof Enemy || livingAttacker instanceof Monster) {
                float aggressiveMultiplier = (pieces == 4) ? 0.75F : (1.0F - (pieces * 0.05F));
                event.setAmount(event.getAmount() * aggressiveMultiplier);
            }
        }
    }

    public static int countDemonbloodArmorPieces(LivingEntity entity) {
        int count = 0;
        for (ItemStack armorPiece : entity.getArmorSlots()) {
            if (armorPiece.is(ModTags.Items.DEMONBLOOD_ARMOR)) {
                count++;
            }
        }
        return count;
    }
}
