package dev.zsskayr.merlins_inferno.entity;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import dev.zsskayr.merlins_inferno.entity.ai.ImpCircleGoal;
import dev.zsskayr.merlins_inferno.entity.ai.ImpCorneredStrikeGoal;
import dev.zsskayr.merlins_inferno.entity.ai.ImpDiveGoal;
import dev.zsskayr.merlins_inferno.entity.ai.ImpFlockGoal;
import dev.zsskayr.merlins_inferno.entity.ai.ImpFleeGoal;
import dev.zsskayr.merlins_inferno.entity.ai.ImpPerchGoal;
import dev.zsskayr.merlins_inferno.entity.ai.ImpStealGoal;
import dev.zsskayr.merlins_inferno.entity.ai.ImpTheft;

/**
 * The Imp - the mod's first demon (tag {@code merlins_inferno:demon}): small, weak, flying,
 * abundant across the Nether and annoying rather than dangerous. Personality rule: <b>it wants
 * the gold more than it wants to win the fight</b> - that single trait ties together the theft,
 * the cowardice and the pack behaviour below.
 * <ul>
 *     <li><b>Courage:</b> alone or in a pair it flees the player. It only takes the initiative once
 *     there are at least {@link #COURAGE_GROUP_SIZE} <i>available</i> imps (counting itself) within
 *     {@link #GROUP_RADIUS} blocks with line of sight to each other (see {@link #updateCourage()}).
 *     An imp that is fleeing at low health, or busy carrying loot, doesn't count.</li>
 *     <li><b>Pack attack:</b> spaced-out dives (never more than 2 imps approaching at once), each
 *     ending in a low curve - the opening for the player - and a retreat ({@link ImpDiveGoal}).</li>
 *     <li><b>Theft:</b> "hand snatcher" - takes one unit of gold nugget/ingot/raw gold from the
 *     ground or a player's hand, or a golden tool/weapon from the hand of any other mob (piglins
 *     get furious and chase it). Telegraphed, damage-less lunge; see {@link ImpStealGoal}.</li>
 *     <li><b>Cornered:</b> pressed close by a threat it failed to flee from, it lands one defensive
 *     hit and dashes past ({@link ImpCorneredStrikeGoal}) - even at low health, but that never
 *     puts it back on the offensive.</li>
 * </ul>
 * Equipment slots double as the visible inventory: the main hand is the birth sword (5% of
 * spawns, never changes), the off hand is the stolen loot. Both always drop on death
 * (drop chance 2.0 = "always, undamaged").
 */
public class ImpEntity extends Monster implements GeoEntity {
    /** Available imps needed (self included) before the pack takes the initiative. */
    public static final int COURAGE_GROUP_SIZE = 3;
    public static final double GROUP_RADIUS = 12.0;
    public static final float FLEE_HEALTH = 3.0F;
    /** Ticks a stolen item is guaranteed not to despawn with its carrier (60s). */
    public static final int LOOT_PROTECTION_TICKS = 1200;
    private static final float ARMED_SPAWN_CHANCE = 0.05F;
    /** Total damage when armed (the birth sword replaces, rather than adds to, the base damage). */
    private static final float ARMED_DAMAGE = 3.0F;
    private static final int HESITATION_TICKS = 20;

    /** Visual/animation state, synced to clients: arms stretched for the snatch, or charging in a dive. */
    public static final int STATE_IDLE = 0;
    public static final int STATE_GRABBING = 1;
    public static final int STATE_DIVING = 2;
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(ImpEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.imp.idle");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation.imp.attack");
    private static final RawAnimation AIR_ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation.imp.air_attack");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    // Client visual state. Debounce changes once per entity tick, not once per rendered frame.
    private String movementAnimationState;
    private String pendingMovementAnimationState;
    private int pendingMovementAnimationTick;
    private RawAnimation movementAnimation = IDLE_ANIMATION;

    // --- server-side behaviour state ---
    private boolean courageous;
    private int hesitateTicks;
    private int groupCheckDelay;
    private boolean diving;
    private long lastDiveStartTick = -100L;
    private int diveCooldown;
    private int stealCooldown;
    private int retreatTicks;
    private int panicTicks;
    private int strikeCooldown;
    private int breakoutTicks;
    private int fleeStuckTicks;
    private int lootProtectionTicks;
    private int soundCooldown;
    private int jitterDelay;
    @Nullable
    private LivingEntity fleeThreat;

