package dev.zsskayr.merlins_inferno.worldgen.elysium.structure;

import net.minecraft.core.Direction;

/**
 * A site's local frame: origin plus one of four rotations. Local {@code u} runs "forward" (the direction the site faces,
 * {@code Direction.from2DDataValue(rot)}: 0 south, 1 west, 2 north, 3 east), local {@code v} runs to the forward
 * direction's clockwise neighbour. Builders draw in (u, v) and never think about rotation again.
 */
final class Frame {
    final int ox, oz, rot;
    final Direction forward, right;
    private final int fx, fz, rx, rz;

    Frame(int ox, int oz, int rot) {
        this.ox = ox;
        this.oz = oz;
        this.rot = Math.floorMod(rot, 4);
        this.forward = Direction.from2DDataValue(this.rot);
        this.right = forward.getClockWise();
        this.fx = forward.getStepX();
        this.fz = forward.getStepZ();
        this.rx = right.getStepX();
        this.rz = right.getStepZ();
    }

    int x(int u, int v) {
        return ox + fx * u + rx * v;
    }

    int z(int u, int v) {
        return oz + fz * u + rz * v;
    }

    /** World x of a fractional local point (rounded to the block). */
    int x(double u, double v) {
        return (int) Math.floor(ox + 0.5 + fx * u + rx * v);
    }

    int z(double u, double v) {
        return (int) Math.floor(oz + 0.5 + fz * u + rz * v);
    }

    /** World direction of a local horizontal direction where SOUTH means "forward" and WEST means "right". */
    Direction dir(Direction local) {
        if (local.getAxis() == Direction.Axis.Y) {
            return local;
        }
        Direction d = local;
        for (int i = 0; i < rot; i++) {
            d = d.getClockWise();
        }
        return d;
    }

    /** World direction for "towards +u" (forward) / "towards -u" (back) / "+v" / "-v". */
    Direction fwd() {
        return forward;
    }

    Direction back() {
        return forward.getOpposite();
    }

    Direction rightDir() {
        return right;
    }

    Direction leftDir() {
        return right.getOpposite();
    }
}
