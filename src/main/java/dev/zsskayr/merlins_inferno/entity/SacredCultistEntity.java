package dev.zsskayr.merlins_inferno.entity;

import java.util.EnumSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PathfinderMob;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * A cultist of the Sacred Church, and (rarely) of the open world too. In Circle 1 it is neutral - it ignores
 * players until struck, then fights back and calls the other cultists within earshot to do the same
 * ({@link HurtByTargetGoal} with alert-others). It also joins in when a nearby Sacred Priest is struck (see
 * {@code SacredPriestEntity#hurt}). From Circle 2 on - the rumor that the player unleashed the Pandora Box's
 * evils - it is hostile to that player on sight ({@link ProgressionHelper}). Never despawns; the church spawns
 * eight of them once, and a few more wander the world rarely (biome modifier).
 * <p>
 * Animated with GeckoLib ({@code geo/cultist.geo.json}, {@code animations/cultist.animation.json}): idle, walk,
 * run, a triggered attack, the kneeling prayer bracketed by pray_start/pray_end ({@link PrayGoal}'s
 * {@link Pose#CROUCHING}), and a death clip that holds its last frame.
 */
public class SacredCultistEntity extends PathfinderMob implements GeoEntity {
    private static final double BROTHERHOOD_RADIUS = 32.0;
    private static final float IRON_SWORD_CHANCE = 0.30F;
    private static final float SERAPHIUM_SWORD_CHANCE = 0.08F;

    /** Horizontal speed (blocks per tick, squared) above which it counts as moving / running. */
    private static final double WALK_SPEED_SQR = 0.002;
    private static final double WALK_HYSTERESIS_SQR = 0.0007;
    private static final double RUN_SPEED_SQR = 0.035;
    /** The death clip is 2.4 s; the body lingers a little past it so the last pose is held before it vanishes. */
    private static final int DEATH_ANIMATION_TICKS = 50;

    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation.cultist.attack");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("animation.cultist.death");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    // Client-side visual state, debounced once per entity tick, same technique as the Dullahan's.
    private String animationState;
    private String pendingAnimationState;
    private int pendingAnimationTick;
    private RawAnimation loopAnimation = RawAnimation.begin().thenLoop("animation.cultist.idle");

    public SacredCultistEntity(EntityType<? extends SacredCultistEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(3, new PrayGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Circle 2+: hostile to the player on sight (the Pandora Box rumor). Circle 1: retaliation only,
        // and the whole congregation joins in.
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, true,
                living -> living instanceof Player player && ProgressionHelper.hasReached(player, ProgressionHelper.SECOND_CIRCLE)));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, SacredCultistEntity.class).setAlertOthers(SacredCultistEntity.class));
        // The faithful hunt the unholy: undead and demons are attacked on sight, whatever the circle.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false,
                living -> living.getType().is(EntityTypeTags.UNDEAD) || living.getType().is(ModTags.EntityTypes.DEMON)));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
            SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        // Most go bare-handed; some carry an iron sword, and a rare few a Seraphium one. Never dropped, so
        // cultists can't be farmed for Seraphium swords.
        float roll = this.random.nextFloat();
        if (roll < SERAPHIUM_SWORD_CHANCE) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SERAPHIUM_SWORD.get()));
        } else if (roll < SERAPHIUM_SWORD_CHANCE + IRON_SWORD_CHANCE) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return data;
    }

    /** Brotherhood, like zombified piglins: strike one and every cultist and priest within earshot turns on the attacker. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            this.rallyBrethren(source);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        this.rallyBrethren(cause);
    }

    private void rallyBrethren(DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker instanceof SacredCultistEntity
                || attacker instanceof SacredPriestEntity || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (attacker instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        AABB area = this.getBoundingBox().inflate(BROTHERHOOD_RADIUS);
        for (Mob brother : level.getEntitiesOfClass(Mob.class, area,
                m -> m != this && m.isAlive() && (m instanceof SacredCultistEntity || m instanceof SacredPriestEntity))) {
            brother.setTarget(attacker);
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

    /** Lingers for the whole death clip instead of vanilla's 20 ticks, like the Dullahan's. */
    private int deathTicks;

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
    // attack trigger - so nothing fights over the same bones. Praying is read straight from the
    // vanilla pose PrayGoal already sets; it needs no extra synced state.
    // ------------------------------------------------------------------------------------------

    /** idle / walk / run on foot, or "pray" while {@link PrayGoal} has it kneeling. */
    private String desiredAnimationState() {
        if (this.getPose() == Pose.CROUCHING) {
            return "pray";
        }
        // Position change per tick works on the client too (remote entities carry no reliable velocity).
        double dx = this.getX() - this.xOld;
        double dz = this.getZ() - this.zOld;
        double speedSqr = dx * dx + dz * dz;
        boolean wasMoving = "walk".equals(this.animationState) || "run".equals(this.animationState);
        boolean moving = speedSqr > (wasMoving ? WALK_HYSTERESIS_SQR : WALK_SPEED_SQR);
        if (!moving) {
            return this.idleVariant();
        }
        return speedSqr > RUN_SPEED_SQR ? "run" : "walk";
    }

    private static final int MIN_IDLE_VARIANT_TICKS = 100;
    private static final int MAX_IDLE_VARIANT_TICKS = 300;
    private boolean idleAlt;
    private int idleVariantSwitchTick = -1;

    /** Standing idle alternates between the plain loop and "idle_hands_on_hips" every 5-15 seconds. */
    private String idleVariant() {
        if (this.idleVariantSwitchTick < 0) {
            this.idleVariantSwitchTick = this.tickCount + MIN_IDLE_VARIANT_TICKS + this.random.nextInt(MAX_IDLE_VARIANT_TICKS - MIN_IDLE_VARIANT_TICKS + 1);
        } else if (this.tickCount >= this.idleVariantSwitchTick) {
            this.idleAlt = !this.idleAlt;
            this.idleVariantSwitchTick = this.tickCount + MIN_IDLE_VARIANT_TICKS + this.random.nextInt(MAX_IDLE_VARIANT_TICKS - MIN_IDLE_VARIANT_TICKS + 1);
        }
        return this.idleAlt ? "idle_hands_on_hips" : "idle";
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
                this.pendingAnimationState = desired;
                this.pendingAnimationTick = this.tickCount;
                this.loopAnimation = RawAnimation.begin().thenLoop("animation.cultist." + desired);
            } else if (!desired.equals(this.animationState)) {
                if (!desired.equals(this.pendingAnimationState)) {
                    this.pendingAnimationState = desired;
                    this.pendingAnimationTick = this.tickCount;
                } else if (this.tickCount - this.pendingAnimationTick >= 3) {
                    // The model only ships bracket clips for entering/leaving the kneeling prayer.
                    RawAnimation next = RawAnimation.begin();
                    if ("pray".equals(desired)) {
                        next = next.thenPlay("animation.cultist.pray_start");
                    } else if ("pray".equals(this.animationState)) {
                        next = next.thenPlay("animation.cultist.pray_end");
                    }
                    this.loopAnimation = next.thenLoop("animation.cultist." + desired);
                    this.animationState = desired;
                }
            } else {
                this.pendingAnimationState = desired;
                this.pendingAnimationTick = this.tickCount;
            }
            return state.setAndContinue(this.loopAnimation);
        }).triggerableAnim("attack", ATTACK_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.random.nextInt(3) == 0 ? SoundEvents.VILLAGER_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    /** Kneels (crouching pose) for stretches while it has nothing else to do, then gets up and mills about. */
    private static class PrayGoal extends Goal {
        private final SacredCultistEntity mob;
        private int ticksLeft;

        PrayGoal(SacredCultistEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return this.mob.getTarget() == null && this.mob.getRandom().nextInt(120) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticksLeft > 0 && this.mob.getTarget() == null;
        }

        @Override
        public void start() {
            this.ticksLeft = 200 + this.mob.getRandom().nextInt(300);
            this.mob.getNavigation().stop();
            this.mob.setPose(Pose.CROUCHING);
        }

        @Override
        public void tick() {
            this.ticksLeft--;
        }

        @Override
        public void stop() {
            this.mob.setPose(Pose.STANDING);
        }
    }
}
