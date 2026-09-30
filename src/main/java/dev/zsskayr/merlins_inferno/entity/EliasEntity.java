package dev.zsskayr.merlins_inferno.entity;

import java.util.EnumSet;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.blockentity.SacredAltarBlockEntity;
import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.worldgen.structure.GreatBell;

/**
 * Elias - the Angelical path's miniboss: a Lyrium-mad zealot who keeps vigil over a shrine, blindfold
 * on, dragging a censer on a chain. Not an angel: a faithful, broken by what he worships.
 * <ul>
 *     <li><b>Stats:</b> the miniboss ruler (stronger than Ostara: 400 health, 18 damage, some armor), slow, with the chain
 *     reaching past a normal swing.</li>
 *     <li><b>The Great Bell (signature):</b> while he fights, the church's bell tolls every minute. Each toll
 *     makes him stronger and lets him regenerate more, but he also takes more damage. On the seventh toll - or
 *     when his health drops below 30% - the bell shatters and he goes into <b>fury</b>: faster, hitting harder,
 *     no longer regenerating, and taking normal damage again. See {@link GreatBell}.</li>
 *     <li><b>Sanctified (explicit exception):</b> every blow he lands raises Sanctified on a player by a level.
 *     The rule elsewhere is that mobs never spread it ({@code SanctifiedCombatHandler}); he does it
 *     deliberately, here, and nowhere else. Milk clears it.</li>
 *     <li><b>Fury:</b> the Sanctified effect climbs one level higher.</li>
 *     <li><b>Loot:</b> Celestial Essence (data/merlins_inferno/loot_table/entities/elias.json).</li>
 * </ul>
 * Death starts the altar's cooldown; a diamond on the altar wakes the next one.
 * <p>
 * Animated with GeckoLib ({@code geo/elias.geo.json}, {@code animations/elias.animation.json}): the same clip set
 * the Sacred Priest has (idle/idle_hands_on_hips/walk/run/attack/pray_start/pray/pray_end/death - he too kneels
 * in vigil, Seraphium Sword planted, when left in peace), plus a fury-only moveset once {@link #enraged} is set:
 * idle_fury, run_fury and a wider, harder two-handed attack_fury.
 */
public class EliasEntity extends Monster implements GeoEntity {
    public static final double MAX_HEALTH = 400.0;
    public static final double ATTACK_DAMAGE = 18.0;
    public static final double MOVEMENT_SPEED = 0.2;

    /** The chain reaches this far past a normal melee swing on each side. */
    private static final double CHAIN_REACH_BONUS = 0.5;
    private static final double HOME_RADIUS = 24.0;

    // --- the Great Bell's hold on him ---
    /** How often (ticks) the bell is checked and its regeneration applied. */
    private static final int BELL_PERIOD = 10;
    /** Ticks of combat between tolls: one minute. */
    private static final int TOLL_INTERVAL = 1200;
    /** The bell breaks on this toll. */
    private static final int MAX_TOLLS = 7;
    /** Health regained per toll per period: 0.5 per 10 ticks = 1 health/s per toll, 7/s at the last one. */
    private static final float TOLL_HEAL_PER_PERIOD = 0.5F;
    /** Each toll: +6% attack damage, +8% damage taken. */
    private static final double TOLL_STRENGTH_EACH = 0.06;
    private static final float TOLL_VULNERABILITY_EACH = 0.08F;
    /** Fury: extra attack damage on top of what the tolls gave him. */
    private static final double FURY_STRENGTH = 0.25;
    private static final int TOLL_NOTICE_RADIUS = 48;

    // --- Sanctified per blow ---
    private static final int SANCTIFIED_DURATION = 200;
    private static final int SANCTIFIED_MAX_AMPLIFIER = 2;           // level III
    private static final int SANCTIFIED_MAX_AMPLIFIER_ENRAGED = 3;   // level IV

