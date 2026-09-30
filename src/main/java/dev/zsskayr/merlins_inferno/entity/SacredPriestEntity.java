package dev.zsskayr.merlins_inferno.entity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import dev.zsskayr.merlins_inferno.blockentity.SacredAltarBlockEntity;
import dev.zsskayr.merlins_inferno.entity.ai.ChurchServiceGoal;

/**
 * The Sacred Priest - the Angelical path's Circle 1 miniboss and the guardian of the Sacred Church. Neutral
 * until struck (he ignores players who leave him be), then fights back and his cultists join him. Elias
 * cannot be woken at the altar until he has fallen. By day he wanders near the church; from dusk to dawn he takes his
 * place behind the altar, facing the congregation, and kneels in silent vigil with the broadsword planted before him
 * ({@link ChurchServiceGoal}) - until he is struck or warned, which ends it till the next dawn. Mid-fight he intones a
 * healing prayer every so often instead.
 * <ul>
 *     <li><b>Stats:</b> 150 health, 10 damage.</li>
 *     <li><b>Loot:</b> Lyrium Shards (data/.../loot_table/entities/sacred_priest.json).</li>
 * </ul>
 * Animated with GeckoLib ({@code geo/sacred_priest.geo.json}, {@code animations/sacred_priest.animation.json}); the
 * broadsword is baked into the model, not a held item.
 */
public class SacredPriestEntity extends Monster implements GeoEntity, ChurchServiceGoal.Attendee {
    public static final double MAX_HEALTH = 150.0;
    public static final double ATTACK_DAMAGE = 10.0;
    public static final double MOVEMENT_SPEED = 0.23;

    private static final int PRAYER_INTERVAL = 160;
    private static final float PRAYER_HEAL = 6.0F;
    private static final int HOME_RADIUS = 24;
    /** Cultists this close rally to him when he is struck (see {@link #hurt}). */
    private static final double CULTIST_ALERT_RADIUS = 16.0;

    /** The death clip is 2.5 s; the body lingers a little past it so the last pose is held before it vanishes. */
    private static final int DEATH_ANIMATION_TICKS = 55;

    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation.sacred_priest.attack");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("animation.sacred_priest.death");
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.sacred_priest.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.sacred_priest.walk");
    private static final RawAnimation RUN_ANIMATION = RawAnimation.begin().thenLoop("animation.sacred_priest.run");
    /** Kneels down (pray_start), then holds the prayer. */
    private static final RawAnimation PRAY_ANIMATION = RawAnimation.begin().thenPlay("animation.sacred_priest.pray_start").thenLoop("animation.sacred_priest.pray");
    /** Vanilla's limb-swing amount (about 4 x blocks moved per tick, capped at 1): a stroll is ~0.5, a chase ~1. */
    private static final float RUN_LIMB_SWING = 0.7F;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int deathTicks;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.merlins_inferno.sacred_priest"),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

    @Nullable
    private BlockPos altarPos;
    /** Where he stands during the night service - behind the altar, facing the nave - or null if he has no post. */
    @Nullable
    private Vec3 servicePost;
    private float serviceYaw;
    /** Struck or warned: he stays out of the service until dawn. */
    private boolean alarmed;

