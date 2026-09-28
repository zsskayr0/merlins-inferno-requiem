package dev.zsskayr.merlins_inferno.entity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.ai.StarvedChargeGoal;
import dev.zsskayr.merlins_inferno.entity.ai.StarvedSeekGoal;
import dev.zsskayr.merlins_inferno.registry.ModSounds;

/**
 * The Starved - the Nether's miniboss between the Imp and the true boss (tag {@code merlins_inferno:demon}).
 * The Imp's counterpart: big, four-legged, grounded, relentless, and hungry for flesh rather than gold.
 * <ul>
 *     <li><b>Never flees</b>, has a long follow range and, once it loses sight of its target, walks to the
 *     last place it saw it ({@link StarvedSeekGoal}).</li>
 *     <li><b>Growing hunger</b> (the central mechanic): while it has a target and hasn't landed a hit it
 *     gains a hunger stack every {@link #HUNGER_INTERVAL} ticks, up to {@link #MAX_HUNGER}. Each stack
 *     raises speed and damage. A successful hit "feeds" it: hunger resets and it heals
 *     {@link #FEED_HEAL_FRACTION} of its max health.</li>
 *     <li><b>Charge</b> at {@link #CHARGE_HUNGER} stacks or more: a telegraphed, dodgeable dash
 *     ({@link StarvedChargeGoal}).</li>
 * </ul>
 */
