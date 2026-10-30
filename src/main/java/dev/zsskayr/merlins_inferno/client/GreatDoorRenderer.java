package dev.zsskayr.merlins_inferno.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

import dev.zsskayr.merlins_inferno.blockentity.GreatDoorBlockEntity;

/** Draws a great door (see {@link GreatDoorBlockEntity}). */
public class GreatDoorRenderer extends GeoBlockRenderer<GreatDoorBlockEntity> {
    public GreatDoorRenderer() {
        super(new GreatDoorModel());
    }

    /** The door is 3 wide and 3 tall and its open leaves reach 1.5 blocks out: far beyond its one cell, so it is not culled with it. */
    @Override
    public AABB getRenderBoundingBox(GreatDoorBlockEntity door) {
        BlockPos pos = door.getBlockPos();
        return new AABB(pos.getX() - 3, pos.getY(), pos.getZ() - 3, pos.getX() + 4, pos.getY() + 3, pos.getZ() + 4);
    }
}