    /** Below this fraction of his health the bell breaks, whatever the toll count. */
    private static final float ENRAGE_HEALTH_FRACTION = 0.3F;
    private static final ResourceLocation ENRAGE_MODIFIER = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "elias_enrage");
    private static final ResourceLocation FURY_STRENGTH_MODIFIER = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "elias_fury_strength");
    private static final ResourceLocation TOLL_STRENGTH_MODIFIER = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "elias_toll_strength");

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.merlins_inferno.elias"),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

    @Nullable
    private BlockPos altarPos;
    @Nullable
    private BlockPos bellPos;
    private boolean bellLooked;
    private int tolls;
    private int tollTimer;
    private boolean enraged;

    // --- GeckoLib animation ---
    private static final double WALK_SPEED_SQR = 0.0016;
    private static final double WALK_HYSTERESIS_SQR = 0.0006;
    private static final double RUN_SPEED_SQR = 0.02;
    /** The death clip is 2.5 s; the body lingers a little past it so the last pose is held before it vanishes. */
    private static final int DEATH_ANIMATION_TICKS = 55;

    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation.elias.attack");
    private static final RawAnimation ATTACK_FURY_ANIMATION = RawAnimation.begin().thenPlay("animation.elias.attack_fury");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("animation.elias.death");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private String animationState;
    private String pendingAnimationState;
    private int pendingAnimationTick;
    private RawAnimation loopAnimation = RawAnimation.begin().thenLoop("animation.elias.idle");
    private int deathTicks;

    public EliasEntity(EntityType<? extends EliasEntity> type, Level level) {
        super(type, level);
        this.xpReward = 50;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** Ties him to a shrine: he doesn't wander from it, and its altar starts its cooldown when he dies. */
    public void setHome(BlockPos altar) {
        this.altarPos = altar.immutable();
        this.restrictTo(this.altarPos, (int) HOME_RADIUS);
    }

    // ------------------------------------------------------------------------------------------
    // AI
    // ------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(3, new PrayGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected AABB getAttackBoundingBox() {
        return super.getAttackBoundingBox().inflate(CHAIN_REACH_BONUS, 0.0, CHAIN_REACH_BONUS);
    }

    /** Every blow raises the player's Sanctified a level (see the class doc for why this is his alone). */
    @Override
    public boolean doHurtTarget(Entity target) {
        this.level().playSound(null, this.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 1.0F, 0.7F);
        if (!this.level().isClientSide) {
            this.triggerAnim("move", this.enraged ? "attack_fury" : "attack");
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Player player) {
            MobEffectInstance current = player.getEffect(ModEffects.SANCTIFIED);
            int cap = this.enraged ? SANCTIFIED_MAX_AMPLIFIER_ENRAGED : SANCTIFIED_MAX_AMPLIFIER;
            int next = current == null ? 0 : Math.min(cap, current.getAmplifier() + 1);
            player.addEffect(new MobEffectInstance(ModEffects.SANCTIFIED, SANCTIFIED_DURATION, next));
        }
        return hit;
    }

    // ------------------------------------------------------------------------------------------
    // The Great Bell
    // ------------------------------------------------------------------------------------------

    /** One-off (per load) look at the altar for where the bell hangs; an Elias with no altar has no bell. */
    private void findBell(ServerLevel level) {
        this.bellLooked = true;
        this.bellPos = null;
        if (this.altarPos != null && level.getBlockEntity(this.altarPos) instanceof SacredAltarBlockEntity altar) {
            this.bellPos = altar.getBellPos();
        }
    }

    private void tickBell(ServerLevel level) {
        if (this.enraged || this.bellPos == null) {
            return;
        }
        if (!GreatBell.isIntact(level, this.bellPos)) {
            this.fury(level); // someone broke it for him
            return;
        }
        if (this.getTarget() != null) {
            this.tollTimer += BELL_PERIOD;
            if (this.tollTimer >= TOLL_INTERVAL) {
                this.tollTimer = 0;
                this.toll(level);
                if (this.enraged) {
                    return;
                }
            }
        }
        if (this.tolls > 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(TOLL_HEAL_PER_PERIOD * this.tolls);
        }
    }

    private void toll(ServerLevel level) {
        this.tolls++;
        GreatBell.ring(level, this.bellPos);
        this.applyTollStrength();
        this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
        this.tellNearby(level, Component.translatable("message.merlins_inferno.elias.toll", this.tolls, MAX_TOLLS));
        if (this.tolls >= MAX_TOLLS) {
            this.fury(level);
        }
    }

    private void applyTollStrength() {
        AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.removeModifier(TOLL_STRENGTH_MODIFIER);
            if (this.tolls > 0) {
                damage.addTransientModifier(new AttributeModifier(TOLL_STRENGTH_MODIFIER, TOLL_STRENGTH_EACH * this.tolls, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    private void tellNearby(ServerLevel level, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) <= (double) TOLL_NOTICE_RADIUS * TOLL_NOTICE_RADIUS) {
                player.displayClientMessage(message, true);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Tick, damage, frenzy
    // ------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (!this.bellLooked) {
            this.findBell(serverLevel);
        }
        if (this.tickCount % BELL_PERIOD == 0 && this.isAlive()) {
            this.tickBell(serverLevel);
        }
        if (!this.enraged && this.getHealth() < this.getMaxHealth() * ENRAGE_HEALTH_FRACTION) {
            this.fury(serverLevel);
        }
    }

    /** The bell shatters and he goes into fury: faster, stronger, no more regeneration, no more vulnerability. */
    private void fury(ServerLevel level) {
        if (this.enraged) {
            return;
        }
        if (this.bellPos != null) {
            GreatBell.shatter(level, this.bellPos);
        }
        this.enrage();
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.2F, 1.3F);
        this.tellNearby(level, Component.translatable("message.merlins_inferno.elias.fury"));
    }

    /** The fury's stats, applied silently (also on reload). */
    private void enrage() {
        this.enraged = true;
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(ENRAGE_MODIFIER)) {
            speed.addTransientModifier(new AttributeModifier(ENRAGE_MODIFIER, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null && !damage.hasModifier(FURY_STRENGTH_MODIFIER)) {
            damage.addTransientModifier(new AttributeModifier(FURY_STRENGTH_MODIFIER, FURY_STRENGTH, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** Every toll leaves him softer: 8% more damage taken per toll, until the bell breaks. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && !this.enraged && this.tolls > 0) {
            amount *= 1.0F + TOLL_VULNERABILITY_EACH * this.tolls;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (this.level() instanceof ServerLevel serverLevel && this.altarPos != null
                && serverLevel.getBlockEntity(this.altarPos) instanceof SacredAltarBlockEntity altar) {
            altar.startCooldown(serverLevel);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** Lingers for the whole death clip instead of vanilla's 20 ticks, like the Sacred Priest's. */
    @Override
    protected void tickDeath() {
        ++this.deathTicks;
        if (this.deathTicks >= DEATH_ANIMATION_TICKS && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    // ------------------------------------------------------------------------------------------
    // GeckoLib animation: one controller owns the whole pose. Praying is read straight from the vanilla pose
    // PrayGoal sets (disabled once enraged - see PrayGoal.canUse); the fury moveset takes over entirely once
    // this.enraged is set, replacing idle/walk/run/attack outright rather than blending with them.
    // ------------------------------------------------------------------------------------------

    private String desiredAnimationState() {
        if (this.getPose() == Pose.CROUCHING) {
            return "pray";
        }
        double dx = this.getX() - this.xOld;
        double dz = this.getZ() - this.zOld;
        double speedSqr = dx * dx + dz * dz;
        boolean wasMoving = "walk".equals(this.animationState) || "run".equals(this.animationState) || "run_fury".equals(this.animationState);
        boolean moving = speedSqr > (wasMoving ? WALK_HYSTERESIS_SQR : WALK_SPEED_SQR);
        if (this.enraged) {
            return moving ? "run_fury" : "idle_fury";
        }
        if (!moving) {
            return "idle";
        }
        return speedSqr > RUN_SPEED_SQR ? "run" : "walk";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 4, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(DEATH_ANIMATION);
            }
            String desired = this.desiredAnimationState();
            if (this.animationState == null) {
                this.animationState = desired;
                this.loopAnimation = RawAnimation.begin().thenLoop("animation.elias." + desired);
            } else if (!desired.equals(this.animationState)) {
                // Only entering/leaving the kneeling prayer is debounced (it is the one state with bracket clips
                // to protect from a one-tick flicker); idle/walk/run/fury states swap the moment the threshold
                // does, so a slow, stop-start wander never gets stuck showing idle.
                boolean bracketed = "pray".equals(desired) || "pray".equals(this.animationState);
                if (bracketed) {
                    if (!desired.equals(this.pendingAnimationState)) {
                        this.pendingAnimationState = desired;
                        this.pendingAnimationTick = this.tickCount;
                        return state.setAndContinue(this.loopAnimation);
                    } else if (this.tickCount - this.pendingAnimationTick < 3) {
                        return state.setAndContinue(this.loopAnimation);
                    }
                }
                RawAnimation next = RawAnimation.begin();
                if ("pray".equals(desired)) {
                    next = next.thenPlay("animation.elias.pray_start");
                } else if ("pray".equals(this.animationState)) {
                    next = next.thenPlay("animation.elias.pray_end");
                }
                this.loopAnimation = next.thenLoop("animation.elias." + desired);
                this.animationState = desired;
            }
            return state.setAndContinue(this.loopAnimation);
        }).triggerableAnim("attack", ATTACK_ANIMATION).triggerableAnim("attack_fury", ATTACK_FURY_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    // ------------------------------------------------------------------------------------------
    // Boss bar, save
    // ------------------------------------------------------------------------------------------

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Enraged", this.enraged);
        compound.putInt("Tolls", this.tolls);
        compound.putInt("TollTimer", this.tollTimer);
        if (this.altarPos != null) {
            compound.putLong("Altar", this.altarPos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Altar")) {
            this.setHome(BlockPos.of(compound.getLong("Altar")));
        }
        this.tolls = compound.getInt("Tolls");
        this.tollTimer = compound.getInt("TollTimer");
        if (this.tolls > 0) {
            this.applyTollStrength();
            this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
        }
        if (compound.getBoolean("Enraged")) {
            this.enrage();
        }
    }

    // ------------------------------------------------------------------------------------------
    // Sounds - vanilla placeholders until the mod has its own audio assets.
    // ------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.CHAIN_STEP, 0.4F, 0.8F);
    }

    /** Kneels in silent vigil, sword planted, while left in peace - never once the bell has broken and fury has taken him. */
    private static class PrayGoal extends Goal {
        private final EliasEntity elias;
        private int ticksLeft;

        PrayGoal(EliasEntity elias) {
            this.elias = elias;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return !this.elias.enraged && this.elias.getTarget() == null && this.elias.getRandom().nextInt(150) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticksLeft > 0 && !this.elias.enraged && this.elias.getTarget() == null;
        }

        @Override
        public void start() {
            this.ticksLeft = 300 + this.elias.getRandom().nextInt(400);
            this.elias.getNavigation().stop();
            this.elias.setPose(Pose.CROUCHING);
        }

        @Override
        public void tick() {
            this.ticksLeft--;
        }

        @Override
        public void stop() {
            this.elias.setPose(Pose.STANDING);
        }
    }
}
