package dev.zsskayr.merlins_inferno.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Paragonium, Circle 2's Mundane route - the plain metal that answers neither magic nor corruption.
 * <ul>
 * <li><b>Hunter</b> (every {@link ModTags.Items#PARAGON_TOOLS} weapon, bow included): {@link #HUNTER_BONUS} more damage against anything
 * supernatural ({@link ModTags.EntityTypes#SUPERNATURAL}), and the touchstone - each consecutive hit on the same target ignores another
 * {@link #PIERCE_PER_HIT} of its armor, up to {@link #PIERCE_MAX}. Switching target or pausing {@link #CHAIN_WINDOW} ticks starts over.</li>
 * <li><b>Unshaken</b> (full Paragon armor): immune to Paralysis (Luminite) and Wither (Ebony); every other harmful effect lasts half as long.</li>
 * </ul>
 */
public final class ParagonCombatHandler {
    public static final float HUNTER_BONUS = 0.25F;
    public static final float PIERCE_PER_HIT = 0.10F;
    public static final float PIERCE_MAX = 0.50F;
    public static final long CHAIN_WINDOW = 60L;
    private static final String TARGET_KEY = "merlins_inferno_paragon_target";
    private static final String STACKS_KEY = "merlins_inferno_paragon_stacks";
    private static final String LAST_KEY = "merlins_inferno_paragon_last";

    /** Guards the re-add of a halved effect from being halved again. */
    private static final ThreadLocal<Boolean> SHORTENING = ThreadLocal.withInitial(() -> false);

    /** Armor ignored for a hit that lands with this many earlier hits in the chain. */
    public static float pierce(int earlierHits) {
        return Math.min(PIERCE_MAX, PIERCE_PER_HIT * Math.max(0, earlierHits));
    }

    public static boolean wearsFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.PARAGON_HELMET.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.PARAGON_CHESTPLATE.get())
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.PARAGON_LEGGINGS.get())
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.PARAGON_BOOTS.get());
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || !CombatWeapon.of(event.getSource()).is(ModTags.Items.PARAGON_TOOLS)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        // Humans never count, however much they dabble in the supernatural (witches, illagers, cultists, Elias) - unless they are
        // undead: a zombie villager is no longer a man.
        var type = victim.getType();
        if (type.is(ModTags.EntityTypes.SUPERNATURAL)
                && (!type.is(ModTags.EntityTypes.HUMANS) || type.is(net.minecraft.tags.EntityTypeTags.UNDEAD))) {
            event.setAmount(event.getAmount() * (1.0F + HUNTER_BONUS));
        }

        long now = victim.level().getGameTime();
        CompoundTag data = attacker.getPersistentData();
        boolean chained = data.getInt(TARGET_KEY) == victim.getId() && now - data.getLong(LAST_KEY) <= CHAIN_WINDOW;
        int earlier = chained ? data.getInt(STACKS_KEY) : 0;
        data.putInt(TARGET_KEY, victim.getId());
        data.putInt(STACKS_KEY, earlier + 1);
        data.putLong(LAST_KEY, now);

        float keep = 1.0F - pierce(earlier);
        if (keep < 1.0F) {
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * keep);
        }
    }

    @SubscribeEvent
    public void onEffectApplicable(MobEffectEvent.Applicable event) {
        MobEffectInstance effect = event.getEffectInstance();
        LivingEntity entity = event.getEntity();
        if (effect == null || SHORTENING.get() || entity.level().isClientSide() || !wearsFullSet(entity)) {
            return;
        }
        if (effect.is(ModEffects.PARALYSIS) || effect.is(MobEffects.WITHER)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }
        if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL || effect.isInfiniteDuration() || effect.getDuration() < 2) {
            return;
        }
        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        MobEffectInstance halved = new MobEffectInstance(effect.getEffect(), effect.getDuration() / 2, effect.getAmplifier(),
                effect.isAmbient(), effect.isVisible(), effect.showIcon());
        SHORTENING.set(true);
        try {
            entity.addEffect(halved);
        } finally {
            SHORTENING.set(false);
        }
    }
}
