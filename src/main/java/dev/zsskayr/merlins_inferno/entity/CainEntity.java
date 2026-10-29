package dev.zsskayr.merlins_inferno.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Cain, the First Immortal - a Circle 2 boss of the Infernal route, brought over from the original Merlin's Oblivion.
 * He walks Oblivion carrying the last, fully awakened {@code Dark Coven}.
 * <ul>
 *     <li><b>Stats (from the original):</b> 800 health, 20 armor, 30 damage; fire-immune, undead; immune to drowning,
 *     explosions and the Wither itself.</li>
 *     <li><b>Withering blows:</b> every blow he lands withers its target for a few seconds.</li>
 *     <li><b>Demonic Pact:</b> below half health he invokes the same pact as his sword every {@link #PACT_INTERVAL}
 *     ticks - faster and tougher for a while.</li>
 *     <li><b>Loot:</b> Ebony Scrap, Netherite Scrap, Nether Star, the rare Hollow Relic
 *     (data/.../loot_table/entities/cain.json); the loot goes into a per-player chest where it fell, like every boss.</li>
 * </ul>
 * Never spawns on his own except in Circle 2 and alone (see {@link #checkCainSpawnRules}); where he is placed is up to
 * Oblivion's own worldgen. Vanilla Wither Skeleton sounds as placeholders.
 */
public class CainEntity extends Monster {
    public static final double MAX_HEALTH = 800.0;
    public static final double ATTACK_DAMAGE = 30.0;
    public static final double ARMOR = 20.0;
    public static final double MOVEMENT_SPEED = 0.28;

    private static final int WITHER_DURATION = 100;
    /** Ticks between two pacts once he is below half health. */
    private static final int PACT_INTERVAL = 500;
    private static final int PACT_DURATION = 160;
    /** A natural spawn attempt only goes through one time in this many - on top of the biome's own weight. */
    private static final int SPAWN_ATTEMPT_ODDS = 40;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.merlins_inferno.cain"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    private int pactTimer;

    public CainEntity(EntityType<? extends CainEntity> type, Level level) {
        super(type, level);
        this.xpReward = 700;
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F); // the sword is his, not loot
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    /**
     * Rare and alone: ground-only, only once the nearest player has reached Circle 2, only one attempt in
     * {@link #SPAWN_ATTEMPT_ODDS}, and never while another Cain is loaded anywhere in the dimension.
     */
    public static boolean checkCainSpawnRules(EntityType<CainEntity> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos,
            RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        // Only Oblivion lists them, but the Nether biomes are shared with the Nether itself.
        if (!(level.getLevel().dimension() == dev.zsskayr.merlins_inferno.portal.OblivionDimension.LEVEL)
                && spawnType != MobSpawnType.SPAWNER && spawnType != MobSpawnType.SPAWN_EGG && spawnType != MobSpawnType.COMMAND) {
            return false;
        }
        if (spawnType == MobSpawnType.SPAWNER || spawnType == MobSpawnType.SPAWN_EGG || spawnType == MobSpawnType.COMMAND) {
            return true;
        }
        if (!level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type)
                || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!ProgressionHelper.hasReached(serverLevel, ProgressionHelper.SECOND_CIRCLE)) {
            return false;
        }
        if (random.nextInt(SPAWN_ATTEMPT_ODDS) != 0) {
            return false;
        }
        return serverLevel.getEntities(EntityTypeTest.forClass(CainEntity.class), Entity::isAlive).isEmpty();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (spawnType != MobSpawnType.NATURAL) {
            this.setPersistenceRequired(); // placed on purpose (egg, command, structure): he stays
        }
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return data;
    }

    /** He always carries the last form of the Dark Coven. */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.DARK_COVEN_VII.get()));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION, 0), this);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.DROWN) || source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.WITHER_SKULL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.WITHER) && super.canBeAffected(effect);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.isAlive() && this.getTarget() != null && this.getHealth() < this.getMaxHealth() * 0.5F && ++this.pactTimer >= PACT_INTERVAL) {
            this.pactTimer = 0;
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, PACT_DURATION, 1));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, PACT_DURATION, 1));
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0, this.getZ(), 40, 0.6, 0.9, 0.6, 0.05);
            this.playSound(SoundEvents.WITHER_SPAWN, 0.8F, 0.6F);
        }
    }

    /** The old Cain trailed ash and flame behind him. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(i == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ASH,
                        this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, 0.02, 0.0);
            }
        }
    }

    /** A Cain that spawned on his own is gone once nobody is around and he is not in a fight - they never pile up. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return this.getTarget() == null;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
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
}
