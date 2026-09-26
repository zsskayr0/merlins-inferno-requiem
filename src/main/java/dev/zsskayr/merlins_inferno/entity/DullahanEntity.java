package dev.zsskayr.merlins_inferno.entity;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import dev.zsskayr.merlins_inferno.registry.ModTags;
import dev.zsskayr.merlins_inferno.worldgen.biome.ModBiomes;

/**
 * The Dullahan - Hallowed Grove's night miniboss (design doc, section 5): a tall headless knight
 * carrying its own head under one arm and lashing out with a whip made from a spine. On foot, no
 * mount.
 * <ul>
 *     <li><b>Stats:</b> half a Warden's health and damage (see {@link #createAttributes()}) -
 *     tune in playtesting.</li>
 *     <li><b>Presence:</b> spawns only at night, only inside the Hallowed Grove
 *     ({@link #checkDullahanSpawnRules}). Like the Warden it isn't a light-based spawn: once it's
 *     out it stays until dawn or until there is no player left in the biome near it, then fades
 *     away ({@link #shouldStayActive}). Only naturally-spawned ones are bound to that rule - one
 *     from a spawn egg, a spawner or a command stays put (see {@link #bound}).</li>
 *     <li><b>Gold:</b> in the legend it fears gold. An item of {@link ModTags.Items#DULLAHAN_REPELLENT}
 *     (gold ingots) lying near it is consumed and freezes it for {@link #PARALYSIS_TICKS} while it
 *     takes {@link #PARALYZED_DAMAGE_MULTIPLIER}x damage - a tactical window, never required. A
 *     cooldown stops it being chain-locked.</li>
 *     <li><b>Loot:</b> 2-3 Fae Essence (data/merlins_inferno/loot_table/entities/dullahan.json).</li>
 * </ul>
 */
public class DullahanEntity extends Monster {
    /** Half a Warden's 500 health / 30 melee damage - the doc's baseline, adjustable in playtesting. */
    public static final double MAX_HEALTH = 250.0;
    public static final double ATTACK_DAMAGE = 15.0;

    public static final int PARALYSIS_TICKS = 120;
    private static final int REPELLENT_COOLDOWN_TICKS = 240;
    private static final double REPELLENT_RANGE = 6.0;
    public static final float PARALYZED_DAMAGE_MULTIPLIER = 1.5F;

    /** The whip reaches past a normal melee swing by this much on each horizontal side. */
    private static final double WHIP_REACH_BONUS = 1.5;

    /** A bound Dullahan survives this long (ticks) with no reason to stay before it fades. */
    private static final int FADE_GRACE_TICKS = 100;
    private static final double PLAYER_PRESENCE_RANGE = 96.0;
    private static final double SPAWN_EXCLUSION_RADIUS = 128.0;
    private static final int NIGHT_START = 13000;
    private static final int NIGHT_END = 23000;

    private static final EntityDataAccessor<Boolean> DATA_PARALYZED = SynchedEntityData.defineId(DullahanEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.merlins_inferno.dullahan"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    /** Whether the night/biome rule governs this one (natural spawns only). Persisted. */
    private boolean bound;
    private int paralysisTicks;
    private int repellentCooldown;
    private int inactiveTicks;

    public DullahanEntity(EntityType<? extends DullahanEntity> type, Level level) {
        super(type, level);
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    // ------------------------------------------------------------------------------------------
    // Spawning
    // ------------------------------------------------------------------------------------------

    /** Night, inside the Hallowed Grove, and no other Dullahan around - "one per stretch of forest". */
    public static boolean checkDullahanSpawnRules(EntityType<DullahanEntity> type, ServerLevelAccessor level, MobSpawnType spawnType,
            BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.SPAWNER) {
            return true;
        }
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }
        if (!isNight(level.dayTime()) || !level.getBiome(pos).is(ModBiomes.HALLOWED_GROVE)) {
            return false;
        }
        return level.getEntitiesOfClass(DullahanEntity.class, new AABB(pos).inflate(SPAWN_EXCLUSION_RADIUS)).isEmpty();
    }

    /** Vanilla's own night window (bed-usable time). Not {@code Level#isNight}: that also flips on in thunderstorms. */
    private static boolean isNight(long dayTime) {
        long time = dayTime % 24000L;
        return time >= NIGHT_START && time < NIGHT_END;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        this.bound = spawnType == MobSpawnType.NATURAL;
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // presence is governed by tick() (dawn / leaving the biome), not by distance
    }

