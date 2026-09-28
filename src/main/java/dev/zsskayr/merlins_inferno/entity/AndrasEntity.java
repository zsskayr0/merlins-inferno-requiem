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

import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;

/**
 * Andras, the Forgotten Demon - the Infernal path's Circle 1 boss: a lesser demon, the overseer of the Nether's
 * lower ranks. He keeps Imps at his beck and call and treats Starveds as tools.
 * <ul>
 *     <li><b>Stats:</b> 370 health, 15 damage; fire-immune; a demon (see the {@code demon} entity tag).</li>
 *     <li><b>Contracts:</b> while he fights, every {@link #SUMMON_INTERVAL} ticks he calls up to
 *     {@link #SUMMON_COUNT} Imps (never more than {@link #MAX_IMPS} of them around him).</li>
 *     <li><b>Mark of Debt:</b> every blow he lands brands a player ({@code ModEffects#MARK_OF_DEBT}): more damage
 *     taken, for {@link #MARK_DURATION} ticks.</li>
 *     <li><b>Loot:</b> the Book of Contracts, the key item of the Pandora Box ritual (data/.../loot_table/entities/andras.json).</li>
 * </ul>
 * Spawns rarely in the Nether, solitary. His fortress is future content. Drawn as an enlarged humanoid placeholder.
 */
public class AndrasEntity extends Monster {
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
            this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 0.6F);
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