    public ImpEntity(EntityType<? extends ImpEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 3;
        this.setPathfindingMalus(PathType.LAVA, -1.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
        this.setNoGravity(true);
        // 2.0F = "always drops, and undamaged" (see Mob#dropCustomDeathLoot) - both the birth sword
        // and any stolen loot must come back exactly as they were.
        this.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 2.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** Natural spawns must be within this many blocks of a solid surface (wall, floor or ceiling). */
    private static final int SURFACE_SPAWN_RADIUS = 3;

    /**
     * Open air near a surface (so imps spread along walls and ledges instead of clouding the middle
     * of big caverns and flooding the monster cap), and not above a lava lake (recovering loot
     * from one would be a pure punishment).
     */
    public static boolean checkImpSpawnRules(EntityType<ImpEntity> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (spawnType == MobSpawnType.SPAWNER) {
            return true;
        }
        return level.getBlockState(pos).isAir() && isNearSurface(level, pos) && !isOverLava(level, pos);
    }

    /**
     * Within {@link #SURFACE_SPAWN_RADIUS} blocks of a solid surface, checked along the six axes
     * only (18 lookups) - the full 7x7x7 cube it used to scan (343) ran on every natural spawn
     * attempt in the Nether. A surface that's only diagonally near is missed, which merely makes
     * the spawn a touch pickier.
     */
    private static boolean isNearSurface(LevelAccessor level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            for (int step = 1; step <= SURFACE_SPAWN_RADIUS; step++) {
                cursor.setWithOffset(pos, direction.getStepX() * step, direction.getStepY() * step, direction.getStepZ() * step);
                if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isOverLava(LevelAccessor level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < 24 && cursor.getY() > level.getMinBuildHeight(); i++) {
            cursor.move(0, -1, 0);
            BlockState state = level.getBlockState(cursor);
            if (state.getFluidState().is(FluidTags.LAVA)) {
                return true;
            }
            if (!state.isAir()) {
                return false;
            }
        }
        return false;
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, STATE_IDLE);
    }

    @Override
    protected void registerGoals() {
        // Lower number = higher priority. See each goal's javadoc for its rules.
        this.goalSelector.addGoal(0, new ImpCorneredStrikeGoal(this));
        this.goalSelector.addGoal(1, new ImpFleeGoal(this, ImpFleeGoal.Mode.URGENT));
        this.goalSelector.addGoal(2, new ImpPerchGoal(this));
        this.goalSelector.addGoal(3, new ImpStealGoal(this));
        this.goalSelector.addGoal(4, new ImpDiveGoal(this));
        this.goalSelector.addGoal(5, new ImpFleeGoal(this, ImpFleeGoal.Mode.SHY));
        this.goalSelector.addGoal(6, new ImpCircleGoal(this));
        this.goalSelector.addGoal(7, new ImpFlockGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (this.random.nextFloat() < ARMED_SPAWN_CHANCE) {
            // Half-worn golden sword, no enchantments.
            ItemStack sword = new ItemStack(Items.GOLDEN_SWORD);
            int max = sword.getMaxDamage();
            sword.setDamageValue(max / 4 + this.random.nextInt(Math.max(1, max / 2)));
            this.setItemSlot(EquipmentSlot.MAINHAND, sword);
        }
        return result;
    }

    // ------------------------------------------------------------------------------------------
    // Movement helpers (the goals steer with velocity directly - simple, and predictable in caves)
    // ------------------------------------------------------------------------------------------

    /** Eases the current velocity toward flying at {@code speed} blocks/tick to {@code dest}, and faces it. */
    public void flyToward(Vec3 dest, double speed) {
        Vec3 delta = dest.subtract(this.position());
        double distance = delta.length();
        if (distance < 1.0E-3) {
            return;
        }
        Vec3 desired = delta.scale(Math.min(speed, distance) / distance);
        this.setDeltaMovement(this.getDeltaMovement().lerp(desired, 0.25));
        this.faceHorizontally(delta);
    }

    /** Sets velocity outright (dive/lunge/breakout impulses). */
    public void dash(Vec3 direction, double speed, double blend) {
        Vec3 dir = direction.normalize().scale(speed);
        this.setDeltaMovement(this.getDeltaMovement().scale(1.0 - blend).add(dir.scale(blend)));
        this.faceHorizontally(direction);
    }

    private void faceHorizontally(Vec3 direction) {
        if (direction.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
            this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 30.0F));
            this.yHeadRot = this.getYRot();
        }
    }

    public boolean isClearPath(Vec3 from, Vec3 to) {
        return this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    /** A point roughly {@code distance} away from {@code threat}, reachable in a straight line - or null if boxed in. */
    @Nullable
    public Vec3 pickEscapePoint(Vec3 threatPos, double distance) {
        Vec3 me = this.position();
        Vec3 away = me.subtract(threatPos).multiply(1.0, 0.0, 1.0);
        if (this.breakoutTicks > 0) {
            away = away.scale(-1.0); // just struck: run for the far side of the threat
        }
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(this.random.nextDouble() - 0.5, 0.0, this.random.nextDouble() - 0.5);
        }
        away = away.normalize();
        Vec3 eye = this.getEyePosition();
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 10; i++) {
            Vec3 dir = away.yRot((this.random.nextFloat() - 0.5F) * 2.2F);
            double length = distance * (0.6 + this.random.nextDouble() * 0.6);
            Vec3 candidate = me.add(dir.x * length, (this.random.nextDouble() - 0.35) * 5.0, dir.z * length);
            if (!this.isClearPath(eye, candidate.add(0.0, this.getEyeHeight() - this.getBbHeight() / 2.0, 0.0))) {
                continue;
            }
            double score = candidate.distanceToSqr(threatPos) + this.random.nextDouble() * 6.0;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /** Random hop-and-pause flight when calm: a small impulse every so often, "little bursts". */
    private void jitter() {
        if (--this.jitterDelay > 0 || this.diving || this.getState() != STATE_IDLE) {
            return;
        }
        this.jitterDelay = 8 + this.random.nextInt(14);
        this.setDeltaMovement(this.getDeltaMovement().add(
                (this.random.nextDouble() - 0.5) * 0.12,
                (this.random.nextDouble() - 0.45) * 0.1,
                (this.random.nextDouble() - 0.5) * 0.12));
    }

    // ------------------------------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.stealCooldown = Math.max(0, this.stealCooldown - 1);
        this.retreatTicks = Math.max(0, this.retreatTicks - 1);
        this.panicTicks = Math.max(0, this.panicTicks - 1);
        this.diveCooldown = Math.max(0, this.diveCooldown - 1);
        this.strikeCooldown = Math.max(0, this.strikeCooldown - 1);
        this.breakoutTicks = Math.max(0, this.breakoutTicks - 1);
        this.hesitateTicks = Math.max(0, this.hesitateTicks - 1);
        this.soundCooldown = Math.max(0, this.soundCooldown - 1);
        if (this.lootProtectionTicks > 0) {
            this.lootProtectionTicks = this.isCarrying() ? this.lootProtectionTicks - 1 : 0;
        }
        if (--this.groupCheckDelay <= 0) {
            this.groupCheckDelay = 10;
            this.updateCourage();
        }
        if (this.fleeThreat != null && !this.fleeThreat.isAlive()) {
            this.fleeThreat = null;
        }
        this.jitter();
    }

    /**
     * Counts the imps able to fight within {@link #GROUP_RADIUS} that this one can see (itself
     * included, if available). Enough of them means courage; losing it starts a short hesitation
     * before the flee behaviour kicks in, and any dive already launched still finishes.
     */
    private void updateCourage() {
        boolean was = this.courageous;
        int count = 0;
        if (this.isAvailableFighter()) {
            count = 1; // itself
            double radiusSqr = GROUP_RADIUS * GROUP_RADIUS;
            // Cheap filter in the query; the line-of-sight raycast runs per candidate and stops as
            // soon as the pack is big enough, instead of testing every neighbour every time.
            List<ImpEntity> nearby = this.level().getEntitiesOfClass(ImpEntity.class, this.getBoundingBox().inflate(GROUP_RADIUS),
                    other -> other != this && other.isAvailableFighter());
            for (ImpEntity other : nearby) {
                if (count >= COURAGE_GROUP_SIZE) {
                    break;
                }
                if (this.distanceToSqr(other) <= radiusSqr && this.hasLineOfSight(other)) {
                    count++;
                }
            }
        }
        this.courageous = count >= COURAGE_GROUP_SIZE;
        if (was && !this.courageous) {
            this.hesitateTicks = HESITATION_TICKS;
        }
    }

    // ------------------------------------------------------------------------------------------
    // State queries used by the goals
    // ------------------------------------------------------------------------------------------

    public boolean isCourageous() {
        return this.courageous;
    }

    public boolean isLowHealth() {
        return this.getHealth() <= FLEE_HEALTH;
    }

    /** Carrying stolen loot (off hand). The birth sword lives in the main hand and doesn't count. */
    public boolean isCarrying() {
        return !this.getOffhandItem().isEmpty();
    }

    /** Counts toward the pack's courage: healthy and not busy with loot. */
    public boolean isAvailableFighter() {
        return this.isAlive() && !this.isLowHealth() && !this.isCarrying() && this.panicTicks <= 0;
    }

    public boolean isArmed() {
        return !this.getMainHandItem().isEmpty();
    }

    /** Pressed close by the threat it has been unable to flee from - see {@code ImpFleeGoal}'s stuck counter. */
    public boolean isCornered() {
        return this.fleeThreat != null && this.fleeStuckTicks >= 15 && this.distanceTo(this.fleeThreat) < 3.5F;
    }

    public int getState() {
        return this.entityData.get(DATA_STATE);
    }

    public void setState(int state) {
        this.entityData.set(DATA_STATE, state);
    }

    public boolean isDiving() {
        return this.diving;
    }

    public void setDiving(boolean diving) {
        this.diving = diving;
    }

    public long getLastDiveStartTick() {
        return this.lastDiveStartTick;
    }

    public void markDiveStarted() {
        this.lastDiveStartTick = this.level().getGameTime();
    }

    public int getDiveCooldown() {
        return this.diveCooldown;
    }

    public void setDiveCooldown(int ticks) {
        this.diveCooldown = ticks;
    }

    public int getStealCooldown() {
        return this.stealCooldown;
    }

    public void setStealCooldown(int ticks) {
        this.stealCooldown = ticks;
    }

    public int getRetreatTicks() {
        return this.retreatTicks;
    }

    public void setRetreatTicks(int ticks) {
        this.retreatTicks = ticks;
    }

    public int getPanicTicks() {
        return this.panicTicks;
    }

    public int getHesitateTicks() {
        return this.hesitateTicks;
    }

    public int getStrikeCooldown() {
        return this.strikeCooldown;
    }

    public void setStrikeCooldown(int ticks) {
        this.strikeCooldown = ticks;
    }

    public void startBreakout() {
        this.breakoutTicks = 25;
    }

    public int getFleeStuckTicks() {
        return this.fleeStuckTicks;
    }

    public void setFleeStuckTicks(int ticks) {
        this.fleeStuckTicks = ticks;
    }

    @Nullable
    public LivingEntity getFleeThreat() {
        return this.fleeThreat;
    }

    public void setFleeThreat(@Nullable LivingEntity threat) {
        this.fleeThreat = threat;
    }

    /** What the dive goal needs to know about the pack, from a single scan. */
    public record DiveScan(int divers, long latestStart) {
    }

    /**
     * How many other imps are in the approach phase of a dive (prepare/charge) within {@code radius},
     * and the game tick of the most recent dive start among the other imps there (or a very old
     * tick) - one entity query for both (they used to be two, run every couple of ticks).
     */
    public DiveScan scanOtherDivers(double radius) {
        int divers = 0;
        long latest = Long.MIN_VALUE / 2;
        for (ImpEntity other : this.level().getEntitiesOfClass(ImpEntity.class, this.getBoundingBox().inflate(radius), other -> other != this)) {
            if (other.diving) {
                divers++;
            }
            latest = Math.max(latest, other.lastDiveStartTick);
        }
        return new DiveScan(divers, latest);
    }



    /** The widest range any goal asks {@link #findThreat} about (ImpFleeGoal's URGENT_RANGE). */
    private static final double THREAT_SCAN_RANGE = 16.0;
    /** How long a threat scan is trusted; a hit on the imp invalidates it at once (see {@link #hurt}). */
    private static final int THREAT_CACHE_TICKS = 5;
    @Nullable
    private LivingEntity cachedThreat;
    private int threatScanTick = -100;

    /**
     * The nearest thing this imp has reason to run from: a player, a mob that has it as an attack
     * target (an angry piglin), or whoever last hurt it - within {@code range} (at most
     * {@link #THREAT_SCAN_RANGE}).
     * <p>
     * The scan (a player lookup plus an entity query with brain lookups) used to run on every call,
     * and several goals call this every tick or two per imp. It now runs once every
     * {@link #THREAT_CACHE_TICKS} ticks for the widest range; because it keeps only the NEAREST
     * threat, answering a smaller range is exact: if the nearest one is out of range, none is in.
     */
    @Nullable
    public LivingEntity findThreat(double range) {
        if (this.tickCount - this.threatScanTick >= THREAT_CACHE_TICKS
                || (this.cachedThreat != null && !this.cachedThreat.isAlive())) {
            this.cachedThreat = this.scanThreat(THREAT_SCAN_RANGE);
            this.threatScanTick = this.tickCount;
        }
        LivingEntity threat = this.cachedThreat;
        return threat != null && this.distanceToSqr(threat) <= range * range ? threat : null;
    }

    @Nullable
    private LivingEntity scanThreat(double range) {
        LivingEntity best = null;
        double bestDistance = range * range;
        Player player = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), range, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        if (player != null) {
            best = player;
            bestDistance = this.distanceToSqr(player);
        }
        for (Mob hunter : this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(range), mob -> mob != this && ImpTheft.isHunting(mob, this))) {
            double distance = this.distanceToSqr(hunter);
            if (distance < bestDistance) {
                best = hunter;
                bestDistance = distance;
            }
        }
        LivingEntity attacker = this.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && this.tickCount - this.getLastHurtByMobTimestamp() < 200
                && !(attacker instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            double distance = this.distanceToSqr(attacker);
            if (distance < bestDistance) {
                best = attacker;
            }
        }
        return best;
    }

    public boolean wasHurtRecently() {
        return this.tickCount - this.getLastHurtByMobTimestamp() < 30 && this.getLastHurtByMob() != null;
    }

    // ------------------------------------------------------------------------------------------
    // Loot
    // ------------------------------------------------------------------------------------------

    /** Picks up {@code stack} as carried loot; the item stays exactly as stolen (data included). */
    public void setCarried(ItemStack stack) {
        this.setItemSlot(EquipmentSlot.OFFHAND, stack);
        this.lootProtectionTicks = LOOT_PROTECTION_TICKS;
    }

    /** Drops the carried loot (marked so imps ignore it for a while). Returns the entity, if anything was dropped. */
    @Nullable
    public ItemEntity dropCarried() {
        ItemStack carried = this.getOffhandItem();
        if (carried.isEmpty()) {
            return null;
        }
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        this.lootProtectionTicks = 0;
        return this.spawnAtLocation(carried);
    }

    /**
     * Anything an imp drops - stolen loot, the birth sword and generated gold alike - is briefly
     * off-limits to every imp: this is what stops steal/drop cycles and generated gold being
     * recycled into the very next theft.
     */
    @Nullable
    @Override
    public ItemEntity spawnAtLocation(ItemStack stack, float yOffset) {
        ItemEntity item = super.spawnAtLocation(stack, yOffset);
        if (item != null) {
            ImpTheft.ignore(item, this.level().getGameTime() + ImpTheft.DROP_IGNORE_TICKS);
        }
        return item;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            this.threatScanTick = -100; // rescan next call: the attacker may be the new nearest threat
        }
        if (hurt && !this.level().isClientSide && this.isAlive()) {
            // Any blow from another entity makes it let go of stolen loot - and it doesn't try again.
            if (this.isCarrying() && (source.getEntity() != null || source.getDirectEntity() != null)) {
                ItemEntity dropped = this.dropCarried();
                if (dropped != null) {
                    dropped.setDeltaMovement(dropped.getDeltaMovement().add(0.0, 0.15, 0.0));
                    this.stealCooldown = 1200;
                    this.panicTicks = 100;
                    this.playSound(SoundEvents.PIGLIN_RETREAT, 1.0F, 1.6F);
                }
            }
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.playAttackAnimation();
        float damage = this.isArmed() ? ARMED_DAMAGE : (float) this.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        DamageSource source = this.damageSources().mobAttack(this);
        boolean hit = target.hurt(source, damage);
        if (hit) {
            this.setLastHurtMob(target);
            if (this.level() instanceof ServerLevel serverLevel) {
                net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(serverLevel, target, source);
            }
        }
        this.swing(InteractionHand.MAIN_HAND);
        return hit;
    }

    // ------------------------------------------------------------------------------------------
    // Despawning, persistence
    // ------------------------------------------------------------------------------------------

    @Override
    public void checkDespawn() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            // Never swallow someone's stolen gold just because the difficulty changed.
            if (!this.level().isClientSide) {
                this.dropCarried();
            }
        } else if (this.lootProtectionTicks > 0) {
            this.noActionTime = 0; // within the 60s recovery window: never despawn
            return;
        }
        super.checkDespawn();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("LootProtection", this.lootProtectionTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.lootProtectionTicks = compound.getInt("LootProtection");
    }

    // ------------------------------------------------------------------------------------------
    // One controller owns the whole pose, including attacks, so two controllers cannot fight
    // over the wings/body. GeckoLib blends arbitrary phases into authored transition clips.
    // ------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 4, state -> {
            boolean wasMoving = "walk".equals(this.movementAnimationState) || "fly".equals(this.movementAnimationState);
            // Hysteresis keeps hovering jitter from repeatedly restarting flight transitions.
            double threshold = wasMoving ? 0.0016 : 0.004;
            boolean airborne = !this.onGround();
            double speedSquared = airborne ? this.getDeltaMovement().lengthSqr()
                    : this.getDeltaMovement().horizontalDistanceSqr();
            double dx = this.getX() - this.xOld;
            double dy = airborne ? this.getY() - this.yOld : 0;
            double dz = this.getZ() - this.zOld;
            boolean moving = Math.max(speedSquared, dx * dx + dy * dy + dz * dz) > threshold;
            String desired = airborne ? (moving ? "fly" : "hover") : (moving ? "walk" : "idle");
            RawAnimation previous = state.getController().getCurrentRawAnimation();
            boolean recoveringFromAttack = ATTACK_ANIMATION.equals(previous) || AIR_ATTACK_ANIMATION.equals(previous);

            if (this.movementAnimationState == null || recoveringFromAttack) {
                // A new observer starts at the current pose; an attack returns to the actual
                // movement state without replaying an old takeoff/landing sequence.
                this.movementAnimationState = desired;
                this.pendingMovementAnimationState = desired;
                this.pendingMovementAnimationTick = this.tickCount;
                this.movementAnimation = RawAnimation.begin().thenLoop("animation.imp." + desired);
            } else if (!desired.equals(this.movementAnimationState)) {
                if (!desired.equals(this.pendingMovementAnimationState)) {
                    this.pendingMovementAnimationState = desired;
                    this.pendingMovementAnimationTick = this.tickCount;
                } else if (this.tickCount - this.pendingMovementAnimationTick >= 3) {
                    this.movementAnimation = RawAnimation.begin()
                            .thenPlay("animation.imp." + this.movementAnimationState + "_to_" + desired)
                            .thenLoop("animation.imp." + desired);
                    this.movementAnimationState = desired;
                }
            } else {
                this.pendingMovementAnimationState = desired;
                this.pendingMovementAnimationTick = this.tickCount;
            }
            return state.setAndContinue(this.movementAnimation);
        }).triggerableAnim("attack", ATTACK_ANIMATION)
                .triggerableAnim("air_attack", AIR_ATTACK_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    /** Server-side: plays the one-shot attack animation on every tracking client. */
    public void playAttackAnimation() {
        if (!this.level().isClientSide) {
            this.triggerAnim("move", this.onGround() ? "attack" : "air_attack");
        }
    }

    // ------------------------------------------------------------------------------------------
    // Flying-mob boilerplate
    // ------------------------------------------------------------------------------------------

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    // ------------------------------------------------------------------------------------------
    // Sounds - vanilla placeholders until the mod has its own audio assets (keep the roles, swap the events).
    // ------------------------------------------------------------------------------------------

    /** Plays one of the imp's "voice lines", at most one every 2 seconds so a flock doesn't become noise. */
    public void speak(SoundEvent sound, float pitch) {
        if (this.soundCooldown <= 0) {
            this.soundCooldown = 40;
            this.playSound(sound, 1.0F, pitch + (this.random.nextFloat() - 0.5F) * 0.2F);
        }
    }

    /** Murmur of greed - the telegraph of a theft attempt. */
    public void greedSound() {
        this.speak(SoundEvents.PIGLIN_ADMIRING_ITEM, 1.5F);
    }

    /** Scream before a dive. */
    public void screamSound() {
        this.speak(SoundEvents.VEX_CHARGE, 1.3F);
    }

    /** Squeal of fear. */
    public void fearSound() {
        this.speak(SoundEvents.PIGLIN_RETREAT, 1.6F);
    }

    /** Cackle - reserved for a successful theft. */
    public void laughSound() {
        this.playSound(SoundEvents.WITCH_CELEBRATE, 1.0F, 1.7F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }
}
