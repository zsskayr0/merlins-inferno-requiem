package dev.zsskayr.merlins_inferno.entity.ai;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.entity.ImpEntity;

/**
 * The "hand snatcher". What it can take:
 * <ul>
 *     <li>one gold nugget/ingot/raw gold lying on the ground;</li>
 *     <li>one unit of those held in a player's hand (never weapons/tools/armor from a player);</li>
 *     <li>a golden tool/weapon held in the main hand of any other mob (piglins get furious and
 *     chase the imp - only the robbed one).</li>
 * </ul>
 * Against a living victim it is a fair, readable exchange: {@code APPROACH} (drifts to the side,
 * within ~5 blocks), {@code TELEGRAPH} (1s, stares at the item, arms out, greed murmur) and
 * {@code LUNGE} (a damage-less dash for the item). Putting the item away, stepping up to the imp
 * (&lt; 2.5 blocks during the telegraph), or hitting it aborts the attempt; an aborted or failed
 * attempt puts the imp on an 8-second cooldown. A ground item skips the telegraph - the imp just
 * darts in - but is reserved so only one imp goes after it.
 * <p>
 * A robbed victim is protected from every imp for 30s ({@link ImpTheft#protect}), the sort of
 * shared truce that stops five imps from stripping someone at once.
 */
public class ImpStealGoal extends Goal {
    private enum Phase { APPROACH, TELEGRAPH, LUNGE }

    private static final double SCAN_RANGE = 16.0;
    private static final double TELEGRAPH_DISTANCE = 5.0;
    private static final double PRESSURE_DISTANCE = 2.5;
    private static final int TELEGRAPH_TICKS = 20;
    private static final int LUNGE_TICKS = 14;
    private static final int APPROACH_TIMEOUT = 160;
    private static final int FAILED_ATTEMPT_COOLDOWN = 160; // 8s
    private static final int RETREAT_TICKS = 40;

    private final ImpEntity imp;
    @Nullable
    private Entity target;
    private Phase phase = Phase.APPROACH;
    private int ticks;
    private int scanDelay;
    private boolean finished;
    private float sideSign = 1.0F;
    @Nullable
    private Vec3 approachSpot;

