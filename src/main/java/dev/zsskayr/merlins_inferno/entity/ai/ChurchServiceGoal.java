package dev.zsskayr.merlins_inferno.entity.ai;

import java.util.EnumSet;
import javax.annotation.Nullable;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Sacred Church's night service. From dusk to dawn a churchgoer walks to its assigned post - a pew for the
 * congregation, the far side of the altar for the Priest - turns to face where it should, and kneels in prayer. It gets
 * up at dawn, or the moment it is hurt, provoked or warned (it has a target, or was struck: {@link Attendee#isAlarmed()}),
 * and does not go back until the next dusk. This is the only place the prayer pose is used.
 */
public class ChurchServiceGoal<T extends PathfinderMob & ChurchServiceGoal.Attendee> extends Goal {
    /** Time of day (ticks) the service starts and ends: the night. */
    public static final long SERVICE_START = 13000L;
    public static final long SERVICE_END = 23000L;

    private static final double ARRIVE_HORIZONTAL_SQR = 1.5 * 1.5;
    private static final double ARRIVE_VERTICAL = 1.6;
    /** After this long on the way, close enough is good enough (a blocked pew must not strand it). */
    private static final int MAX_TRAVEL_TICKS = 900;
    private static final double GIVE_UP_DISTANCE_SQR = 12.0 * 12.0;

    public interface Attendee {
        /** Where its feet go during the service, or null if it has no post (a wanderer of the open world). */
        @Nullable
        Vec3 servicePost();

        /** The yaw it holds while praying. */
        float serviceYaw();

        /** True once it was hurt or warned; stays true until the next dawn. */
        boolean isAlarmed();
    }

    private final T mob;
    private final double speed;
    private boolean arrived;
    private int travelTicks;

    public ChurchServiceGoal(T mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    public static boolean isServiceTime(Level level) {
        long time = level.getDayTime() % 24000L;
        return level.dimensionType().hasSkyLight() && time >= SERVICE_START && time < SERVICE_END;
    }

    @Override
    public boolean canUse() {
        return this.attending();
    }

    @Override
    public boolean canContinueToUse() {
        return this.attending();
    }

    private boolean attending() {
        return this.mob.servicePost() != null && this.mob.getTarget() == null && !this.mob.isAlarmed() && !this.mob.isPassenger()
                && isServiceTime(this.mob.level());
    }

    @Override
    public void start() {
        this.arrived = false;
        this.travelTicks = 0;
    }

    @Override
    public void tick() {
        Vec3 post = this.mob.servicePost();
        if (post == null) {
            return;
        }
        if (!this.arrived) {
            double dx = this.mob.getX() - post.x;
            double dz = this.mob.getZ() - post.z;
            double distSqr = dx * dx + dz * dz;
            boolean close = distSqr <= ARRIVE_HORIZONTAL_SQR && Math.abs(this.mob.getY() - post.y) <= ARRIVE_VERTICAL;
            boolean stuckNearby = ++this.travelTicks > MAX_TRAVEL_TICKS && distSqr <= GIVE_UP_DISTANCE_SQR;
            if (close || stuckNearby) {
                this.arrived = true;
                this.mob.getNavigation().stop();
                this.snapToPost(post);
                this.mob.setPose(Pose.CROUCHING);
            } else if (this.mob.getNavigation().isDone() || this.mob.tickCount % 40 == 0) {
                this.mob.getNavigation().moveTo(post.x, post.y, post.z, this.speed);
            }
            return;
        }
        // Kneeling: hold the spot and the facing, whatever nudged us.
        if (this.mob.position().distanceToSqr(post) > 0.09) {
            this.snapToPost(post);
        }
        this.mob.setYRot(this.mob.serviceYaw());
        this.mob.setYHeadRot(this.mob.serviceYaw());
        this.mob.yBodyRot = this.mob.serviceYaw();
    }

    private void snapToPost(Vec3 post) {
        this.mob.moveTo(post.x, post.y, post.z, this.mob.serviceYaw(), 0.0F);
        this.mob.setDeltaMovement(Vec3.ZERO);
        this.mob.setYHeadRot(this.mob.serviceYaw());
        this.mob.yBodyRot = this.mob.serviceYaw();
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        if (this.mob.getPose() == Pose.CROUCHING) {
            this.mob.setPose(Pose.STANDING);
        }
    }
}
