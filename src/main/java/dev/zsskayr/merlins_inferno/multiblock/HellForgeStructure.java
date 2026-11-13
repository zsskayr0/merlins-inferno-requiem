package dev.zsskayr.merlins_inferno.multiblock;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.block.HellForgeCoreBlock;
import dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * Validator for the Hell Forge multiblock.
 *
 * <p>The 3x3x3 forge is a closed cube with the Core at its centre and the Controller in the middle of its front face.
 *
 * <p>The expanded forges (5, 7 or 9 wide) are square towers: the Core sits in the middle of the floor, the Controller in the
 * middle of the front wall's bottom row, and the walls rise from a cube (as tall as it is wide) up to {@link #MAX_HEIGHT}.
 * The roof is optional: the top layer's inside is either all roof or all open. Catalysts stand in the two layers above the
 * floor, stacked as the player likes, at most {@code radius - 1} of each kind. Every two layers above the cube add a lane
 * and some fuel room.
 *
 * <p>Edges and corners must be the core's shell block; wall, floor and roof faces may also be glass or a Vault. Nothing is
 * stored in the world for the structure: it is re-checked on demand, so breaking a block un-forms it.
 */
public final class HellForgeStructure {
    /** The tallest an expanded forge may be, roof included. */
    public static final int MAX_HEIGHT = 11;

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

        /** Fuel reserve in buckets of lava for the plain cube; towers add to it (see {@link Shape#fuelBuckets}). */
        public int fuelBuckets() {
            return this.fuelBuckets;
        }

        public int processingSlots() {
            return this.processingSlots;
        }

        public int radius() {
            return this.radius;
        }

        public boolean tower() {
            return this != CUBE_3;
        }

        public BlockPos controllerPos(BlockPos core, Direction facing) {
            return core.offset(this.controllerOffset(facing));
        }

