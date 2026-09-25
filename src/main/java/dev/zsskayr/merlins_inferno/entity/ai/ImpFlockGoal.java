package dev.zsskayr.merlins_inferno.entity.ai;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.entity.ImpEntity;

/**
 * Loose flocking: an imp with nothing better to do drifts back toward the nearest other imp
 * until they're within ~7 blocks. This is what rebuilds a pack after a dispersal (courage lost,
 * dives ending in scattered retreats) - without it, imps would stay scattered beyond the
 * courage radius forever. Below every fight/flee/theft goal, so it never overrides those.
 */
public class ImpFlockGoal extends Goal {
    private static final double SEARCH_RANGE = 40.0;
    private static final double COMFORT_DISTANCE = 7.0;
    /** Keeps regrouping imps just outside the shy-flee radius of a nearby player. */
    private static final double PLAYER_MARGIN = 12.5;

    private final ImpEntity imp;
    @Nullable
    private ImpEntity buddy;
    private int scanDelay;

    public ImpFlockGoal(ImpEntity imp) {
        this.imp = imp;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.imp.isCarrying() || this.imp.isLowHealth() || --this.scanDelay > 0) {
            return false;
        }
        this.scanDelay = 10 + this.imp.getRandom().nextInt(10);
        ImpEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (ImpEntity other : this.imp.level().getEntitiesOfClass(ImpEntity.class, this.imp.getBoundingBox().inflate(SEARCH_RANGE),
                other -> other != this.imp && other.isAvailableFighter())) {
            double distance = this.imp.distanceToSqr(other);
            if (distance < best) {
                best = distance;
                nearest = other;
            }
        }
        if (nearest == null || best <= COMFORT_DISTANCE * COMFORT_DISTANCE) {
            return false;
        }
        this.buddy = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.buddy != null && this.buddy.isAlive() && !this.imp.isCarrying()
                && this.imp.distanceToSqr(this.buddy) > COMFORT_DISTANCE * COMFORT_DISTANCE;
    }

    @Override
    public void stop() {
        this.buddy = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.buddy == null) {
            return;
        }
        Vec3 toBuddy = this.buddy.position().subtract(this.imp.position());
        // Regrouping must not mean flying through the player: near one, slide around it instead
        // of cutting across (drop the inward component, push out if inside the comfort ring).
        Player player = this.imp.level().getNearestPlayer(this.imp.getX(), this.imp.getY(), this.imp.getZ(), PLAYER_MARGIN + 3.0, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        if (player != null) {
            Vec3 rel = this.imp.position().subtract(player.position());
            double distance = rel.length();
            if (distance > 1.0E-3) {
                Vec3 radial = rel.scale(1.0 / distance);
                double inward = -toBuddy.dot(radial);
                if (inward > 0.0) {
                    toBuddy = toBuddy.add(radial.scale(inward));
                }
                if (distance < PLAYER_MARGIN) {
                    toBuddy = toBuddy.add(radial.scale(PLAYER_MARGIN - distance));
                }
            }
        }
        this.imp.flyToward(this.imp.position().add(toBuddy), 0.16);
    }
}
