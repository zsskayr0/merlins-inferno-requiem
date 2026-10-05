package dev.zsskayr.merlins_inferno.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.phys.AABB;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;

/**
 * Andras, the Forgotten Demon - the Infernal path's Circle 1 boss: a lesser demon, the overseer of the Nether's
 * lower ranks. He keeps Imps at his beck and call and treats Grymns as tools.
 * <ul>
 *     <li><b>Stats:</b> 370 health, 15 damage; fire-immune; a demon (see the {@code demon} entity tag).</li>
 *     <li><b>Contracts:</b> while he fights, every {@link #SUMMON_INTERVAL} ticks he calls up to
 *     {@link #SUMMON_COUNT} Imps (never more than {@link #MAX_IMPS} of them around him).</li>
 *     <li><b>Mark of Debt:</b> every blow he lands brands a player ({@code ModEffects#MARK_OF_DEBT}): more damage
 *     taken, for {@link #MARK_DURATION} ticks.</li>
 *     <li><b>Loot:</b> the Book of Contracts, the key item of the Pandora Box ritual (data/.../loot_table/entities/andras.json).</li>
 * </ul>
 * Spawns rarely in the Nether, solitary. His fortress is future content. Animated with GeckoLib
 * (idle/walk, combat stance, slash, spell cast, flinch, death).
 */
public class AndrasEntity extends Monster implements GeoEntity {
    public static final double MAX_HEALTH = 370.0;
    public static final double ATTACK_DAMAGE = 15.0;
    public static final double MOVEMENT_SPEED = 0.26;

    private static final int SUMMON_INTERVAL = 400;
    private static final int SUMMON_COUNT = 2;
    private static final int MAX_IMPS = 6;
    private static final double IMP_COUNT_RADIUS = 32.0;
    private static final int MARK_DURATION = 600;
    /** Nothing else of his kind within this many blocks for a natural spawn. */
    private static final double SOLITARY_RADIUS = 256.0;
    /** Length of the death clip (2.4 s): he lingers for all of it instead of vanilla's 20 ticks. */
    private static final int DEATH_ANIMATION_TICKS = 48;
    private static final int HURT_ANIMATION_COOLDOWN = 20;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.andras.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.andras.walk");
    private static final RawAnimation IDLE_COMBAT_ANIMATION = RawAnimation.begin().thenLoop("animation.andras.idle_combat");
    private static final RawAnimation WALK_COMBAT_ANIMATION = RawAnimation.begin().thenLoop("animation.andras.walk_combat");
    private static final RawAnimation SLASH_ANIMATION = RawAnimation.begin().thenPlay("animation.andras.attack_slash");
    private static final RawAnimation CAST_ANIMATION = RawAnimation.begin().thenPlay("animation.andras.cast_spell");
    private static final RawAnimation HIT_ANIMATION = RawAnimation.begin().thenPlay("animation.andras.hit");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("animation.andras.death");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int lastHurtAnimationTick;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.merlins_inferno.andras"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    private int summonTimer;

    public AndrasEntity(EntityType<? extends AndrasEntity> type, Level level) {
        super(type, level);
        this.xpReward = 60;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    public static boolean checkAndrasSpawnRules(EntityType<AndrasEntity> type, ServerLevelAccessor level, MobSpawnType spawnType,
            BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (spawnType == MobSpawnType.SPAWNER || spawnType == MobSpawnType.SPAWN_EGG || spawnType == MobSpawnType.COMMAND) {
            return true;
        }
        if (!level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type)
                || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        return level.getEntitiesOfClass(AndrasEntity.class, new AABB(pos).inflate(SOLITARY_RADIUS)).isEmpty();
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
    public boolean doHurtTarget(Entity target) {
        if (!this.level().isClientSide) {
            this.triggerAnim("move", "slash");
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Player player) {
            player.addEffect(new MobEffectInstance(ModEffects.MARK_OF_DEBT, MARK_DURATION, 0));
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.isAlive() && this.getTarget() != null && ++this.summonTimer >= SUMMON_INTERVAL) {
            this.summonTimer = 0;
            this.summonImps(serverLevel);
        }
    }

    /** Calls a few Imps to his side, unless enough already circle him. */
    private void summonImps(ServerLevel level) {
        List<ImpEntity> nearby = level.getEntitiesOfClass(ImpEntity.class, this.getBoundingBox().inflate(IMP_COUNT_RADIUS), Entity::isAlive);
        int room = Math.min(SUMMON_COUNT, MAX_IMPS - nearby.size());
        for (int i = 0; i < room; i++) {
            ImpEntity imp = ModEntityTypes.IMP.get().create(level);
            if (imp == null) {
                continue;
            }
            imp.moveTo(this.getX() + (this.random.nextDouble() - 0.5) * 4.0, this.getY() + 1.0 + this.random.nextDouble(),
                    this.getZ() + (this.random.nextDouble() - 0.5) * 4.0, this.random.nextFloat() * 360.0F, 0.0F);
            imp.finalizeSpawn(level, level.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            imp.setTarget(this.getTarget());
            level.addFreshEntity(imp);
        }
        if (room > 0) {
            this.triggerAnim("move", "cast");
            this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 0.6F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.isAlive()
                && this.tickCount - this.lastHurtAnimationTick >= HURT_ANIMATION_COOLDOWN) {
            this.lastHurtAnimationTick = this.tickCount;
            this.triggerAnim("move", "hit");
        }
        return hurt;
    }

    /** Lingers for the whole death clip instead of vanilla's 20 ticks (and never tips over - see the renderer). */
    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= DEATH_ANIMATION_TICKS && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
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

    // ------------------------------------------------------------------------------------------
    // Animation: one controller owns the pose; slash, cast and flinch are triggered one-shots.
    // ------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 4, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(DEATH_ANIMATION);
            }
            boolean combat = this.isAggressive() || this.getTarget() != null;
            if (state.isMoving()) {
                return state.setAndContinue(combat ? WALK_COMBAT_ANIMATION : WALK_ANIMATION);
            }
            return state.setAndContinue(combat ? IDLE_COMBAT_ANIMATION : IDLE_ANIMATION);
        })
                .triggerableAnim("slash", SLASH_ANIMATION)
                .triggerableAnim("cast", CAST_ANIMATION)
                .triggerableAnim("hit", HIT_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    // Vanilla placeholders until the mod has its own audio.
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIGLIN_BRUTE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PIGLIN_BRUTE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIGLIN_BRUTE_DEATH;
    }
}
