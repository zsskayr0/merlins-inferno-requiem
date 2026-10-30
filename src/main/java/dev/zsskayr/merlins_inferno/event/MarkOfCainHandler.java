package dev.zsskayr.merlins_inferno.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.mark.MarkHelper;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import dev.zsskayr.merlins_inferno.registry.ModEnchantments;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Everything the Mark of Cain does to its bearer ("Murder"), all keyed off {@link MarkHelper}.
 * <ul>
 *     <li><b>Strength and weakness:</b> faster regeneration, +30% damage dealt, +15% damage taken; immune to Poison, and
 *     the Wither does it no harm (the effect itself is left alone - the Dark Coven's boons live as long as it does);
 *     Holy weapons hurt more, Bane of Humanity and Evil weapons hurt far less.</li>
 *     <li><b>Hunger:</b> food fills half as much of the hunger bar; a kill fills some of it - a passive, neutral or
 *     villager victim as much as a sweet berry, anything else half of that.</li>
 *     <li><b>Mark of the Murder:</b> one roll every 7 minutes; if it comes up, the next blow taken is returned 7-fold to
 *     whoever struck it, then 7 minutes of cooldown.</li>
 *     <li><b>The obligation:</b> the bearer must kill a passive or neutral mob (a villager counts) every 20 minutes. Late,
 *     the Mark takes 2 hearts of maximum health for every 5 minutes overdue - health no regeneration can give back - and
 *     empties the hunger bar, until the next kill.</li>
 *     <li><b>Omen:</b> now and then, walking into a village brings a short Bad Omen.</li>
 * </ul>
 * Hostility of the Angelical route and the Imps' truce live in those mobs' own goals.
 */
public class MarkOfCainHandler {
    private static final int SECOND = 20;
    private static final long MURDER_INTERVAL = 7L * 60 * SECOND;
    private static final double MURDER_CHANCE = 0.5;
    private static final float MURDER_FACTOR = 7.0F;
    private static final long KILL_WINDOW = 20L * 60 * SECOND;
    /** While the Dark Coven's Pact lasts under the Mark: exhaustion a second (about a hunger point every 4 s), and ticks the kill deadline loses a second on top of the usual - 4x the clock. */
    private static final float PACT_EXHAUSTION_PER_SECOND = 1.0F;
    private static final long PACT_CLOCK_EXTRA = 3L * SECOND;
    private static final long TIER_STEP = 5L * 60 * SECOND;
    private static final double HEALTH_PER_TIER = 4.0;
    private static final double MIN_MAX_HEALTH = 2.0;
    private static final float REGEN_PER_SECOND = 1.0F;
    private static final float DEALT_FACTOR = 1.3F;
    private static final float TAKEN_FACTOR = 1.15F;
    private static final float HOLY_FACTOR = 1.5F;
    private static final float HOLY_FACTOR_PER_LEVEL_ABOVE_FIRST = 0.25F;
    private static final float BANE_FACTOR = 0.5F;
    /** Hunger a kill gives back: the victims the obligation asks for fill a sweet berry's worth, any other kill half of that. */
    private static final int KILL_FOOD_OBLIGATION = 2;
    private static final int KILL_FOOD_OTHER = 1;
    private static final float KILL_SATURATION_MODIFIER = 0.1F;
    private static final double OMEN_CHANCE = 0.03;
    private static final int OMEN_DURATION = 30 * SECOND;
    private static final ResourceLocation PENALTY_ID = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "cain_penalty");

    /** The hunger bar as of the last second, to tell eaten food (halved) from what the Mark itself gave. */
    private final java.util.Map<java.util.UUID, Integer> lastFood = new java.util.HashMap<>();

    /** Whoever was inside a village the last time we looked - an omen is rolled on the way in only. */
    private final java.util.Set<java.util.UUID> inVillage = new java.util.HashSet<>();

    public static boolean bearsCain(LivingEntity entity) {
        return MarkHelper.hasMark(entity, ModItems.MARK_OF_CAIN.get());
    }

    /** How late the bearer is, in penalty tiers: 0 while on time, 1 from the deadline, one more per 5 minutes. */
    public static int penaltyTier(long now, long deadline) {
        return now < deadline ? 0 : (int) ((now - deadline) / TIER_STEP) + 1;
    }

    /** What the obligation asks for: anything passive or neutral, villagers and traders included. */
    public static boolean isMurderVictim(LivingEntity victim) {
        return victim.getType().is(ModTags.EntityTypes.PASSIVE_MOBS) || victim.getType().is(ModTags.EntityTypes.NEUTRAL_MOBS)
                || victim instanceof AbstractVillager;
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && player.tickCount % SECOND == 0) {
            tickSecond(player);
        }
    }

    /** One second of the Mark; public so the game tests can step it. */
    public void tickSecond(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (!bearsCain(player)) {
            release(player);
            return;
        }
        if (player.getData(ModAttachments.CAIN_KILL_DEADLINE) == 0L) {
            player.setData(ModAttachments.CAIN_KILL_DEADLINE, now + KILL_WINDOW);
            player.setData(ModAttachments.CAIN_MURDER_AT, now + MURDER_INTERVAL);
        }

        player.removeEffect(MobEffects.POISON);
        if (player.isAlive() && player.getHealth() < player.getMaxHealth()) {
            player.heal(REGEN_PER_SECOND);
        }

        // Mark of the Murder: one roll per interval, win or lose.
        if (now >= player.getData(ModAttachments.CAIN_MURDER_AT)) {
            player.setData(ModAttachments.CAIN_MURDER_AT, now + MURDER_INTERVAL);
            if (!player.getData(ModAttachments.CAIN_MURDER_ARMED) && player.getRandom().nextDouble() < MURDER_CHANCE) {
                player.setData(ModAttachments.CAIN_MURDER_ARMED, true);
            }
        }
        if (player.getData(ModAttachments.CAIN_MURDER_ARMED)) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.4, 0.8, 0.4, 0.02);
        }

        // The Pact feeds on the Mark: a ravenous hunger, and the clock to the next kill runs several times as fast.
        if (player.hasEffect(dev.zsskayr.merlins_inferno.registry.ModEffects.DEMONIC_PACT)) {
            player.causeFoodExhaustion(PACT_EXHAUSTION_PER_SECOND);
            player.setData(ModAttachments.CAIN_KILL_DEADLINE, player.getData(ModAttachments.CAIN_KILL_DEADLINE) - PACT_CLOCK_EXTRA);
        }

        // The obligation.
        int tier = penaltyTier(now, player.getData(ModAttachments.CAIN_KILL_DEADLINE));
        applyPenalty(player, tier);
        if (tier > 0) {
            player.getFoodData().setFoodLevel(0);
            player.getFoodData().setSaturation(0.0F);
            level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.4, 0.8, 0.4, 0.02);
        } else {
            // Whatever the bar rose by since last second, short of the Mark's own kills, was food: half of it counts.
            Integer last = lastFood.get(player.getUUID());
            int food = player.getFoodData().getFoodLevel();
            if (last != null && food > last) {
                player.getFoodData().setFoodLevel(last + (food - last + 1) / 2);
            }
        }
        lastFood.put(player.getUUID(), player.getFoodData().getFoodLevel());

        // Omen on entering a village.
        boolean village = level.isVillage(player.blockPosition());
        boolean entered = village && inVillage.add(player.getUUID());
        if (!village) {
            inVillage.remove(player.getUUID());
        }
        if (entered && player.getRandom().nextDouble() < OMEN_CHANCE) {
            player.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, OMEN_DURATION, 0));
        }
    }

    /** Forgets everything of the Mark when the player no longer bears it. */
    private void release(Player player) {
        lastFood.remove(player.getUUID());
        applyPenalty(player, 0);
        if (player.getData(ModAttachments.CAIN_KILL_DEADLINE) != 0L) {
            player.setData(ModAttachments.CAIN_KILL_DEADLINE, 0L);
            player.setData(ModAttachments.CAIN_MURDER_AT, 0L);
            player.setData(ModAttachments.CAIN_MURDER_ARMED, false);
        }
    }

    private static void applyPenalty(Player player, int tier) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        if (tier == 0) {
            if (health.getModifier(PENALTY_ID) != null) {
                health.removeModifier(PENALTY_ID);
            }
            return;
        }
        double lost = Math.min(HEALTH_PER_TIER * tier, health.getBaseValue() - MIN_MAX_HEALTH);
        AttributeModifier current = health.getModifier(PENALTY_ID);
        if (current == null || current.amount() != -lost) {
            health.addOrReplacePermanentModifier(new AttributeModifier(PENALTY_ID, -lost, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** A kill of the right kind pays the debt and starts the next 20 minutes. */
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player dead) {
            lastFood.remove(dead.getUUID()); // the respawned bar is full: not food
        }
        if (event.getSource().getEntity() instanceof Player player && player.level() instanceof ServerLevel level && bearsCain(player)) {
            boolean victim = isMurderVictim(event.getEntity());
            if (victim) {
                player.setData(ModAttachments.CAIN_KILL_DEADLINE, level.getGameTime() + KILL_WINDOW);
                applyPenalty(player, 0);
            }
            player.getFoodData().eat(victim ? KILL_FOOD_OBLIGATION : KILL_FOOD_OTHER, KILL_SATURATION_MODIFIER);
            lastFood.put(player.getUUID(), player.getFoodData().getFoodLevel());
        }
    }

    @SubscribeEvent
    public void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (bearsCain(event.getEntity())) {
            MobEffectInstance effect = event.getEffectInstance();
            if (effect != null && effect.is(MobEffects.POISON)) {
                event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            }
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getSource().is(DamageTypes.WITHER) && bearsCain(victim)) {
            event.setCanceled(true); // the effect stays (the Dark Coven runs on it), it just does not hurt
            return;
        }
        Entity source = event.getSource().getEntity();
        float amount = event.getAmount();
        boolean changed = false;

        // The returned blow is exactly 7x what was taken: nothing else touches it.
        if (source instanceof LivingEntity attacker && bearsCain(attacker) && !event.getSource().is(DamageTypes.THORNS)) {
            amount *= DEALT_FACTOR;
            changed = true;
        }
        if (victim instanceof Player player && bearsCain(player)) {
            amount *= TAKEN_FACTOR;
            changed = true;
            if (source instanceof LivingEntity attacker) {
                ItemStack weapon = attacker.getMainHandItem();
                int holy = ModEnchantments.getHolyLevel(attacker.level(), weapon);
                if (holy > 0) {
                    amount *= HOLY_FACTOR + HOLY_FACTOR_PER_LEVEL_ABOVE_FIRST * (holy - 1);
                }
                if (ModEnchantments.getBaneOfHumanityLevel(attacker.level(), weapon) > 0
                        || ModEnchantments.getEvilLevel(attacker.level(), weapon) > 0) {
                    amount *= BANE_FACTOR;
                }
                if (attacker != player && player.getData(ModAttachments.CAIN_MURDER_ARMED) && player.level() instanceof ServerLevel level) {
                    long now = level.getGameTime();
                    player.setData(ModAttachments.CAIN_MURDER_ARMED, false);
                    player.setData(ModAttachments.CAIN_MURDER_AT, Math.max(player.getData(ModAttachments.CAIN_MURDER_AT), now + MURDER_INTERVAL));
                    level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.7F, 0.5F);
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
                    attacker.hurt(level.damageSources().thorns(player), amount * MURDER_FACTOR);
                }
            }
        }
        if (changed) {
            event.setAmount(amount);
        }
    }
}
