package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
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
 * El'ysium's cloud sea as blocks: a thin layer the generator lays under everything, meshed by the terrain pipeline, so a
 * shader pack lights and shadows it like any other ground. Nothing can touch it: no collision, no outline, no drops, and
 * entities fall straight through. Without a shader pack it does not render at all and the drawn sheets of
 * {@code ElysiumCloudSea} show instead; the client flips {@link #setShown} when a pack starts or stops.
 */
public class CloudSeaBlock extends Block {
    public static final IntegerProperty TONE = IntegerProperty.create("tone", 0, 2);

    /** Client-side only in effect: nothing on the server asks for a block's render shape. */
    private static volatile boolean shown;

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
                .isViewBlocking((state, level, pos) -> false);
    }

    public static boolean isShown() {
        return shown;
    }

    public static void setShown(boolean value) {
        shown = value;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TONE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return shown ? RenderShape.MODEL : RenderShape.INVISIBLE;
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
