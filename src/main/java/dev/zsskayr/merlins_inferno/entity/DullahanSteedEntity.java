package dev.zsskayr.merlins_inferno.entity;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The black, spectral horse the {@link DullahanEntity} rides. It looks like vanilla's skeleton
 * horse (see the client-side {@code DullahanSteedRenderer}) but is its own entity type so that its
 * AI can be replaced outright: a vanilla horse only takes orders from a Player rider, so a mob rider
 * would just be carried around by the horse's own wandering goals.
 * <ul>
 *     <li><b>Steering:</b> the steed goes after whatever its rider is targeting
 *     ({@link ChaseRiderTargetGoal}) and wanders otherwise; the Dullahan does the fighting from the
 *     saddle. While the rider is gold-paralyzed the steed freezes too.</li>
 *     <li><b>Not a prize:</b> players can't mount, feed or tame it, it drops nothing and gives no
 *     experience. It vanishes shortly after it no longer has a rider (the Dullahan died or faded), so
 *     a lone horse is never left behind. It can be killed - the Dullahan then fights on foot.</li>
 * </ul>
 */
public class DullahanSteedEntity extends SkeletonHorse {
    /** Ticks a riderless steed lingers before it fades (2s). */
    private static final int ORPHAN_FADE_TICKS = 40;

    /**
     * Movement speed attribute, and the multiplier the chase goal asks for. A mounted Dullahan moves at exactly the
     * steed's speed, so THIS is how fast the boss is. It was 0.3 x 1.25 = 0.375 (faster than a sprinting player could
     * comfortably kite); now 0.2 x 1.0. Tune these two.
     */
    public static final double MOVEMENT_SPEED = 0.2;
    private static final double CHASE_SPEED_MODIFIER = 1.0;

    private int orphanTicks;

    public DullahanSteedEntity(EntityType<? extends DullahanSteedEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractHorse.createBaseHorseAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Nullable
    private DullahanEntity rider() {
        return this.getFirstPassenger() instanceof DullahanEntity dullahan ? dullahan : null;
    }

    // ------------------------------------------------------------------------------------------
    // AI - replaces the horse's own (panic, breeding, tempting, ...) entirely
    // ------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ChaseRiderTargetGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
    }

    @Override
    protected void addBehaviourGoals() {
        // registerGoals() above already defines everything this mount does.
    }

    private class ChaseRiderTargetGoal extends Goal {
        ChaseRiderTargetGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Nullable
        private LivingEntity target() {
            DullahanEntity rider = DullahanSteedEntity.this.rider();
            if (rider == null || rider.isParalyzed()) {
                return null;
            }
            LivingEntity target = rider.getTarget();
            return target != null && target.isAlive() ? target : null;
        }

        @Override
        public boolean canUse() {
            return this.target() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = this.target();
            if (target != null) {
                DullahanSteedEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                DullahanSteedEntity.this.getNavigation().moveTo(target, CHASE_SPEED_MODIFIER);
            }
        }

        @Override
        public void stop() {
            DullahanSteedEntity.this.getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.isVehicle()) {
                this.orphanTicks = 0;
            } else if (++this.orphanTicks > ORPHAN_FADE_TICKS) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + this.getBbHeight() / 2.0, this.getZ(),
                        25, this.getBbWidth() / 2.0, this.getBbHeight() / 3.0, this.getBbWidth() / 2.0, 0.02);
                this.discard();
            }
        }
    }

    /** Frozen while the rider is: a paralyzed Dullahan can't be carried off. */
    @Override
    public boolean isImmobile() {
        DullahanEntity rider = this.rider();
        return super.isImmobile() || (rider != null && rider.isParalyzed());
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // lives and dies with its rider, see tick()
    }

    /** Not for players: no mounting, feeding or taming. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }
}
