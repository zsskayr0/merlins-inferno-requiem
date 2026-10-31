package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * El'ysium's cloud sea: a thin layer of blocks the generator lays under everything, meshed by the terrain pipeline, so it
 * looks the same to shader packs and map mods as any other ground. Nothing can touch it: no collision, no outline, no
 * drops, and entities fall straight through.
 *
 * <p>Sky light cannot reach under a big island, which would leave a black patch on the clouds there, so without a shader
 * pack the blocks render full bright ({@link #setBright}).
 */
public class CloudSeaBlock extends Block {
    public static final IntegerProperty TONE = IntegerProperty.create("tone", 0, 2);

    /** Client-side only in effect: nothing on the server asks how a block renders. */
    private static volatile boolean bright = true;

    public CloudSeaBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TONE, 1));
    }

    public static BlockBehaviour.Properties blockProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.SNOW)
                .sound(SoundType.WOOL)
                .strength(-1.0F, 3600000.0F)
                .noCollission()
                .noLootTable()
                .pushReaction(PushReaction.BLOCK)
                .isValidSpawn((state, level, pos, type) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .emissiveRendering((state, level, pos) -> bright);
    }

    public static boolean isBright() {
        return bright;
    }

    public static void setBright(boolean value) {
        bright = value;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TONE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** Faces between two cloud blocks are never seen, whatever their tones. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return adjacent.is(this);
    }
}
