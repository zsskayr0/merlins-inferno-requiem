package dev.zsskayr.merlins_inferno.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import dev.zsskayr.merlins_inferno.entity.ai.GrymnLurkGoal;

/**
 * The Grymn - the Nether's Circle 1 miniboss (tag {@code merlins_inferno:demon}): a phantom-like ghost that
 * took over the Starved's old role. Same stats the Starved had as a miniboss, but none of its hunger/charge
 * mechanics - it is a lurker instead: it flies, always hugging the ground ({@link #HOVER_MIN}-{@link #HOVER_MAX}
 * blocks up), drifting in wait and going straight for the throat once it notices someone.
 * <p>
 * It only ever attacks players ({@link #canAttack}); it never flees. Built on the same
 * {@link FlyingMoveControl}/{@link FlyingPathNavigation} plumbing as the Imp; no fall damage, fire-immune.
 */
public class GrymnEntity extends Monster implements GeoEntity {
    /** Clearance band above the floor it tries to stay in while idle - low enough to feel like it is stalking. */
    public static final double HOVER_MIN = 0.4;
    public static final double HOVER_MAX = 1.4;
    private static final double GROUND_SCAN_DEPTH = 10.0;
    /** Natural spawns only below this height (the Nether's deep half) and in the dark. */
    private static final int MAX_SPAWN_Y = 70;
    private static final int MAX_SPAWN_BLOCK_LIGHT = 9;
    private static final double SOLITARY_RADIUS = 64.0;

    /** Synced to clients: it has a target, so its eyes glow red instead of white. */
    private static final EntityDataAccessor<Boolean> DATA_HUNTING = SynchedEntityData.defineId(GrymnEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.grymn.idle");
    private static final RawAnimation FLY_ANIMATION = RawAnimation.begin().thenLoop("animation.grymn.fly");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation.grymn.attack");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public GrymnEntity(EntityType<? extends GrymnEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 50;
        this.setPathfindingMalus(PathType.LAVA, -1.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, 8.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, 8.0F);
        this.setNoGravity(true);
    }

    /** The Starved's old miniboss stats; flying speed matches its ground speed. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FLYING_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.ATTACK_KNOCKBACK, 0.8);
    }

    /**
     * Solitary, rare (see its biome modifier), and only in the deep, dark parts of the Nether: below
     * {@link #MAX_SPAWN_Y}, low block light, and no other Grymn within {@link #SOLITARY_RADIUS}.
     */
    public static boolean checkGrymnSpawnRules(EntityType<GrymnEntity> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
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
        if (pos.getY() > MAX_SPAWN_Y || level.getBrightness(LightLayer.BLOCK, pos) > MAX_SPAWN_BLOCK_LIGHT) {
            return false;
        }
        return level.getEntitiesOfClass(GrymnEntity.class, new AABB(pos).inflate(SOLITARY_RADIUS)).isEmpty();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.hasCustomName() && distanceToClosestPlayer > 128.0;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HUNTING, false);
    }

    public boolean isHunting() {
        return this.entityData.get(DATA_HUNTING);
    }

    /** Client-only eye animation: 0 = white, 1 = red, eased toward the hunting state a step per tick. */
    private static final float EYE_STEP = 1.0F / 12.0F;
    private float eyeRed;
    private float eyeRedOld;

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.eyeRedOld = this.eyeRed;
            this.eyeRed = Mth.clamp(this.eyeRed + (this.isHunting() ? EYE_STEP : -EYE_STEP), 0.0F, 1.0F);
        }
    }

    public float getEyeRed(float partialTick) {
        return Mth.lerp(partialTick, this.eyeRedOld, this.eyeRed);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        // Lower number = higher priority. There is deliberately no flee goal.
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new GrymnLurkGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Players only - no HurtByTargetGoal, so retaliation never makes it turn on another mob.
        // mustSee = false: it keeps tracking through walls inside its follow range.
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, null));
    }

    /** It only ever attacks the player - whatever tries to set another target (mods, retaliation) is refused. */
    @Override
    public boolean canAttack(LivingEntity target) {
        return target instanceof Player && super.canAttack(target);
    }

    // ------------------------------------------------------------------------------------------
    // Low flight
    // ------------------------------------------------------------------------------------------

    /**
     * Y of the first solid surface below {@code (x, y, z)} within {@link #GROUND_SCAN_DEPTH} blocks (starting a few
     * blocks above, so a point inside a shallow overhang still finds the floor), or {@code Double.NaN} if none.
     */
    public double groundYBelow(double x, double y, double z) {
        Vec3 from = new Vec3(x, y + 3.0, z);
        Vec3 to = new Vec3(x, y - GROUND_SCAN_DEPTH, z);
        var hit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS ? Double.NaN : hit.getLocation().y;
    }

    /** While it has nothing to hunt it settles into the {@link #HOVER_MIN}-{@link #HOVER_MAX} band above the floor. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        boolean hunting = this.getTarget() != null;
        if (hunting != this.isHunting()) {
            this.entityData.set(DATA_HUNTING, hunting);
        }
        if (hunting || this.tickCount % 2 != 0) {
            return;
        }
        double ground = this.groundYBelow(this.getX(), this.getY(), this.getZ());
        double clearance = Double.isNaN(ground) ? Double.MAX_VALUE : this.getY() - ground;
        if (clearance > HOVER_MAX) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        } else if (clearance < HOVER_MIN) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04, 0.0));
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!this.level().isClientSide) {
            this.triggerAnim("move", "attack");
            this.playSound(SoundEvents.PHANTOM_BITE, 0.8F, 0.8F + this.random.nextFloat() * 0.2F);
        }
        return super.doHurtTarget(target);
    }

    // ------------------------------------------------------------------------------------------
    // Animation: one controller owns the pose; the attack is a triggered one-shot.
    // ------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 4,
                state -> state.setAndContinue(this.getDeltaMovement().lengthSqr() > 2.5E-3 ? FLY_ANIMATION : IDLE_ANIMATION))
                .triggerableAnim("attack", ATTACK_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    // ------------------------------------------------------------------------------------------
    // Sounds - vanilla placeholders until the mod has its own audio.
    // ------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }
}
