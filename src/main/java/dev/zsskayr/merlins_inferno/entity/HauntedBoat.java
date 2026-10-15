package dev.zsskayr.merlins_inferno.entity;

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
