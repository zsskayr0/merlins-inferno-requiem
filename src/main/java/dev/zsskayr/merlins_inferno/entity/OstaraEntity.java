package dev.zsskayr.merlins_inferno.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.AABB;

import dev.zsskayr.merlins_inferno.worldgen.biome.ModBiomes;

/**
 * Ostara, the Spring Deity - the Mundane (Druidic) path's Circle 1 boss.
 * <ul>
 *     <li><b>Stats:</b> 300 health, 12 damage.</li>
 *     <li><b>Petals:</b> from a distance she flings a volley of petals: magic damage and a bout of Slowness
 *     and Poison ({@link #PETAL_INTERVAL} ticks apart, up to {@link #PETAL_RANGE} blocks, needs line of sight).</li>
 *     <li><b>Bloom:</b> every {@link #BLOOM_INTERVAL} ticks she heals the monsters around her (and herself, less).</li>
 *     <li><b>Spawn:</b> rare, by day, in the Hallowed Grove only, one at a time ({@link #checkOstaraSpawnRules}).</li>
 *     <li><b>Loot:</b> Eve's Secret, the key item of the Pandora Box ritual (data/.../loot_table/entities/ostara.json).</li>
 * </ul>
 * Drawn as a humanoid placeholder.
 */
public class OstaraEntity extends Monster {
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
    private static final double SOLITARY_RADIUS = 256.0;
    /** Fraction of otherwise-valid natural spawn attempts that succeed. */
    private static final float NATURAL_SPAWN_CHANCE = 0.1F;

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

    /** Daytime, Hallowed Grove only, and none of her within {@value #SOLITARY_RADIUS} blocks. */
    public static boolean checkOstaraSpawnRules(EntityType<OstaraEntity> type, ServerLevelAccessor level, MobSpawnType spawnType,
            BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (spawnType == MobSpawnType.SPAWNER || spawnType == MobSpawnType.SPAWN_EGG || spawnType == MobSpawnType.COMMAND) {
            return true;
        }
        if (level.dayTime() % 24000L >= 12000L || !level.getBiome(pos).is(ModBiomes.HALLOWED_GROVE)) {
            return false;
        }
        // By day she is the grove's only monster, so without this gate she wins nearly every spawn attempt.
        if (random.nextFloat() >= NATURAL_SPAWN_CHANCE) {
            return false;
        }
        if (!level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type)
                || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        return level.getEntitiesOfClass(OstaraEntity.class, new AABB(pos).inflate(SOLITARY_RADIUS)).isEmpty();
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
        LivingEntity target = this.getTarget();
        if (target != null && this.tickCount % PETAL_INTERVAL == 0 && this.distanceToSqr(target) <= PETAL_RANGE * PETAL_RANGE
                && this.hasLineOfSight(target)) {
            this.flingPetals(serverLevel, target);
        }
        if (target != null && this.tickCount % BLOOM_INTERVAL == 0) {
            this.bloom(serverLevel);
        }
    }

    private void flingPetals(ServerLevel level, LivingEntity target) {
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