    /** Dawn, or no player left in the Grove close enough to still be part of this encounter. */
    private boolean shouldStayActive() {
        if (!isNight(this.level().getDayTime())) {
            return false;
        }
        double rangeSqr = PLAYER_PRESENCE_RANGE * PLAYER_PRESENCE_RANGE;
        for (Player player : this.level().players()) {
            if (!player.isSpectator() && this.distanceToSqr(player) <= rangeSqr
                    && this.level().getBiome(player.blockPosition()).is(ModBiomes.HALLOWED_GROVE)) {
                return true;
            }
        }
        return false;
    }

    private void fadeAway() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + this.getBbHeight() / 2.0, this.getZ(),
                    40, this.getBbWidth() / 2.0, this.getBbHeight() / 3.0, this.getBbWidth() / 2.0, 0.02);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SKELETON_DEATH, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
        this.discard();
    }

    // ------------------------------------------------------------------------------------------
    // AI
    // ------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The whip's extra reach: {@code MeleeAttackGoal} asks the mob itself whether the target is in range. */
    @Override
    protected AABB getAttackBoundingBox() {
        return super.getAttackBoundingBox().inflate(WHIP_REACH_BONUS, 0.0, WHIP_REACH_BONUS);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.level().playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 0.6F);
        return super.doHurtTarget(target);
    }

    // ------------------------------------------------------------------------------------------
    // Tick: paralysis, repellent, fading, boss bar
    // ------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (this.paralysisTicks > 0) {
            if (--this.paralysisTicks == 0) {
                this.entityData.set(DATA_PARALYZED, false);
            }
            this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
            this.getNavigation().stop();
        } else {
            this.repellentCooldown = Math.max(0, this.repellentCooldown - 1);
            if (this.repellentCooldown == 0 && this.tickCount % 5 == 0) {
                this.checkForRepellent();
            }
        }

        if (this.bound && this.tickCount % 20 == 0) {
            if (this.shouldStayActive()) {
                this.inactiveTicks = 0;
            } else {
                this.inactiveTicks += 20;
                if (this.inactiveTicks >= FADE_GRACE_TICKS) {
                    this.fadeAway();
                }
            }
        }
    }

    /** Gold on the ground nearby: eat one, freeze. */
    private void checkForRepellent() {
        List<ItemEntity> nearby = this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(REPELLENT_RANGE),
                item -> item.getItem().is(ModTags.Items.DULLAHAN_REPELLENT));
        if (nearby.isEmpty()) {
            return;
        }
        ItemEntity item = nearby.get(0);
        ItemStack stack = item.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setItem(stack);
        }
        this.paralysisTicks = PARALYSIS_TICKS;
        this.repellentCooldown = REPELLENT_COOLDOWN_TICKS;
        this.entityData.set(DATA_PARALYZED, true);
        this.setTarget(null);
        this.getNavigation().stop();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.WAX_ON, this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
                    20, 0.4, 0.6, 0.4, 0.05);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SKELETON_HURT, SoundSource.HOSTILE, 1.2F, 0.5F);
        }
    }

    public boolean isParalyzed() {
        return this.entityData.get(DATA_PARALYZED);
    }

    /** Frozen in place: {@code LivingEntity#aiStep} skips goals and movement input while this is true. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.paralysisTicks > 0;
    }

    /** The window the gold opens: it takes extra damage while paralyzed. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isParalyzed() && !this.level().isClientSide) {
            amount *= PARALYZED_DAMAGE_MULTIPLIER;
        }
        return super.hurt(source, amount);
    }

    // ------------------------------------------------------------------------------------------
    // Boss bar (only players who can see it), data, save
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PARALYZED, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Bound", this.bound);
        compound.putInt("Paralysis", this.paralysisTicks);
        compound.putInt("RepellentCooldown", this.repellentCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.bound = compound.getBoolean("Bound");
        this.paralysisTicks = compound.getInt("Paralysis");
        this.repellentCooldown = compound.getInt("RepellentCooldown");
        this.entityData.set(DATA_PARALYZED, this.paralysisTicks > 0);
    }

    // ------------------------------------------------------------------------------------------
    // Sounds - vanilla placeholders (Wither Skeleton family) until the mod has its own audio assets.
    // ------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WITHER_SKELETON_STEP, 0.15F, 1.0F);
    }
}
