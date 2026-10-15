package dev.zsskayr.merlins_inferno.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidType;

import dev.zsskayr.merlins_inferno.registry.ModItems;

/** The Haunted wood boat: floats on lava as well as water (and, being fire immune by type, never burns). */
public class HauntedBoat extends net.minecraft.world.entity.vehicle.Boat {
    public HauntedBoat(EntityType<? extends net.minecraft.world.entity.vehicle.Boat> type, Level level) {
        super(type, level);
    }

    /**
     * Lava is not as buoyant as water in vanilla's boat physics terms: the hull settles with the rider's feet right at
     * the surface, so they would stand in it. A small constant lift while the hull is in lava shifts that equilibrium
     * up by about a quarter of a block, which keeps the deck (and the rider) clear of the lava.
     */
    private static final double LAVA_LIFT = 0.025D;

    @Override
    public void tick() {
        super.tick();
        BlockPos probe = BlockPos.containing(getX(), getBoundingBox().minY + 0.1D, getZ());
        if (level().getFluidState(probe).is(FluidTags.LAVA)) {
            net.minecraft.world.phys.Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, motion.y + LAVA_LIFT, motion.z);
        }
    }

    @Override
    public Item getDropItem() {
        return ModItems.HAUNTED_BOAT.get();
    }

    @Override
    public boolean canBoatInFluid(FluidState state) {
        return state.is(FluidTags.WATER) || state.is(FluidTags.LAVA);
    }

    @Override
    public boolean canBoatInFluid(FluidType type) {
        return type == Fluids.WATER.getFluidType() || type == Fluids.LAVA.getFluidType();
    }
}
