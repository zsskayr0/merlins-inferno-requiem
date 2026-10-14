package dev.zsskayr.merlins_inferno.entity;

import dev.zsskayr.merlins_inferno.block.BenchBlock;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Invisible, stationary mount. Each segment admits one player. */
public class BenchSeatEntity extends Entity {
    public BenchSeatEntity(EntityType<? extends BenchSeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }
    public static void sit(Level level, BlockPos pos, Direction facing, Player player) {
        if (!level.getEntitiesOfClass(BenchSeatEntity.class, new AABB(pos)).isEmpty()) return;
        BenchSeatEntity seat = ModEntityTypes.BENCH_SEAT.get().create(level);
        if (seat == null) return;
        // Match the raised seat surface, slightly forward of the block center.
        seat.setPos(pos.getX()+0.5+facing.getStepX()*0.08, pos.getY()+BenchBlock.SEAT_HEIGHT,
                pos.getZ()+0.5+facing.getStepZ()*0.08);
        seat.setYRot(facing.toYRot());
        if (level.addFreshEntity(seat)) {
            player.setYRot(facing.toYRot());
            if (!player.startRiding(seat)) seat.discard();
        }
    }
    public static void removeAt(Level level, BlockPos pos) {
        for (BenchSeatEntity seat : level.getEntitiesOfClass(BenchSeatEntity.class, new AABB(pos))) {
            seat.ejectPassengers();
            seat.discard();
        }
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && (!isVehicle() || !(level().getBlockState(blockPosition()).getBlock() instanceof BenchBlock))) {
            ejectPassengers();
            discard();
        }
    }
    @Override public Vec3 getPassengerRidingPosition(Entity passenger) { return position(); }
    @Override protected boolean canAddPassenger(Entity passenger) { return passenger instanceof Player && !isVehicle(); }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Direction facing = getDirection();
        for (Direction d : new Direction[]{facing, facing.getClockWise(), facing.getCounterClockWise(), facing.getOpposite()}) {
            Vec3 target = new Vec3(getX()+d.getStepX()*0.8, Math.floor(getY()), getZ()+d.getStepZ()*0.8);
            if (level().noCollision(passenger, passenger.getBoundingBox().move(target.subtract(passenger.position())))) return target;
        }
        return position().add(0, 1.0, 0);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
