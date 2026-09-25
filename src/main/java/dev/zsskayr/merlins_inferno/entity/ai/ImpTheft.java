package dev.zsskayr.merlins_inferno.entity.ai;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;

import dev.zsskayr.merlins_inferno.entity.ImpEntity;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Shared rules and bookkeeping for the Imp's thefts. The bookkeeping (victim protection, item
 * reservation, "ignore this item" marks) lives in each entity's persistent data as absolute game
 * times, so it survives saves and needs no side tables.
 */
public final class ImpTheft {
    /** After a successful theft the victim is off-limits to every imp for this long (30s). */
    public static final int VICTIM_PROTECTION_TICKS = 600;
    /** Items an imp dropped (hit thief, death loot) are ignored by imps for this long (30s). */
    public static final int DROP_IGNORE_TICKS = 600;
    /** A ground item an imp is heading for is reserved so only one imp goes after it. */
    public static final int RESERVATION_TICKS = 200;

    private static final String KEY_PROTECTED_UNTIL = "merlins_inferno_imp_protected_until";
    private static final String KEY_IGNORE_UNTIL = "merlins_inferno_imp_ignore_until";
    private static final String KEY_RESERVED_BY = "merlins_inferno_imp_reserved_by";
    private static final String KEY_RESERVED_UNTIL = "merlins_inferno_imp_reserved_until";

    private ImpTheft() {
    }

    // --- what can be taken ---

    /** Gold nugget / ingot / raw gold. */
    public static boolean isStealableMaterial(ItemStack stack) {
        return stack.is(ModTags.Items.IMP_STEALABLE);
    }

    /** Any golden tool or weapon (sword, pickaxe, axe, shovel, hoe) - only ever taken from mobs' hands, never the player's. */
    public static boolean isGoldenGear(ItemStack stack) {
        return stack.getItem() instanceof TieredItem tiered && tiered.getTier() == Tiers.GOLD;
    }

    // --- victim protection ("trégua") ---

    public static boolean isProtected(Entity victim, long gameTime) {
        return victim.getPersistentData().getLong(KEY_PROTECTED_UNTIL) > gameTime;
    }

    public static void protect(Entity victim, long gameTime) {
        victim.getPersistentData().putLong(KEY_PROTECTED_UNTIL, gameTime + VICTIM_PROTECTION_TICKS);
    }

    // --- dropped items ---

    public static boolean isIgnored(ItemEntity item, long gameTime) {
        return item.getPersistentData().getLong(KEY_IGNORE_UNTIL) > gameTime;
    }

    public static void ignore(ItemEntity item, long untilGameTime) {
        item.getPersistentData().putLong(KEY_IGNORE_UNTIL, untilGameTime);
    }

    // --- one imp per target at a time ---

    /** True if some other imp has reserved {@code target} and the reservation hasn't lapsed. */
    public static boolean isReservedByOther(Entity target, UUID me, long gameTime) {
        CompoundTag data = target.getPersistentData();
        return data.getLong(KEY_RESERVED_UNTIL) > gameTime
                && data.hasUUID(KEY_RESERVED_BY) && !data.getUUID(KEY_RESERVED_BY).equals(me);
    }

    public static void reserve(Entity target, UUID me, long gameTime) {
        CompoundTag data = target.getPersistentData();
        data.putUUID(KEY_RESERVED_BY, me);
        data.putLong(KEY_RESERVED_UNTIL, gameTime + RESERVATION_TICKS);
    }

    public static void release(Entity target, UUID me) {
        CompoundTag data = target.getPersistentData();
        if (data.hasUUID(KEY_RESERVED_BY) && data.getUUID(KEY_RESERVED_BY).equals(me)) {
            data.remove(KEY_RESERVED_BY);
            data.remove(KEY_RESERVED_UNTIL);
        }
    }

    // --- angry victims ---

    /**
     * Makes a robbed mob furious at the thief - and only that mob (nearby piglins don't join in).
     * Piglins run on the Brain system, where anger is the {@code ANGRY_AT} memory (the same one
     * vanilla sets when you hit one); everything else just gets an attack target.
     */
    public static void angerVictim(Mob victim, ImpEntity thief) {
        if (victim instanceof Piglin || victim instanceof PiglinBrute) {
            victim.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, thief.getUUID(), 600L);
        }
        if (victim instanceof NeutralMob neutral) {
            neutral.setPersistentAngerTarget(thief.getUUID());
            neutral.startPersistentAngerTimer();
        }
        victim.setTarget(thief);
    }

    /** Whether {@code mob} is currently trying to get at {@code imp} (plain target or Brain attack target). */
    public static boolean isHunting(Mob mob, ImpEntity imp) {
        if (mob.getTarget() == imp) {
            return true;
        }
        if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            LivingEntity target = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
            return target == imp;
        }
        return false;
    }
}