public class StarvedEntity extends Monster implements GeoEntity {
    public static final int MAX_HUNGER = 10;
    /** Hunger stacks needed before it starts using its charge. */
    public static final int CHARGE_HUNGER = 4;
    /** Ticks without landing a hit (while hunting) per hunger stack. */
    public static final int HUNGER_INTERVAL = 80;
    public static final float FEED_HEAL_FRACTION = 0.08F;
    /** Share of hits that are bites; a bite hits 30% harder and heals 50% more. */
    private static final float BITE_CHANCE = 0.4F;
    private static final double BITE_DAMAGE_BONUS = 0.3;
    private static final float BITE_FEED_MULTIPLIER = 1.5F;
    private static final ResourceLocation BITE_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "starved_bite_damage");
    private static final double SPEED_PER_HUNGER = 0.05;
    private static final double DAMAGE_PER_HUNGER = 0.08;
    /** Natural spawns only below this height (the Nether's deep half) and in the dark. */
    private static final int MAX_SPAWN_Y = 70;
    private static final int MAX_SPAWN_BLOCK_LIGHT = 9;
    private static final double SOLITARY_RADIUS = 64.0;

    private static final ResourceLocation HUNGER_SPEED_ID = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "starved_hunger_speed");
    private static final ResourceLocation HUNGER_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "starved_hunger_damage");

    /** Animation/pose state, synced to clients. */
    public static final int STATE_IDLE = 0;
    public static final int STATE_WINDUP = 1;
    public static final int STATE_CHARGING = 2;
    private static final EntityDataAccessor<Integer> DATA_HUNGER = SynchedEntityData.defineId(StarvedEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(StarvedEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.starved.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.starved.walk");
    private static final RawAnimation RUN_ANIMATION = RawAnimation.begin().thenLoop("animation.starved.run");
    // Windup and charge reuse "hungry" and "run" - the model has no dedicated clips for them.
    private static final RawAnimation WINDUP_ANIMATION = RawAnimation.begin().thenLoop("animation.starved.hungry");
    private static final RawAnimation CHARGE_ANIMATION = RawAnimation.begin().thenLoop("animation.starved.run");
    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation.starved.attack");
    private static final RawAnimation BITE_ANIMATION = RawAnimation.begin().thenPlay("animation.starved.bite");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    private int hungerTicks;
    private int soundCooldown;

    public StarvedEntity(EntityType<? extends StarvedEntity> type, Level level) {
        super(type, level);
        this.xpReward = 50;
        this.setPathfindingMalus(PathType.LAVA, -1.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, 8.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, 8.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.ATTACK_KNOCKBACK, 0.8);
    }

    /**
     * Solitary, rare (see its biome modifier), and only in the deep, dark parts of the Nether: below
     * {@link #MAX_SPAWN_Y}, low block light, and no other Starved within {@link #SOLITARY_RADIUS}.
     */
    public static boolean checkStarvedSpawnRules(EntityType<StarvedEntity> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
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
        return level.getEntitiesOfClass(StarvedEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(SOLITARY_RADIUS)).isEmpty();
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
        builder.define(DATA_HUNGER, 0);
        builder.define(DATA_STATE, STATE_IDLE);
    }

    @Override
    protected void registerGoals() {
        // Lower number = higher priority. There is deliberately no flee goal.
        this.goalSelector.addGoal(1, new StarvedChargeGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(3, new StarvedSeekGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // mustSee = false: it keeps tracking through walls inside its follow range.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, null));
        // The Nether's own game is prey to it too: hoglins and piglins (brutes included).
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class, 10, false, false,
                prey -> prey instanceof net.minecraft.world.entity.monster.hoglin.Hoglin
                        || prey instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin));
    }

    // ------------------------------------------------------------------------------------------
    // Hunger
    // ------------------------------------------------------------------------------------------

    public int getHunger() {
        return this.entityData.get(DATA_HUNGER);
    }

    public void setHunger(int hunger) {
        int clamped = Mth.clamp(hunger, 0, MAX_HUNGER);
        if (clamped == this.getHunger()) {
            return;
        }
        this.entityData.set(DATA_HUNGER, clamped);
        this.applyHungerModifiers(clamped);
    }

    private void applyHungerModifiers(int hunger) {
        this.setModifier(Attributes.MOVEMENT_SPEED, HUNGER_SPEED_ID, hunger * SPEED_PER_HUNGER);
        this.setModifier(Attributes.ATTACK_DAMAGE, HUNGER_DAMAGE_ID, hunger * DAMAGE_PER_HUNGER);
    }

    private void setModifier(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount > 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        this.soundCooldown = Math.max(0, this.soundCooldown - 1);
        if (this.getTarget() != null && this.getTarget().isAlive()) {
            if (++this.hungerTicks >= HUNGER_INTERVAL) {
                this.hungerTicks = 0;
                if (this.getHunger() < MAX_HUNGER) {
                    this.setHunger(this.getHunger() + 1);
                    this.playSound(ModSounds.STARVED_HUNGER.get(), 0.7F, 0.85F + this.getHunger() * 0.03F);
                }
            }
        } else {
            // Nothing to hunt: the hunger slowly subsides so it never idles at full frenzy.
            if (++this.hungerTicks >= HUNGER_INTERVAL * 2) {
                this.hungerTicks = 0;
                this.setHunger(this.getHunger() - 1);
            }
        }
    }

    /** It feeds on what it hits: hunger resets and it recovers a slice of its health. */
    private void feed(float healMultiplier) {
        this.hungerTicks = 0;
        this.setHunger(0);
        this.heal(this.getMaxHealth() * FEED_HEAL_FRACTION * healMultiplier);
        this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.5F);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // Two strikes: the claw swipe (attack) and the heavier bite, which feeds it more.
        boolean bite = !this.level().isClientSide && this.random.nextFloat() < BITE_CHANCE;
        if (!this.level().isClientSide) {
            this.triggerAnim("move", bite ? "bite" : "attack");
            this.playSound(ModSounds.STARVED_ATTACK.get(), 0.8F, (bite ? 0.7F : 0.9F) + this.random.nextFloat() * 0.2F);
        }
        AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeModifier biteBonus = new AttributeModifier(BITE_DAMAGE_ID, BITE_DAMAGE_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (bite && damage != null) {
            damage.addTransientModifier(biteBonus);
        }
        boolean hit;
        try {
            hit = super.doHurtTarget(target);
        } finally {
            if (bite && damage != null) {
                damage.removeModifier(BITE_DAMAGE_ID);
            }
        }
        if (hit && !this.level().isClientSide) {
            this.feed(bite ? BITE_FEED_MULTIPLIER : 1.0F);
        }
        return hit;
    }

    // ------------------------------------------------------------------------------------------
    // Charge state (driven by StarvedChargeGoal)
    // ------------------------------------------------------------------------------------------

    public int getState() {
        return this.entityData.get(DATA_STATE);
    }

    public void setState(int state) {
        this.entityData.set(DATA_STATE, state);
    }

    /** Announces itself with a roar the moment it picks up a target - the "it has noticed you" cue. */
    @Override
    public void setTarget(@Nullable net.minecraft.world.entity.LivingEntity target) {
        boolean noticed = target != null && this.getTarget() == null && !this.level().isClientSide;
        super.setTarget(target);
        if (noticed) {
            this.roar(ModSounds.STARVED_NOTICE.get(), 1.0F);
        }
    }

    /** Charge telegraph: still the vanilla Ravager roar until a dedicated windup sound exists. */
    public void roar() {
        this.roar(SoundEvents.RAVAGER_ROAR, 0.6F);
    }

    private void roar(SoundEvent sound, float pitch) {
        if (this.soundCooldown <= 0) {
            this.soundCooldown = 40;
            this.playSound(sound, 1.2F, pitch);
        }
    }

    @Nullable
    public Vec3 lastKnownTargetPos;

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Hunger", this.getHunger());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setHunger(compound.getInt("Hunger"));
    }

    // ------------------------------------------------------------------------------------------
    // Animation: one controller owns the pose; attacks are triggered one-shots.
    // ------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 4, state -> {
            RawAnimation animation;
            if (this.getState() == STATE_WINDUP) {
                animation = WINDUP_ANIMATION;
            } else if (this.getState() == STATE_CHARGING) {
                animation = CHARGE_ANIMATION;
            } else if (this.getDeltaMovement().horizontalDistanceSqr() > 2.5E-3) {
                animation = this.getHunger() >= CHARGE_HUNGER / 2 ? RUN_ANIMATION : WALK_ANIMATION;
            } else {
                animation = IDLE_ANIMATION;
            }
            return state.setAndContinue(animation);
        }).triggerableAnim("attack", ATTACK_ANIMATION)
                .triggerableAnim("bite", BITE_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    // ------------------------------------------------------------------------------------------
    // Sounds - vanilla placeholders.
    // ------------------------------------------------------------------------------------------

    /** Ambient breathing is for when it's calm: in a fight the notice/hunger/step/attack sounds carry it. */
    @Override
    public void playAmbientSound() {
        if (this.getTarget() == null && this.tickCount - this.getLastHurtByMobTimestamp() > 100) {
            super.playAmbientSound();
        }
    }

    /** A long ambient clip must not talk over the hurt cry: cut it for nearby players. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.network.protocol.game.ClientboundStopSoundPacket stop = new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
                    ModSounds.STARVED_AMBIENT.getId(), this.getSoundSource());
            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(this) < 64.0 * 64.0) {
                    player.connection.send(stop);
                }
            }
        }
        return hurt;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.STARVED_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(ModSounds.STARVED_STEP.get(), 0.5F, 0.9F + this.random.nextFloat() * 0.2F);
    }
}
