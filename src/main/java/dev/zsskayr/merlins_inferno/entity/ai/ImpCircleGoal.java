package dev.zsskayr.merlins_inferno.entity.ai;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.entity.ImpEntity;

/**
 * A courageous pack, between dives: they fan out and jostle for position around the player at
 * 6-10 blocks and a few blocks up - chest out, but in no formation and with no leader.
 */
public class ImpCircleGoal extends Goal {
    private static final double SEARCH_RANGE = 24.0;

    private final ImpEntity imp;
    @Nullable
    private Player player;
    @Nullable
    private Vec3 spot;
    private int repickDelay;

    public ImpCircleGoal(ImpEntity imp) {
        this.imp = imp;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.imp.isCourageous() || !this.imp.isAvailableFighter()) {
            return false;
        }
        this.player = this.imp.level().getNearestPlayer(this.imp.getX(), this.imp.getY(), this.imp.getZ(), SEARCH_RANGE, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        return this.player != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.imp.isCourageous() && this.imp.isAvailableFighter() && this.player != null && this.player.isAlive()
                && this.imp.distanceTo(this.player) < SEARCH_RANGE + 6.0;
    }

    @Override
    public void start() {
        this.repickDelay = 0;
        this.spot = null;
    }

    @Override
    public void stop() {
        this.player = null;
        this.spot = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.player == null) {
            return;
        }
        this.imp.getLookControl().setLookAt(this.player);
        if (--this.repickDelay <= 0 || this.spot == null) {
            this.repickDelay = 30 + this.imp.getRandom().nextInt(25);
            this.spot = this.pickSpot();
        }
        if (this.spot != null) {
            this.imp.flyToward(this.spot, 0.22);
        }
    }

    @Nullable
    private Vec3 pickSpot() {
        Vec3 center = this.player.position();
        Vec3 eye = this.imp.getEyePosition();
        // Stay in the same sector as the rest of the pack (otherwise a ring of imps around the
        // player is >12 blocks apart and the pack's courage - which needs them close - flickers).
        double sumX = 0.0;
        double sumZ = 0.0;
        for (ImpEntity other : this.imp.level().getEntitiesOfClass(ImpEntity.class, this.player.getBoundingBox().inflate(SEARCH_RANGE),
                other -> other != this.imp && other.isAvailableFighter())) {
            Vec3 rel = other.position().subtract(center);
            double length = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
            if (length > 1.0E-3) {
                sumX += rel.x / length;
                sumZ += rel.z / length;
            }
        }
        boolean hasPack = sumX * sumX + sumZ * sumZ > 1.0E-3;
        double packAngle = hasPack ? Math.atan2(sumZ, sumX) : this.imp.getRandom().nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < 6; i++) {
            double angle = packAngle + (this.imp.getRandom().nextDouble() - 0.5) * 1.4;
            double radius = 6.0 + this.imp.getRandom().nextDouble() * 4.0;
            Vec3 candidate = center.add(Math.cos(angle) * radius, 2.0 + this.imp.getRandom().nextDouble() * 3.0, Math.sin(angle) * radius);
            if (this.imp.isClearPath(eye, candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
