package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.block.BenchBlock;
import dev.zsskayr.merlins_inferno.entity.BenchSeatEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import dev.zsskayr.merlins_inferno.worldgen.structure.SacredChurchPiece;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class BenchGameTests {
    @GameTest(template="bench_empty")
    public static void connectionsAndRemoval(GameTestHelper helper) {
        var level = helper.getLevel();
        int row = 2;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos center = helper.absolutePos(new BlockPos(4, 2, row));
            Direction side = facing.getClockWise();
            BlockPos a = center.relative(side.getOpposite()), c = center.relative(side);
            BlockState state = ModBlocks.BENCH.get().defaultBlockState().setValue(BenchBlock.FACING, facing);
            level.setBlock(center, state, 3);
            helper.assertTrue(!level.getBlockState(center).getValue(BenchBlock.LEFT), "Single bench has both ends");
            level.setBlock(a, state, 3); level.setBlock(c, state, 3);
            BlockState middle = level.getBlockState(center);
            helper.assertTrue(middle.getValue(BenchBlock.LEFT) && middle.getValue(BenchBlock.RIGHT), "Connections work in every direction");
            helper.assertTrue(level.getBlockState(a).getValue(BenchBlock.RIGHT), "First end connects");
            helper.assertTrue(level.getBlockState(c).getValue(BenchBlock.LEFT), "Last end connects");
            level.setBlock(c, state.setValue(BenchBlock.FACING, facing.getOpposite()), 3);
            helper.assertTrue(!level.getBlockState(center).getValue(BenchBlock.RIGHT), "Opposite-facing benches do not join");
            level.setBlock(a, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(!level.getBlockState(center).getValue(BenchBlock.LEFT), "Breaking restores the end");
            level.setBlock(center, Blocks.AIR.defaultBlockState(), 3); level.setBlock(c, Blocks.AIR.defaultBlockState(), 3);
            row += 2;
        }
        helper.succeed();
    }
    @GameTest(template="bench_empty")
    public static void seatingAndCleanup(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        var level = helper.getLevel();
        level.setBlock(pos, ModBlocks.BENCH.get().defaultBlockState(), 3);
        Player first = helper.makeMockPlayer(GameType.SURVIVAL);
        Player second = helper.makeMockPlayer(GameType.SURVIVAL);
        first.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(ModBlocks.BENCH.get()));
        var result = level.getBlockState(pos).useItemOn(first.getMainHandItem(), level, first,
                net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false));
        helper.assertTrue(result == net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,
                "Holding bench allows extending the row instead of sitting");

        BenchSeatEntity.sit(level, pos, Direction.NORTH, first);
        helper.assertTrue(first.getVehicle() instanceof BenchSeatEntity, "Player sits on the bench");
        var seat = first.getVehicle();
        helper.assertTrue(Math.abs(seat.getY() - pos.getY() - BenchBlock.SEAT_HEIGHT) < 0.00001,
                "Rider mount matches the raised seat surface");
        BenchSeatEntity.sit(level, pos, Direction.NORTH, second);
        helper.assertTrue(!second.isPassenger(), "Occupied seat rejects second player");
        first.stopRiding(); seat.tick();
        helper.assertTrue(seat.isRemoved(), "Dismount removes unused seat");
        BenchSeatEntity.sit(level, pos, Direction.NORTH, second);
        helper.assertTrue(second.isPassenger(), "Vacated seat can be reused");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(!second.isPassenger(), "Breaking bench dismounts rider");
        helper.succeed();
    }
    @GameTest(template="bench_empty")
    public static void churchPewsAreBenches(GameTestHelper helper) {
        var template = helper.getLevel().getStructureManager().get(SacredChurchPiece.TEMPLATE).orElseThrow();
        var settings = new StructurePlaceSettings();
        var benches = template.filterBlocks(BlockPos.ZERO, settings, ModBlocks.BENCH.get());
        helper.assertTrue(benches.size() == 80, "Church template holds 80 pew benches, found " + benches.size());
        var positions = benches.stream().map(info -> info.pos()).collect(java.util.stream.Collectors.toSet());
        for (var info : benches) {
            BlockPos p = info.pos();
            helper.assertTrue(info.state().getValue(BenchBlock.FACING) == Direction.EAST, "Pew faces the altar");
            helper.assertTrue(info.state().getValue(BenchBlock.LEFT) == positions.contains(p.north())
                    && info.state().getValue(BenchBlock.RIGHT) == positions.contains(p.south()), "Pew ends match its row at " + p);
        }
        helper.assertTrue(template.filterBlocks(BlockPos.ZERO, settings, Blocks.SPRUCE_STAIRS).stream()
                .noneMatch(info -> info.pos().getY() == 2 && info.state().getValue(StairBlock.FACING) == Direction.WEST
                        && info.pos().getX() >= 14 && info.pos().getX() <= 36), "No pew stairs are left");
        helper.succeed();
    }
    @GameTest(template="bench_empty")
    public static void waterAndMirror(GameTestHelper helper) {
        BlockState state = ModBlocks.BENCH.get().defaultBlockState().setValue(BenchBlock.WATERLOGGED,true)
                .setValue(BenchBlock.LEFT,true);
        helper.assertTrue(state.getFluidState().getType()==Fluids.WATER, "Waterlogged bench retains water");
        BlockState mirrored = state.mirror(Mirror.FRONT_BACK);
        helper.assertTrue(!mirrored.getValue(BenchBlock.LEFT) && mirrored.getValue(BenchBlock.RIGHT), "Mirror swaps connected ends");
        helper.succeed();
    }
}
