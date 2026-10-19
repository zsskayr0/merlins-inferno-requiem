package dev.zsskayr.merlins_inferno.multiblock;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.block.HellForgeCoreBlock;
import dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * Validator for the Hell Forge's fixed 3x3x3 multiblock. There is exactly one layout, anchored on
 * the Controller, which sits at the centre of the front face (the face it points {@code FACING}
 * towards) - the cube's centre is therefore one block behind the Controller:
 * <ul>
 *     <li>centre (1): a {@link HellForgeCoreBlock};</li>
 *     <li>face centres (6): the Controller itself on the front face; on the other five Compressed
 *     Netherrack, a {@link HellForgeVaultBlock} or Hellglass (from Circle 2 - a window onto the Core);</li>
 *     <li>edges and corners (20): Compressed Netherrack.</li>
 * </ul>
 * Nothing is placed or stored in the world for the structure: it is re-checked on demand, so
 * breaking any block simply un-forms it and rebuilding it re-forms it.
 */
public final class HellForgeStructure {
    /** Outcome of a check: {@code error} is {@code null} exactly when {@code formed} is true. */
    public record Result(boolean formed, int coreTier, @Nullable Component error) {
        static Result ok(int coreTier) {
            return new Result(true, coreTier, null);
        }

        static Result fail(Component error) {
            return new Result(false, 0, error);
        }
    }

    private HellForgeStructure() {
    }

    /** The cube's centre, where the Core must be, for a Controller at {@code controllerPos}. */
    public static BlockPos corePos(BlockPos controllerPos, Direction facing) {
        return controllerPos.relative(facing.getOpposite());
    }

    public static Result validate(Level level, BlockPos controllerPos, Direction facing) {
        BlockPos centre = corePos(controllerPos, facing);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = centre.offset(dx, dy, dz);
                    if (pos.equals(controllerPos)) {
                        continue; // the anchor itself
                    }
                    if (!level.hasChunkAt(pos)) {
                        return Result.fail(Component.translatable("message.merlins_inferno.hell_forge.incomplete"));
                    }
                    BlockState state = level.getBlockState(pos);
                    int axes = (dx != 0 ? 1 : 0) + (dy != 0 ? 1 : 0) + (dz != 0 ? 1 : 0);
                    if (axes == 0) {
                        if (!(state.getBlock() instanceof HellForgeCoreBlock)) {
                            return Result.fail(Component.translatable("message.merlins_inferno.hell_forge.need_core",
                                    pos.getX(), pos.getY(), pos.getZ()));
                        }
                    } else if (axes == 1) {
                        if (!state.is(ModBlocks.COMPRESSED_NETHERRACK.get()) && !state.is(ModBlocks.HELLGLASS.get())
                                && !(state.getBlock() instanceof HellForgeVaultBlock)) {
                            return Result.fail(Component.translatable("message.merlins_inferno.hell_forge.need_face",
                                    pos.getX(), pos.getY(), pos.getZ()));
                        }
                    } else if (!state.is(ModBlocks.COMPRESSED_NETHERRACK.get())) {
                        return Result.fail(Component.translatable("message.merlins_inferno.hell_forge.need_shell",
                                pos.getX(), pos.getY(), pos.getZ()));
                    }
                }
            }
        }
        BlockState core = level.getBlockState(centre);
        return Result.ok(((HellForgeCoreBlock) core.getBlock()).getTier());
    }
}