    public ImpStealGoal(ImpEntity imp) {
        this.imp = imp;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.imp.isCarrying() || this.imp.getStealCooldown() > 0 || this.imp.isLowHealth() || this.imp.getPanicTicks() > 0) {
            return false;
        }
        if (--this.scanDelay > 0) {
            return false;
        }
        this.scanDelay = 4 + this.imp.getRandom().nextInt(4);
        this.target = this.findTarget();
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.finished && this.target != null && this.target.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.phase = Phase.APPROACH;
        this.ticks = 0;
        this.finished = false;
        this.approachSpot = null;
        this.sideSign = this.imp.getRandom().nextBoolean() ? 1.0F : -1.0F;
        ImpTheft.reserve(this.target, this.imp.getUUID(), this.imp.level().getGameTime());
        if (this.target instanceof ItemEntity) {
            this.imp.greedSound();
        }
    }

    @Override
    public void stop() {
        if (this.target != null) {
            ImpTheft.release(this.target, this.imp.getUUID());
        }
        this.imp.setState(ImpEntity.STATE_IDLE);
        this.target = null;
    }

    // ---------------------------------------------------------------------------------------
    // Choosing a target
    // ---------------------------------------------------------------------------------------

    @Nullable
    private Entity findTarget() {
        long now = this.imp.level().getGameTime();
        AABB area = this.imp.getBoundingBox().inflate(SCAN_RANGE);
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (ItemEntity item : this.imp.level().getEntitiesOfClass(ItemEntity.class, area,
                item -> ImpTheft.isStealableMaterial(item.getItem()) && !ImpTheft.isIgnored(item, now)
                        && !ImpTheft.isReservedByOther(item, this.imp.getUUID(), now) && this.imp.hasLineOfSight(item))) {
            double distance = this.imp.distanceToSqr(item);
            if (distance < bestDistance) {
                best = item;
                bestDistance = distance;
            }
        }
        for (Player player : this.imp.level().getEntitiesOfClass(Player.class, area,
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator() && heldMaterial(player) != null
                        && !ImpTheft.isProtected(player, now) && !ImpTheft.isReservedByOther(player, this.imp.getUUID(), now)
                        && !(this.imp.wasHurtRecently() && this.imp.getLastHurtByMob() == player)
                        && this.imp.hasLineOfSight(player))) {
            double distance = this.imp.distanceToSqr(player);
            if (distance < bestDistance) {
                best = player;
                bestDistance = distance;
            }
        }
        for (Mob mob : this.imp.level().getEntitiesOfClass(Mob.class, area,
                mob -> !(mob instanceof ImpEntity) && mob.isAlive() && ImpTheft.isGoldenGear(mob.getMainHandItem())
                        && !ImpTheft.isProtected(mob, now) && !ImpTheft.isReservedByOther(mob, this.imp.getUUID(), now)
                        && this.imp.hasLineOfSight(mob))) {
            double distance = this.imp.distanceToSqr(mob);
            if (distance < bestDistance) {
                best = mob;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** The hand stack of {@code player} that holds stealable gold (main hand preferred), or null. */
    @Nullable
    private static ItemStack heldMaterial(Player player) {
        if (ImpTheft.isStealableMaterial(player.getMainHandItem())) {
            return player.getMainHandItem();
        }
        if (ImpTheft.isStealableMaterial(player.getOffhandItem())) {
            return player.getOffhandItem();
        }
        return null;
    }

    private boolean stillHolding() {
        if (this.target instanceof Player player) {
            return heldMaterial(player) != null;
        }
        if (this.target instanceof Mob mob) {
            return ImpTheft.isGoldenGear(mob.getMainHandItem());
        }
        return this.target != null && this.target.isAlive();
    }

    // ---------------------------------------------------------------------------------------
    // The snatch
    // ---------------------------------------------------------------------------------------

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        this.ticks++;
        if (this.imp.hurtTime > 0) {
            this.fail(); // hit while trying: gives up
            return;
        }
        if (this.target instanceof ItemEntity item) {
            this.tickGround(item);
            return;
        }
        switch (this.phase) {
            case APPROACH -> this.tickApproach();
            case TELEGRAPH -> this.tickTelegraph();
            case LUNGE -> this.tickLunge();
        }
    }

    private void tickGround(ItemEntity item) {
        this.imp.getLookControl().setLookAt(item);
        this.imp.flyToward(item.position().add(0.0, 0.1, 0.0), 0.38);
        if (this.imp.getBoundingBox().inflate(0.4).intersects(item.getBoundingBox())) {
            ItemStack stack = item.getItem();
            ItemStack one = stack.split(1);
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
            this.imp.setCarried(one);
            this.imp.laughSound();
            this.finished = true;
        } else if (this.ticks > APPROACH_TIMEOUT) {
            this.imp.setStealCooldown(60);
            this.finished = true;
        }
    }

    private void tickApproach() {
        this.imp.getLookControl().setLookAt(this.target);
        double distance = this.imp.distanceTo(this.target);
        if (distance <= TELEGRAPH_DISTANCE) {
            this.phase = Phase.TELEGRAPH;
            this.ticks = 0;
            this.imp.setState(ImpEntity.STATE_GRABBING);
            this.imp.greedSound();
            return;
        }
        if (this.ticks > APPROACH_TIMEOUT || !this.stillHolding()) {
            this.fail();
            return;
        }
        if (this.approachSpot == null || this.ticks % 10 == 0) {
            // Drift in from the side rather than straight on.
            Vec3 fromTarget = this.imp.position().subtract(this.target.position()).multiply(1.0, 0.0, 1.0);
            if (fromTarget.lengthSqr() < 1.0E-3) {
                fromTarget = new Vec3(1.0, 0.0, 0.0);
            }
            Vec3 dir = fromTarget.normalize().yRot(this.sideSign * 1.1F);
            this.approachSpot = this.target.position().add(dir.x * 4.5, this.target.getBbHeight() * 0.7, dir.z * 4.5);
        }
        this.imp.flyToward(this.approachSpot, 0.3);
    }

    private void tickTelegraph() {
        this.imp.getLookControl().setLookAt(this.target);
        this.imp.setDeltaMovement(this.imp.getDeltaMovement().scale(0.6));
        if (!this.stillHolding() || this.imp.distanceTo(this.target) < PRESSURE_DISTANCE) {
            this.fail(); // item put away, or the target stepped up to it
            return;
        }
        if (this.ticks >= TELEGRAPH_TICKS) {
            this.phase = Phase.LUNGE;
            this.ticks = 0;
            this.imp.playAttackAnimation();
        }
    }

    private void tickLunge() {
        if (!this.stillHolding()) {
            this.fail();
            return;
        }
        Vec3 aim = this.target.position().add(0.0, this.target.getBbHeight() * 0.6, 0.0);
        this.imp.dash(aim.subtract(this.imp.position()), 0.5, 0.6);
        if (this.imp.getBoundingBox().inflate(0.35).intersects(this.target.getBoundingBox())) {
            this.snatch();
        } else if (this.ticks > LUNGE_TICKS) {
            this.fail();
        }
    }

    /** Contact: take the item, protect the victim, and (for a mob) make it furious. */
    private void snatch() {
        long now = this.imp.level().getGameTime();
        if (ImpTheft.isProtected(this.target, now)) {
            this.fail(); // another imp got there first
            return;
        }
        ItemStack taken;
        if (this.target instanceof Player player) {
            ItemStack held = heldMaterial(player);
            if (held == null) {
                this.fail();
                return;
            }
            taken = held.copyWithCount(1);
            held.shrink(1);
        } else if (this.target instanceof Mob mob) {
            taken = mob.getMainHandItem().copy();
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            ImpTheft.angerVictim(mob, this.imp);
        } else {
            this.fail();
            return;
        }
        ImpTheft.protect(this.target, now);
        this.imp.setCarried(taken);
        this.imp.laughSound();
        this.finished = true;
    }

    /** An attempt that didn't work out: back off and wait 8 seconds. */
    private void fail() {
        this.imp.setStealCooldown(FAILED_ATTEMPT_COOLDOWN);
        this.imp.setRetreatTicks(RETREAT_TICKS);
        this.finished = true;
    }
}
