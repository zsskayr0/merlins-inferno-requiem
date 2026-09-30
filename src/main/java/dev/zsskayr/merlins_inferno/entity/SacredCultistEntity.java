package dev.zsskayr.merlins_inferno.entity;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
import dev.zsskayr.merlins_inferno.entity.ai.ChurchServiceGoal;
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
 * run, a triggered attack, the kneeling prayer bracketed by pray_start/pray_end ({@link ChurchServiceGoal}'s
 * {@link Pose#CROUCHING}), and a death clip that holds its last frame.
 * <p>
 * Church life: the church gives each of its cultists a pew seat ({@link #setServicePost}). By day they wander loose around
 * the church; from dusk to dawn they file into their seats, face the altar and pray - and only then. Being hurt, provoked
 * or warned (a target) ends the prayer until the next dawn.
 */
public class SacredCultistEntity extends PathfinderMob implements GeoEntity, ChurchServiceGoal.Attendee {
    private static final double BROTHERHOOD_RADIUS = 32.0;
    private static final float IRON_SWORD_CHANCE = 0.30F;
    private static final float SERAPHIUM_SWORD_CHANCE = 0.08F;

    /** The death clip is 2.4 s; the body lingers a little past it so the last pose is held before it vanishes. */
    private static final int DEATH_ANIMATION_TICKS = 50;

    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation.cultist.attack");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("animation.cultist.death");
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.cultist.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.cultist.walk");
    private static final RawAnimation RUN_ANIMATION = RawAnimation.begin().thenLoop("animation.cultist.run");
    /** Kneels down (pray_start), then holds the prayer. */
    private static final RawAnimation PRAY_ANIMATION = RawAnimation.begin().thenPlay("animation.cultist.pray_start").thenLoop("animation.cultist.pray");
    /** Vanilla's limb-swing amount (about 4 x blocks moved per tick, capped at 1) above which the legs are visibly working. */
    private static final float MOVING_LIMB_SWING = 0.08F;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    /** Where it prays during the night service (its pew), or null for a cultist of the open world. */
    @Nullable
    private Vec3 servicePost;
    private float serviceYaw;
    /** Hurt or warned: it stays out of the service until dawn. */
    private boolean alarmed;

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
        this.goalSelector.addGoal(3, new ChurchServiceGoal<>(this, 0.7));
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

    /** Gives it a seat: where its feet go and the direction it faces while praying. */
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

    /** A target - it was provoked, or its brethren called it - also breaks the prayer. */
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

    /** Kneeling worshippers are not shoved out of their pews. */
    @Override
    public boolean isPushable() {
        return this.getPose() != Pose.CROUCHING && super.isPushable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.servicePost != null) {
            tag.putDouble("PostX", this.servicePost.x);
            tag.putDouble("PostY", this.servicePost.y);
            tag.putDouble("PostZ", this.servicePost.z);
            tag.putFloat("PostYaw", this.serviceYaw);
        }
        tag.putBoolean("Alarmed", this.alarmed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("PostX")) {
            this.servicePost = new Vec3(tag.getDouble("PostX"), tag.getDouble("PostY"), tag.getDouble("PostZ"));
            this.serviceYaw = tag.getFloat("PostYaw");
        }
        this.alarmed = tag.getBoolean("Alarmed");
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
            this.alarmed = true;
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The standard GeckoLib shape, decided fresh every frame: the kneeling pose ChurchServiceGoal sets shows the prayer,
        // otherwise vanilla's limb swing - worked out from how far the entity really moved each tick, so it is reliable for
        // remote entities where GeckoLib's isMoving (delta movement) is not - picks walk or idle, and run while it is
        // chasing (the aggressive flag is synced). No hand-rolled state to get stuck.
        controllers.add(new AnimationController<>(this, "move", 8, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(DEATH_ANIMATION);
            }
            if (this.getPose() == Pose.CROUCHING) {
                return state.setAndContinue(PRAY_ANIMATION);
            }
            if (state.getLimbSwingAmount() > MOVING_LIMB_SWING) {
                return state.setAndContinue(this.isAggressive() ? RUN_ANIMATION : WALK_ANIMATION);
            }
            return state.setAndContinue(IDLE_ANIMATION);
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
}