        private BlockPos controllerOffset(Direction facing) {
            return new BlockPos(facing.getStepX() * this.radius, 0, facing.getStepZ() * this.radius);
        }
    }

    private static final Layout[] LAYOUTS = Layout.values();

    public record Core(BlockPos pos, HellForgeCoreBlock block, Layout layout) {
    }

    /**
     * The built shape around a Core: its half width, the lowest and highest layer relative to the Core, and whether the top
     * is roofed. A cube 3 is (1, -1, 1, roofed); a tower is (r, 0, height - 1, roof).
     */
    public record Shape(int radius, int yMin, int yMax, boolean roof) {
        public static final Shape NONE = new Shape(1, -1, 1, true);

        public int height() {
            return this.yMax - this.yMin + 1;
        }

        /** Layers built above the plain cube of this width. */
        public int extraLayers() {
            return Math.max(0, this.height() - (2 * this.radius + 1));
        }

        public int lanes(Layout layout) {
            return Math.min(9, layout.processingSlots() + this.extraLayers() / 2);
        }

        public int fuelBuckets(Layout layout) {
            return layout.fuelBuckets() + this.extraLayers() / 2;
        }

        /** Blocks of the shell: the whole box less its hollow, less the open top's inside. */
        public int shellBlocks() {
            int side = 2 * this.radius + 1, inner = side - 2;
            int blocks = side * side * this.height() - inner * inner * Math.max(0, this.height() - 2);
            return this.roof ? blocks : blocks - inner * inner;
        }

        /** The centre of the box, for effects. */
        public double centreY() {
            return (this.yMin + this.yMax) / 2.0;
        }

        /** 0 for a block inside, otherwise how many of the box's outer planes it lies on (1 face, 2 edge, 3 corner). */
        public int axes(int x, int y, int z) {
            return (Math.abs(x) == this.radius ? 1 : 0) + (Math.abs(z) == this.radius ? 1 : 0)
                    + (y == this.yMin || y == this.yMax ? 1 : 0);
        }

        /** Every face position (where glass or a Vault may stand), in a fixed order: the Vault ports. */
        public List<BlockPos> faces() {
            List<BlockPos> faces = new ArrayList<>();
            for (int x = -this.radius; x <= this.radius; x++) for (int y = this.yMin; y <= this.yMax; y++) for (int z = -this.radius; z <= this.radius; z++) {
                if (this.axes(x, y, z) != 1 || (x == 0 && y == 0 && z == 0)) continue;
                if (!this.roof && y == this.yMax && Math.abs(x) < this.radius && Math.abs(z) < this.radius) continue;
                faces.add(new BlockPos(x, y, z));
            }
            return faces;
        }

        /** Packs the shape for the screen: radius, height and roof in one int. */
        public int pack() {
            return this.radius | this.height() << 4 | (this.roof ? 1 : 0) << 9 | (this.yMin < 0 ? 1 : 0) << 10;
        }

        public static Shape unpack(int packed) {
            int radius = Math.max(1, packed & 15), height = Math.max(3, packed >> 4 & 31);
            boolean centred = (packed >> 10 & 1) == 1 || radius == 1;
            int yMin = centred ? -(height / 2) : 0;
            return new Shape(radius, yMin, yMin + height - 1, (packed >> 9 & 1) == 1);
        }
    }

    /** {@code catalysts} counts each kind: 0 speed, 1 yield, 2 stabilizer, 3 bellows, 4 economizer, 5 ward. */
    public record Result(@Nullable Core core, Shape shape, int processingSlots, int[] catalysts, @Nullable Component error,
                         int tier, @Nullable Block shell) {
        private static Result fail(@Nullable Core core, String error, Object... args) {
            return new Result(core, Shape.NONE, 0, new int[6], Component.translatable("message.merlins_inferno.hell_forge." + error, args), 0, null);
        }

        public int speedCatalysts() {
            return this.catalysts[0];
        }

        public int yieldCatalysts() {
            return this.catalysts[1];
        }

        public boolean formed() {
            return this.error == null;
        }

        public int coreTier() {
            return this.formed() && this.core != null ? this.tier : 0;
        }
    }

    private HellForgeStructure() {
    }

    /** Every Core whose forge box could contain the given block (shell, interior or above the floor). */
    public static List<Core> coresAround(Level level, BlockPos pos) {
        List<Core> found = new ArrayList<>();
        for (Layout layout : LAYOUTS) {
            int r = layout.radius();
            int low = layout.tower() ? -(MAX_HEIGHT - 1) : -1, high = layout.tower() ? 0 : 1;
            for (int dx = -r; dx <= r; dx++) for (int dy = low; dy <= high; dy++) for (int dz = -r; dz <= r; dz++) {
                Core core = coreAt(level, pos.offset(dx, dy, dz), layout);
                if (core != null && !found.contains(core)) found.add(core);
            }
        }
        return found;
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

    /** How tall a tower is: its corner column of shell, counted up from the floor, at most {@link #MAX_HEIGHT}. */
    private static int measureHeight(Level level, BlockPos core, int radius, Block shell) {
        int height = 0;
        while (height < MAX_HEIGHT) {
            BlockPos corner = core.offset(radius, height, radius);
            if (!level.hasChunkAt(corner) || !level.getBlockState(corner).is(shell)) break;
            height++;
        }
        return height;
    }

    private static boolean face(BlockState state) {
        return state.is(ModBlocks.HELLGLASS.get()) || state.is(ModBlocks.CRYPT_GLASS.get()) || state.getBlock() instanceof HellForgeVaultBlock;
    }

    public static Result validate(Level level, BlockPos controllerPos, Direction facing) {
        Core core = findCoreFromController(level, controllerPos, facing);
        if (core == null) {
            BlockPos guess = controllerPos.relative(facing.getOpposite());
            return Result.fail(null, "need_core", guess.getX(), guess.getY(), guess.getZ());
        }
        Layout layout = core.layout();
        int radius = layout.radius();
        // A Magmatic Core takes whichever shell its corner shows, and the matching controller; the others have their own.
        Block shellBlock = core.block().shellBlock();
        Block controllerBlock = core.block().controllerBlock();
        int tier = core.block().getTier();
        if (core.block().magmatic()) {
            BlockState corner = level.getBlockState(core.pos().offset(radius, layout.tower() ? 0 : -1, radius));
            if (corner.is(ModBlocks.BLIGHTSTEEL_CASING.get())) {
                shellBlock = ModBlocks.BLIGHTSTEEL_CASING.get();
                controllerBlock = ModBlocks.BLIGHTED_HELLFORGE_CONTROLLER.get();
                tier = 2;
            } else {
                shellBlock = ModBlocks.COMPRESSED_NETHERRACK.get();
                controllerBlock = ModBlocks.HELL_FORGE_CONTROLLER.get();
                tier = 1;
            }
        }
        if (!level.getBlockState(controllerPos).is(controllerBlock)) {
            return Result.fail(core, "wrong_controller");
        }
        if (layout.tower() && level instanceof ServerLevel serverLevel
                && !ProgressionHelper.hasReached(serverLevel, ProgressionHelper.SECOND_CIRCLE)) {
            return Result.fail(core, "need_second_circle");
        }
        Shape shape;
        if (layout.tower()) {
            int height = measureHeight(level, core.pos(), radius, shellBlock);
            if (height < 2 * radius + 1) {
                BlockPos top = core.pos().offset(radius, height, radius);
                return Result.fail(core, "need_shell", top.getX(), top.getY(), top.getZ());
            }
            // The top layer's inside decides: shell, glass or a Vault anywhere there means a roof, which must then be whole.
            boolean roof = false;
            for (int x = -radius + 1; x < radius && !roof; x++) for (int z = -radius + 1; z < radius && !roof; z++) {
                roof = !level.getBlockState(core.pos().offset(x, height - 1, z)).isAir();
            }
            shape = new Shape(radius, 0, height - 1, roof);
        } else {
            shape = new Shape(1, -1, 1, true);
        }
        int cap = Math.max(0, radius - 1);
        int[] catalysts = new int[6];
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = shape.yMin(); dy <= shape.yMax(); dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = core.pos().offset(dx, dy, dz);
                    if (pos.equals(controllerPos) || (dx == 0 && dy == 0 && dz == 0)) {
                        continue; // the anchor and the Core themselves
                    }
                    if (!level.hasChunkAt(pos)) {
                        return Result.fail(core, "incomplete");
                    }
                    BlockState state = level.getBlockState(pos);
                    int axes = shape.axes(dx, dy, dz);
                    boolean openTop = !shape.roof() && dy == shape.yMax() && axes == 1;
                    if (axes == 0 || openTop) {
                        if (state.isAir()) {
                            continue;
                        }
                        if (openTop) {
                            return Result.fail(core, "need_roof", pos.getX(), pos.getY(), pos.getZ());
                        }
                        boolean catalystLayer = layout.tower() ? dy == shape.yMin() + 1 || dy == shape.yMin() + 2 : false;
                        if (!catalystLayer) {
                            return Result.fail(core, "need_air", pos.getX(), pos.getY(), pos.getZ());
                        }
                        if (core.block().magmatic()) {
                            return Result.fail(core, "need_air", pos.getX(), pos.getY(), pos.getZ());
                        }
                        int kind = core.block().catalystKind(state);
                        if (kind == -2) {
                            return Result.fail(core, "wrong_catalyst", pos.getX(), pos.getY(), pos.getZ());
                        }
                        if (kind < 0) {
                            return Result.fail(core, "need_air_or_catalyst", pos.getX(), pos.getY(), pos.getZ());
                        }
                        if (++catalysts[kind] > cap) {
                            return Result.fail(core, "too_many_catalysts", cap);
                        }
                        continue;
                    }
                    if (state.is(shellBlock)) {
                        continue;
                    }
                    if (axes == 1) {
                        if (face(state)) continue;
                        return Result.fail(core, "need_face", pos.getX(), pos.getY(), pos.getZ());
                    }
                    return Result.fail(core, "need_shell", pos.getX(), pos.getY(), pos.getZ());
                }
            }
        }
        return new Result(core, shape, core.block().magmatic() ? 1 : shape.lanes(layout), catalysts, null, tier, shellBlock);
    }
}
