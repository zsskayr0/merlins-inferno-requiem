package dev.zsskayr.merlins_inferno.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
import dev.zsskayr.merlins_inferno.registry.ModTags;
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
 *     <li><b>Sanctified (explicit exception):</b> every blow he lands raises Sanctified on whoever it hits - player or
 *     creature - by a level.
 *     The rule elsewhere is that mobs never spread it ({@code SanctifiedCombatHandler}); he does it
 *     deliberately, here, and nowhere else. Milk clears it.</li>
 *     <li><b>Fury:</b> the Sanctified effect climbs one level higher.</li>
 *     <li><b>Loot:</b> Celestial Essence (data/merlins_inferno/loot_table/entities/elias.json).</li>
 * </ul>
 * Death starts the altar's cooldown; a diamond on the altar wakes the next one.
 * <p>
 * Animated with GeckoLib ({@code geo/elias.geo.json}, {@code animations/elias.animation.json}): a weary knight
 * who fights because he was ordered to. Clips: idle, walk, run, hurt, death and six attacks - three light
 * (right blade, left blade, both blades crossing) and three heavy (one blade overhead, both blades overhead,
 * a spin). The attack goal picks one, plays it, and lands the damage at the clip's impact frame, so a
 * well-timed dodge makes him whiff. Besides players he hunts undead and demons on sight.
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
    /** Baseline recovery, independent of the bell (an Elias without an altar has none): per BELL_PERIOD. */
    private static final float IDLE_REGEN_PER_PERIOD = 2.0F;     // 4 health/s once he has lost his target
    private static final float COMBAT_REGEN_PER_PERIOD = 0.25F;  // 0.5 health/s mid-fight, none in fury

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
    private static final String ANIM = "animation.elias_mk2.";
    /** Walk/run switch on real movement; the low "off" threshold stops a standing Elias from sliding on the idle clip. */
    private static final double MOVE_ON_SQR = 0.0005;
    private static final double MOVE_OFF_SQR = 0.00015;
    private static final double RUN_SPEED_SQR = 0.02;
    /** The death clip is 4 s (80 ticks); the body lingers a little past it so the last pose is held before it vanishes. */
    private static final int DEATH_ANIMATION_TICKS = 90;
    private static final int HURT_ANIMATION_GAP = 14;

    private static final ResourceLocation ATTACK_MULTIPLIER = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "elias_attack_multiplier");

    /**
     * The attack set. {@code hitTick} is the clip's impact frame (seconds * 20), {@code length} the clip length in
     * ticks, {@code reach} how far past his body the blades bite, {@code radius} &gt; 0 makes it a sweep that hits
     * everything around him instead of the one target.
     */
    enum Attack {
        LIGHT_RIGHT("attack_light_right", 23, 10, 1.0, 3.2, 0.0, 0.2, 3.0, false, 8),
        LIGHT_LEFT("attack_light_left", 23, 10, 1.0, 3.2, 0.0, 0.2, 3.0, false, 8),
        LIGHT_DUAL("attack_light_dual", 26, 12, 1.35, 3.4, 0.0, 0.4, 2.0, false, 12),
        HEAVY_SINGLE("attack_heavy_single", 42, 27, 1.9, 3.8, 0.0, 0.9, 2.0, true, 18),
        HEAVY_DUAL("attack_heavy_dual", 50, 30, 2.3, 4.0, 1.6, 1.1, 1.2, true, 22),
        HEAVY_SPIN("attack_heavy_spin", 50, 21, 1.45, 3.9, 3.9, 0.7, 1.0, true, 18);

        final String clip;
        final int length;
        final int hitTick;
        final double damage;
        final double reach;
        final double radius;
        final double knockback;
        final double weight;
        final boolean heavy;
        final int recovery;

        Attack(String clip, int length, int hitTick, double damage, double reach, double radius, double knockback, double weight, boolean heavy, int recovery) {
            this.clip = clip;
            this.length = length;
            this.hitTick = hitTick;
            this.damage = damage;
            this.reach = reach;
            this.radius = radius;
            this.knockback = knockback;
            this.weight = weight;
            this.heavy = heavy;
            this.recovery = recovery;
        }
    }

    /** Heavies are rationed: this many ticks between them (halved in fury). */
    private static final int HEAVY_COOLDOWN = 110;
    private static final int SPIN_COOLDOWN = 220;

    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold(ANIM + "death");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private String animationState;
    private RawAnimation loopAnimation = RawAnimation.begin().thenLoop(ANIM + "idle");
    private int deathTicks;
    /** Server side: true from the moment an attack clip starts until it has fully played out. */
    private boolean attacking;
    private int heavyCooldown;
    private int spinCooldown;
    private int lastHurtAnimTick = -100;

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
        this.goalSelector.addGoal(2, new EliasAttackGoal(this));
        // He keeps vigil: long stretches standing still, an occasional slow wander (never the old restless stroll).
        WaterAvoidingRandomStrollGoal stroll = new WaterAvoidingRandomStrollGoal(this, 0.5);
        stroll.setInterval(360);
        this.goalSelector.addGoal(7, stroll);
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // Ordered to purge the unholy: undead and demons are hunted on sight.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false, EliasEntity::isUnholy));
    }

    /** Imps are beneath his notice: never targeted, never swept, and not even a reason to fight back. */
    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof ImpEntity) && super.canAttack(target);
    }

    /** Undead and demons are fair game for him (see {@link EntityTypeTags#UNDEAD} and the mod's demon tag). */
    private static boolean isUnholy(LivingEntity living) {
        return !(living instanceof ImpEntity) && (living.getType().is(EntityTypeTags.UNDEAD) || living.getType().is(ModTags.EntityTypes.DEMON));
    }

    /** Who a sweeping blade may hit: players and the unholy, never his own congregation or villagers. */
    private boolean isEnemy(LivingEntity living) {
        if (living == this || !living.isAlive()) {
            return false;
        }
        if (living instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return !(living instanceof ImpEntity) && isUnholy(living);
    }

    @Override
    protected AABB getAttackBoundingBox() {
        return super.getAttackBoundingBox().inflate(CHAIN_REACH_BONUS, 0.0, CHAIN_REACH_BONUS);
    }

    /** Every blow raises the victim's Sanctified a level - player or creature alike (see the class doc for why this is his alone). */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity victim) {
            MobEffectInstance current = victim.getEffect(ModEffects.SANCTIFIED);
            int cap = this.enraged ? SANCTIFIED_MAX_AMPLIFIER_ENRAGED : SANCTIFIED_MAX_AMPLIFIER;
            int next = current == null ? 0 : Math.min(cap, current.getAmplifier() + 1);
            victim.addEffect(new MobEffectInstance(ModEffects.SANCTIFIED, SANCTIFIED_DURATION, next));
        }
        return hit;
    }

    /** One landed blow with a damage multiplier and a shove, via the normal melee path (so Sanctified still applies). */
    private boolean hitWith(LivingEntity target, double damageMultiplier, double knockback) {
        AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.removeModifier(ATTACK_MULTIPLIER);
            if (damageMultiplier != 1.0) {
                damage.addTransientModifier(new AttributeModifier(ATTACK_MULTIPLIER, damageMultiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        boolean hit = this.doHurtTarget(target);
        if (damage != null) {
            damage.removeModifier(ATTACK_MULTIPLIER);
        }
        if (hit && knockback > 0.0) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double len = Math.max(Math.sqrt(dx * dx + dz * dz), 1.0E-4);
            target.knockback(knockback, -dx / len, -dz / len);
        }
        return hit;
    }

    /** The clip's impact frame: damage lands now, if the target is still in reach (so dodging works). */
    private void strike(Attack attack, LivingEntity primary) {
        float pitch = attack.heavy ? 0.6F : 0.9F;
        this.level().playSound(null, this.blockPosition(), attack.heavy ? SoundEvents.MACE_SMASH_GROUND : SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, attack.heavy ? 1.0F : 0.9F, pitch);
        List<LivingEntity> victims = new ArrayList<>();
        if (attack.radius > 0.0) {
            AABB area = this.getBoundingBox().inflate(attack.radius, 1.0, attack.radius);
            for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area, this::isEnemy)) {
                if (this.distanceTo(living) <= attack.radius + living.getBbWidth() * 0.5) {
                    victims.add(living);
                }
            }
        } else if (primary != null && this.isEnemy(primary) && this.canReach(primary, attack.reach + 0.6)) {
            victims.add(primary);
        }
        for (LivingEntity victim : victims) {
            this.hitWith(victim, attack.damage, attack.knockback);
        }
    }

    private boolean canReach(LivingEntity target, double reach) {
        return this.distanceTo(target) - target.getBbWidth() * 0.5 <= reach && Math.abs(target.getY() - this.getY()) < 2.8;
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

    /** He always mends, bell or no bell: briskly once the fight is over, a trickle during it (nothing in fury). */
    private void tickRegen() {
        if (this.getHealth() >= this.getMaxHealth()) {
            return;
        }
        if (this.getTarget() == null) {
            this.heal(IDLE_REGEN_PER_PERIOD);
        } else if (!this.enraged) {
            this.heal(COMBAT_REGEN_PER_PERIOD);
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
        if (this.heavyCooldown > 0) {
            this.heavyCooldown--;
        }
        if (this.spinCooldown > 0) {
            this.spinCooldown--;
        }
        if (!this.bellLooked) {
            this.findBell(serverLevel);
        }
        if (this.tickCount % BELL_PERIOD == 0 && this.isAlive()) {
            this.tickBell(serverLevel);
            this.tickRegen();
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
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.isAlive() && !this.attacking && this.tickCount - this.lastHurtAnimTick > HURT_ANIMATION_GAP) {
            this.lastHurtAnimTick = this.tickCount;
            this.triggerAnim("move", "hurt");
        }
        return hurt;
    }

    /** The blades and the long cape reach well past his hitbox: keep him drawn while any of that is on screen. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(3.5, 0.5, 3.5);
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
    // GeckoLib animation: one controller owns the whole pose. Locomotion (idle/walk/run) is read from real
    // movement; the attack goal and hurt() fire the one-shot clips through triggerAnim.
    // ------------------------------------------------------------------------------------------

    private String desiredAnimationState() {
        double dx = this.getX() - this.xOld;
        double dz = this.getZ() - this.zOld;
        double speedSqr = dx * dx + dz * dz;
        boolean wasMoving = "walk".equals(this.animationState) || "run".equals(this.animationState);
        if (speedSqr <= (wasMoving ? MOVE_OFF_SQR : MOVE_ON_SQR)) {
            return "idle";
        }
        return speedSqr > RUN_SPEED_SQR || (this.enraged && speedSqr > RUN_SPEED_SQR * 0.5) ? "run" : "walk";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<EliasEntity> controller = new AnimationController<>(this, "move", 6, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(DEATH_ANIMATION);
            }
            String desired = this.desiredAnimationState();
            if (!desired.equals(this.animationState)) {
                this.animationState = desired;
                this.loopAnimation = RawAnimation.begin().thenLoop(ANIM + desired);
            }
            return state.setAndContinue(this.loopAnimation);
        });
        for (Attack attack : Attack.values()) {
            controller.triggerableAnim(attack.clip, RawAnimation.begin().thenPlay(ANIM + attack.clip));
        }
        controller.triggerableAnim("hurt", RawAnimation.begin().thenPlay(ANIM + "hurt"));
        controllers.add(controller);
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
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 0.5F, 0.7F);
    }

    /**
     * Chases the target, then picks and plays an attack. The picked clip is triggered on the client, he stands his
     * ground while it plays, and the damage lands at the clip's impact frame (see {@link EliasEntity#strike}).
     * Heavies are rationed by cooldown; the same attack never plays twice in a row; fury quickens everything.
     */
    private static class EliasAttackGoal extends Goal {
        private final EliasEntity elias;
        @Nullable
        private Attack current;
        @Nullable
        private Attack last;
        private int ticks;
        private int cooldown;
        private int repath;

        EliasAttackGoal(EliasEntity elias) {
            this.elias = elias;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.elias.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.current != null || this.canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.elias.setAggressive(true);
            this.repath = 0;
            this.cooldown = Math.max(this.cooldown, 12);
        }

        @Override
        public void stop() {
            this.elias.setAggressive(false);
            this.elias.getNavigation().stop();
            this.elias.attacking = this.current != null;
        }

        @Override
        public void tick() {
            LivingEntity target = this.elias.getTarget();
            if (this.current == null) {
                if (target == null) {
                    return;
                }
                this.elias.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (this.cooldown > 0) {
                    this.cooldown--;
                }
                if (--this.repath <= 0) {
                    this.repath = 4 + this.elias.getRandom().nextInt(7);
                    this.elias.getNavigation().moveTo(target, 1.0);
                }
                if (this.cooldown <= 0) {
                    Attack pick = this.choose(target);
                    if (pick != null) {
                        this.begin(pick);
                    }
                }
                return;
            }
            // mid-swing: plant his feet, keep the blades pointed at whoever he is cutting at
            this.elias.getNavigation().stop();
            if (target != null) {
                this.elias.getLookControl().setLookAt(target, 40.0F, 40.0F);
            }
            this.ticks++;
            if (this.ticks == this.current.hitTick) {
                this.elias.strike(this.current, target);
            }
            if (this.ticks >= this.current.length) {
                double pace = this.elias.enraged ? 0.65 : 1.0;
                this.cooldown = (int) Math.round(this.current.recovery * pace);
                this.last = this.current;
                this.current = null;
                this.elias.attacking = false;
            }
        }

        private void begin(Attack attack) {
            this.current = attack;
            this.ticks = 0;
            this.elias.attacking = true;
            this.elias.getNavigation().stop();
            if (attack.heavy) {
                this.elias.heavyCooldown = this.elias.enraged ? HEAVY_COOLDOWN / 2 : HEAVY_COOLDOWN;
            }
            if (attack == Attack.HEAVY_SPIN) {
                this.elias.spinCooldown = this.elias.enraged ? SPIN_COOLDOWN / 2 : SPIN_COOLDOWN;
            }
            this.elias.triggerAnim("move", attack.clip);
        }

        /** Weighted pick among the attacks that can reach the target right now. */
        @Nullable
        private Attack choose(LivingEntity target) {
            List<Attack> pool = new ArrayList<>();
            List<Double> weights = new ArrayList<>();
            double total = 0.0;
            int crowd = this.elias.level().getEntitiesOfClass(LivingEntity.class,
                    this.elias.getBoundingBox().inflate(Attack.HEAVY_SPIN.radius, 1.0, Attack.HEAVY_SPIN.radius), this.elias::isEnemy).size();
            for (Attack attack : Attack.values()) {
                if (attack == this.last || !this.elias.canReach(target, attack.reach)) {
                    continue;
                }
                if (attack.heavy && this.elias.heavyCooldown > 0) {
                    continue;
                }
                if (attack == Attack.HEAVY_SPIN && (this.elias.spinCooldown > 0 || crowd < 2 && this.elias.getRandom().nextFloat() > 0.25F)) {
                    continue;
                }
                double w = attack.weight * (attack.heavy && this.elias.enraged ? 1.6 : 1.0) * (attack == Attack.HEAVY_SPIN && crowd >= 2 ? 2.0 : 1.0);
                pool.add(attack);
                weights.add(w);
                total += w;
            }
            if (pool.isEmpty()) {
                return null;
            }
            double roll = this.elias.getRandom().nextDouble() * total;
            for (int i = 0; i < pool.size(); i++) {
                roll -= weights.get(i);
                if (roll <= 0.0) {
                    return pool.get(i);
                }
            }
            return pool.get(pool.size() - 1);
        }
    }
}