    public SacredPriestEntity(EntityType<? extends SacredPriestEntity> type, Level level) {
        super(type, level);
        this.xpReward = 30;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    /** Ties him to a church: he stays near it, and its altar learns of his death. */
    public void setHome(BlockPos altar) {
        this.altarPos = altar.immutable();
        this.restrictTo(this.altarPos, HOME_RADIUS);
    }

    /** Gives him his place at the altar: where his feet go and the direction he faces while praying. */
    public void setServicePost(Vec3 post, float yaw) {
        this.servicePost = post;
        this.serviceYaw = yaw;
    }

    @Override
    @Nullable
    public Vec3 servicePost() {
        return this.servicePost;
    }

    @Override
    public float serviceYaw() {
        return this.serviceYaw;
    }

    @Override
    public boolean isAlarmed() {
        return this.alarmed;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (target != null) {
            this.alarmed = true;
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.alarmed && this.getTarget() == null && !ChurchServiceGoal.isServiceTime(this.level())) {
            this.alarmed = false; // a new day: peace again, ready for the next night's service
        }
    }

    /** A kneeling priest is not shoved away from the altar. */
    @Override
    public boolean isPushable() {
        return this.getPose() != Pose.CROUCHING && super.isPushable();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(3, new ChurchServiceGoal<>(this, 0.7));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Retaliation only, like his flock.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, SacredCultistEntity.class));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.isAlive() && this.getTarget() != null && this.tickCount % PRAYER_INTERVAL == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(PRAYER_HEAL);
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.8F);
        }
    }

    /** Cultists nearby rally to him - "defend the clergy" - the moment he is struck. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            this.alarmed = true;
        }
        if (hurt && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker && this.level() instanceof ServerLevel serverLevel) {
            for (SacredCultistEntity cultist : serverLevel.getEntitiesOfClass(SacredCultistEntity.class,
                    new AABB(this.blockPosition()).inflate(CULTIST_ALERT_RADIUS), SacredCultistEntity::isAlive)) {
                cultist.setTarget(attacker);
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (this.level() instanceof ServerLevel serverLevel && this.altarPos != null
                && serverLevel.getBlockEntity(this.altarPos) instanceof SacredAltarBlockEntity altar) {
            altar.setPriestPending(false);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!this.level().isClientSide) {
            this.triggerAnim("move", "attack");
        }
        return super.doHurtTarget(target);
    }

    /** Lingers for the whole death clip instead of vanilla's 20 ticks, like the Sacred Cultist's. */
    @Override
    protected void tickDeath() {
        ++this.deathTicks;
        if (this.deathTicks >= DEATH_ANIMATION_TICKS && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    // ------------------------------------------------------------------------------------------
    // GeckoLib animation: one controller owns the whole pose - loops, the kneeling bracket and the
    // attack trigger. Praying is read straight from the vanilla pose ChurchServiceGoal already sets.
    // ------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The standard GeckoLib shape, decided fresh every frame: the kneeling pose ChurchServiceGoal sets shows the prayer,
        // otherwise GeckoLib's own movement test (vanilla's limb swing, which is reliable for remote entities) picks walk,
        // run or idle. No hand-rolled state to get stuck.
        controllers.add(new AnimationController<>(this, "move", 8, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(DEATH_ANIMATION);
            }
            if (this.getPose() == Pose.CROUCHING) {
                return state.setAndContinue(PRAY_ANIMATION);
            }
            if (state.isMoving()) {
                return state.setAndContinue(state.getLimbSwingAmount() > RUN_LIMB_SWING ? RUN_ANIMATION : WALK_ANIMATION);
            }
            return state.setAndContinue(IDLE_ANIMATION);
        }).triggerableAnim("attack", ATTACK_ANIMATION));
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
        if (this.altarPos != null) {
            compound.putLong("Altar", this.altarPos.asLong());
        }
        if (this.servicePost != null) {
            compound.putDouble("PostX", this.servicePost.x);
            compound.putDouble("PostY", this.servicePost.y);
            compound.putDouble("PostZ", this.servicePost.z);
            compound.putFloat("PostYaw", this.serviceYaw);
        }
        compound.putBoolean("Alarmed", this.alarmed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Altar")) {
            this.setHome(BlockPos.of(compound.getLong("Altar")));
        }
        if (compound.contains("PostX")) {
            this.servicePost = new Vec3(compound.getDouble("PostX"), compound.getDouble("PostY"), compound.getDouble("PostZ"));
            this.serviceYaw = compound.getFloat("PostYaw");
        }
        this.alarmed = compound.getBoolean("Alarmed");
    }

    // Vanilla placeholders until the mod has its own audio.
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }
}
