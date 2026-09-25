package dev.zsskayr.merlins_inferno.entity.ai;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.entity.ImpEntity;

/**
 * After a theft the imp doesn't fly off forever: once out of its threat's reach it finds a
 * nearby perch (open air next to a solid surface, never over lava) and hovers there admiring its
 * prize, until something comes within reach again (the urgent flee goal then takes over).
 */
public class ImpPerchGoal extends Goal {
    private static final double THREAT_RANGE = 12.0;

    private final ImpEntity imp;
    @Nullable
    private Vec3 perch;
    private int ticks;

    public ImpPerchGoal(ImpEntity imp) {
        this.imp = imp;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.imp.isCarrying() && this.imp.findThreat(THREAT_RANGE) == null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.perch = this.findPerch();
        this.ticks = 0;
    }

    @Override
    public void stop() {
        this.perch = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.perch == null) {
            if (this.ticks % 20 == 0) {
                this.perch = this.findPerch();
            }
            this.imp.setDeltaMovement(this.imp.getDeltaMovement().scale(0.9));
            return;
        }
        if (this.imp.position().distanceToSqr(this.perch) > 1.5) {
            this.imp.flyToward(this.perch, 0.3);
        } else {
            // Hover in place and gloat.
            this.imp.setDeltaMovement(this.imp.getDeltaMovement().scale(0.8));
            if (this.ticks % 80 == 0) {
                this.imp.greedSound();
            }
        }
    }

    @Nullable
    private Vec3 findPerch() {
        Level level = this.imp.level();
        BlockPos origin = this.imp.blockPosition();
        Vec3 eye = this.imp.getEyePosition();
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockPos pos = origin.offset(
                    this.imp.getRandom().nextInt(17) - 8,
                    this.imp.getRandom().nextInt(7) - 3,
                    this.imp.getRandom().nextInt(17) - 8);
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
                continue;
            }
            // A solid surface within 3 blocks below, with no lava anywhere above it.
            boolean surface = false;
            for (int dy = 1; dy <= 6; dy++) {
                BlockPos below = pos.below(dy);
                if (level.getFluidState(below).is(FluidTags.LAVA)) {
                    break;
                }
                if (!level.getBlockState(below).isAir()) {
                    surface = dy <= 3;
                    break;
                }
            }
            if (!surface) {
                continue;
            }
            Vec3 center = Vec3.atBottomCenterOf(pos).add(0.0, 0.3, 0.0);
            if (this.imp.isClearPath(eye, center)) {
                return center;
            }
        }
        return null;
    }
}
