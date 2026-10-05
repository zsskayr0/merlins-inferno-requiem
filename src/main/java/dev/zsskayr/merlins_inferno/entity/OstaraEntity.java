package dev.zsskayr.merlins_inferno.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Ostara, the Spring Deity - the Mundane (Druidic) path's Circle 1 boss.
 * <ul>
 *     <li><b>Stats:</b> 300 health, 12 damage.</li>
 *     <li><b>Petals:</b> from a distance she flings a volley of petals: magic damage and a bout of Slowness
 *     and Poison ({@link #PETAL_INTERVAL} ticks apart, up to {@link #PETAL_RANGE} blocks, needs line of sight).</li>
 *     <li><b>Bloom:</b> every {@link #BLOOM_INTERVAL} ticks she heals the monsters around her (and herself, less).</li>
 *     <li><b>Spawn:</b> never naturally - {@code OstaraSpawnHandler} calls her to a Rowanwood tree at dawn every third day, and
 *     she fades away at dusk ({@link #isDawnBorn}); one at a time.</li>
 *     <li><b>Loot:</b> Eve's Secret, the key item of the Pandora Box ritual (data/.../loot_table/entities/ostara.json).</li>
 * </ul>
 * Animated with GeckoLib (idle, giggle, attack); her model is made of meshes, drawn by {@code OstaraRenderer}.
 */
public class OstaraEntity extends Monster implements GeoEntity {
    public static final double MAX_HEALTH = 300.0;
    public static final double ATTACK_DAMAGE = 12.0;
    public static final double MOVEMENT_SPEED = 0.25;

    private static final int PETAL_INTERVAL = 80;
    private static final double PETAL_RANGE = 16.0;
    private static final float PETAL_DAMAGE = 4.0F;
    private static final int BLOOM_INTERVAL = 200;
    private static final double BLOOM_RADIUS = 12.0;
    private static final float BLOOM_ALLY_HEAL = 20.0F;
    private static final float BLOOM_SELF_HEAL = 8.0F;
    /** Time of day (ticks) from which she fades away. */
    private static final long DUSK = 12000L;
    private static final long NOT_DAWN_BORN = -1L;

    /** Ticks between two attack clips: the melee goal swings every second, the clip lasts two. */
    private static final int ATTACK_CLIP_COOLDOWN = 40;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.ostara.idle");
    private static final RawAnimation GIGGLE_ANIMATION = RawAnimation.begin().thenLoop("animation.ostara.idle_risadinha");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation.ostara.attack");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int lastAttackAnimationTick = Integer.MIN_VALUE;

    /** The world day she was called on by the Rowanwood dawn ritual, or {@link #NOT_DAWN_BORN} (egg, command). */
    private long dawnDay = NOT_DAWN_BORN;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.merlins_inferno.ostara"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);

    public OstaraEntity(EntityType<? extends OstaraEntity> type, Level level) {
        super(type, level);
        this.xpReward = 60;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** No natural spawns: only the dawn ritual (and eggs, commands, spawners) place her. */
    public static boolean checkOstaraSpawnRules(EntityType<OstaraEntity> type, ServerLevelAccessor level, MobSpawnType spawnType,
            BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        return spawnType == MobSpawnType.SPAWNER || spawnType == MobSpawnType.SPAWN_EGG || spawnType == MobSpawnType.COMMAND;
    }

    /** Marks her as called by the dawn ritual of the given world day: she leaves at dusk. */
    public void setDawnDay(long day) {
        this.dawnDay = day;
    }

    public boolean isDawnBorn() {
        return this.dawnDay != NOT_DAWN_BORN;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("DawnDay", this.dawnDay);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.dawnDay = tag.contains("DawnDay") ? tag.getLong("DawnDay") : NOT_DAWN_BORN;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (!this.isAlive()) {
            return;
        }
        if (this.isDawnBorn()) {
            long time = serverLevel.getDayTime();
            if (time % 24000L >= DUSK || time / 24000L != this.dawnDay) {
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 1.0, this.getZ(), 40, 0.6, 1.0, 0.6, 0.05);
                this.discard();
                return;
            }
        }
        LivingEntity target = this.getTarget();
        if (target != null && this.tickCount % PETAL_INTERVAL == 0 && this.distanceToSqr(target) <= PETAL_RANGE * PETAL_RANGE
                && this.hasLineOfSight(target)) {
            this.flingPetals(serverLevel, target);
        }
        if (target != null && this.tickCount % BLOOM_INTERVAL == 0) {
            this.bloom(serverLevel);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.announceAttack();
        return super.doHurtTarget(target);
    }

    private void announceAttack() {
        if (!this.level().isClientSide && this.tickCount - this.lastAttackAnimationTick >= ATTACK_CLIP_COOLDOWN) {
            this.lastAttackAnimationTick = this.tickCount;
            this.triggerAnim("move", "attack");
        }
    }

    // ------------------------------------------------------------------------------------------
    // Animation: one controller owns the pose; the attack clip is a triggered one-shot.
    // ------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 4,
                state -> state.setAndContinue(this.isAggressive() ? IDLE_ANIMATION : GIGGLE_ANIMATION))
                .triggerableAnim("attack", ATTACK_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    private void flingPetals(ServerLevel level, LivingEntity target) {
        this.announceAttack();
        target.hurt(this.damageSources().indirectMagic(this, this), PETAL_DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
        this.playSound(SoundEvents.CHERRY_LEAVES_PLACE, 1.0F, 1.2F);
    }

    /** A pulse of spring: mends every monster nearby (never the player's allies), and herself a little. */
    private void bloom(ServerLevel level) {
        for (Monster ally : level.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(BLOOM_RADIUS), m -> m != this && m.isAlive())) {
            ally.heal(BLOOM_ALLY_HEAL);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, ally.getX(), ally.getY() + 1.0, ally.getZ(), 6, 0.3, 0.5, 0.3, 0.0);
        }
        this.heal(BLOOM_SELF_HEAL);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 1.0, this.getZ(), 30, 1.5, 1.0, 1.5, 0.05);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ------------------------------------------------------------------------------------------
    // Boss bar
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

    // Vanilla placeholders until the mod has its own audio.
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITCH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }
}
