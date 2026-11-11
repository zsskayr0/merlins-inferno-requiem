package dev.zsskayr.merlins_inferno.multiblock;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.block.HellForgeCoreBlock;
import dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Validator for the Hell Forge multiblock. The Core sits at the centre of the cube.
 * Expanded forges can have a glass front with the Controller centred beneath it.
 * In the 3x3x3 forge the Controller sits at the centre of the front face.
 * Vaults sit at the centres of the other faces.
 * Expanded forges have an open interior with catalysts in its top and bottom layers.
 * Nothing is placed or stored in the world for the structure: it is re-checked on demand, so
 * breaking any block simply un-forms it and rebuilding it re-forms it.
 */
public final class HellForgeStructure {
    public enum Layout {
        CUBE_3(1, 1, 1),
        CUBE_5(2, 5, 3),
        CUBE_7(3, 7, 6),
        CUBE_9(4, 9, 10);

        private final int radius;
        private final int processingSlots;
        private final int fuelBuckets;

        Layout(int radius, int processingSlots, int fuelBuckets) {
            this.fuelBuckets = fuelBuckets;
            this.radius = radius;
            this.processingSlots = processingSlots;
        }

        /** Fuel reserve in buckets of lava; larger forges hold more. */
        public int fuelBuckets() {
            return this.fuelBuckets;
        }

        public int radius() {
            return this.radius;
        }

        public BlockPos faceCentre(BlockPos centre, Direction outward) {
            return centre.relative(outward, this.radius);
        }

        public BlockPos controllerPos(BlockPos centre, Direction facing) {
            return centre.offset(this.controllerOffset(facing));
        }

        private BlockPos controllerOffset(Direction facing) {
            int y = this == CUBE_3 ? 0 : -this.radius;
            return new BlockPos(facing.getStepX() * this.radius, y, facing.getStepZ() * this.radius);
        }
    }

    private static final Layout[] LAYOUTS = Layout.values();

    public record Core(BlockPos pos, HellForgeCoreBlock block, Layout layout) {
    }

    /** The Core can be present even if another part of the structure is missing. */
    public record Result(@Nullable Core core, int processingSlots, int speedCatalysts, int yieldCatalysts,
                         @Nullable Component error) {
        private static Result fail(@Nullable Core core, String error, Object... args) {
            return new Result(core, 0, 0, 0, Component.translatable("message.merlins_inferno.hell_forge." + error, args));
        }

        public boolean formed() {
            return this.error == null;
        }

        public int coreTier() {
            return this.formed() && this.core != null ? this.core.block().getTier() : 0;
        }
    }

    private HellForgeStructure() {
    }

    /** Finds the Core from a face centre, even while the rest of the forge is incomplete. */
    @Nullable
    public static Core findCore(Level level, BlockPos facePos, Direction outward) {
        for (Layout layout : LAYOUTS) {
            int r = layout.radius();
            for (int u = -r + 1; u < r; u++) for (int v = -r + 1; v < r; v++) {
                BlockPos origin = facePos.relative(outward.getOpposite(), r);
                BlockPos pos = switch (outward.getAxis()) {
                    case X -> origin.offset(0, u, v);
                    case Y -> origin.offset(u, 0, v);
                    case Z -> origin.offset(u, v, 0);
                };
                Core core = coreAt(level, pos, layout);
                if (core != null) return core;
            }
        }
        return null;
    }

    @Nullable
    public static Core findCoreFromController(Level level, BlockPos controllerPos, Direction facing) {
        for (Layout layout : LAYOUTS) {
            BlockPos pos = controllerPos.subtract(layout.controllerOffset(facing));
            Core core = coreAt(level, pos, layout);
            if (core != null) {
                return core;
            }
        }
        return null;
    }

    @Nullable
    private static Core coreAt(Level level, BlockPos pos, Layout layout) {
        if (level.hasChunkAt(pos)
                && level.getBlockState(pos).getBlock() instanceof HellForgeCoreBlock core
                && core.supports(layout)) {
            return new Core(pos, core, layout);
        }
        return null;
    }

    public static Result validate(Level level, BlockPos controllerPos, Direction facing) {
        Core core = findCoreFromController(level, controllerPos, facing);
        Layout layout = core == null ? Layout.CUBE_3 : core.layout();
        BlockPos centre = core == null ? layout.faceCentre(controllerPos, facing.getOpposite()) : core.pos();
        int radius = layout.radius();
        if (radius > 1 && level instanceof ServerLevel serverLevel
                && !ProgressionHelper.hasReached(serverLevel, ProgressionHelper.SECOND_CIRCLE)) {
            return Result.fail(core, "need_second_circle");
        }
        int speedCatalysts = 0;
        int yieldCatalysts = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = centre.offset(dx, dy, dz);
                    if (pos.equals(controllerPos)) {
                        continue; // the anchor itself
                    }
                    if (!level.hasChunkAt(pos)) {
                        return Result.fail(core, "incomplete");
                    }
                    BlockState state = level.getBlockState(pos);
                    if (pos.equals(centre)) {
                        if (core == null) {
                            return Result.fail(null, "need_core", pos.getX(), pos.getY(), pos.getZ());
                        }
                        continue;
                    }
                    int shellAxes = (Math.abs(dx) == radius ? 1 : 0)
                            + (Math.abs(dy) == radius ? 1 : 0) + (Math.abs(dz) == radius ? 1 : 0);
                    if (shellAxes == 0) {
                        if (state.isAir()) {
                            continue;
                        }
                        if (Math.abs(dy) != radius - 1) {
                            return Result.fail(core, "need_air", pos.getX(), pos.getY(), pos.getZ());
                        }
                        if (state.is(ModTags.Blocks.HELL_FORGE_SPEED_CATALYSTS)) {
                            speedCatalysts++;
                        } else if (state.is(ModTags.Blocks.HELL_FORGE_YIELD_CATALYSTS)) {
                            yieldCatalysts++;
                        } else {
                            return Result.fail(core, "need_air_or_catalyst", pos.getX(), pos.getY(), pos.getZ());
                        }
                        continue;
                    }
                    if (state.is(ModBlocks.COMPRESSED_NETHERRACK.get())) {
                        continue;
                    }
                    if (shellAxes == 1) {
                        if (state.is(ModBlocks.HELLGLASS.get()) || state.is(ModBlocks.CRYPT_GLASS.get())
                                || state.getBlock() instanceof HellForgeVaultBlock) continue;
                        return Result.fail(core, "need_face", pos.getX(), pos.getY(), pos.getZ());
                    }
                    return Result.fail(core, "need_shell", pos.getX(), pos.getY(), pos.getZ());
                }
            }
        }
        return new Result(core, layout.processingSlots, speedCatalysts, yieldCatalysts, null);
    }
}
