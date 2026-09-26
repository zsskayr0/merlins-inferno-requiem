package dev.zsskayr.merlins_inferno.entity;

import java.util.EnumSet;

import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A worshipper of the Vigil Shrine: a robed devotee who kneels in the pews. Neutral - it ignores players
 * until struck, then fights back and calls the other worshippers within earshot to do the same
 * ({@link HurtByTargetGoal} with alert-others). Never despawns; the shrine spawns them once.
 */
public class WorshipperEntity extends PathfinderMob {
    public WorshipperEntity(EntityType<? extends WorshipperEntity> type, Level level) {
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
        // Retaliation only - and the whole congregation joins in.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, WorshipperEntity.class).setAlertOthers(WorshipperEntity.class));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
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
        private final WorshipperEntity mob;
        private int ticksLeft;

        PrayGoal(WorshipperEntity mob) {
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
