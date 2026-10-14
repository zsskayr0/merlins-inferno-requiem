package dev.zsskayr.merlins_inferno.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.server.level.ServerLevel;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;

/**
 * The Flesh Golem - a Circle 2 miniboss of the Infernal route, brought over from the original Merlin's Oblivion (where
 * it was a 50-health mob; it is a proper miniboss now).
 * <ul>
 *     <li><b>Stats:</b> 220 health, 14 damage, 6 armor, heavy knockback resistance; fire-immune, undead.</li>
 *     <li><b>Gorging:</b> every blow it lands heals it ({@link #HEAL_PER_HIT} health).</li>
 *     <li><b>Loot:</b> Ebony Scrap, Devil Flesh and the rare Hollow Relic (data/.../loot_table/entities/flesh_golem.json).</li>
 * </ul>
 * Circle 2 only, and alone-ish: a natural spawn needs the nearest player to have reached Circle 2. Where it spawns is
 * Oblivion's own worldgen/biome spawn list. Vanilla Husk sounds as placeholders.
 */
public class FleshGolemEntity extends Monster {
    public static final double MAX_HEALTH = 220.0;
    public static final double ATTACK_DAMAGE = 14.0;
    public static final double ARMOR = 6.0;
    public static final double MOVEMENT_SPEED = 0.24;

    private static final float HEAL_PER_HIT = 6.0F;
    private static final double CIRCLE_CHECK_RADIUS = 64.0;

    public FleshGolemEntity(EntityType<? extends FleshGolemEntity> type, Level level) {
        super(type, level);
        this.xpReward = 150;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** A regular ground spawn, but only once the nearest player has reached Circle 2. */
    public static boolean checkFleshGolemSpawnRules(EntityType<FleshGolemEntity> type, ServerLevelAccessor level, MobSpawnType spawnType,
            BlockPos pos, RandomSource random) {
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
        Player nearest = serverLevel.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, CIRCLE_CHECK_RADIUS, false);
        return nearest != null && ProgressionHelper.hasReached(nearest, ProgressionHelper.SECOND_CIRCLE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.heal(HEAL_PER_HIT);
        }
        return hit;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.hasCustomName() && distanceToClosestPlayer > 128.0;
    }

    // Vanilla placeholders until the mod has its own audio.
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HUSK_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.HUSK_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HUSK_DEATH;
    }
}
