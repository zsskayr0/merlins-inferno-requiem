package dev.zsskayr.merlins_inferno.block;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Ebony Debris. Placed by hand it takes on the look of the rock it is surrounded by (basalt, blackstone, netherrack,
 * flesh...), and world generation sets the host to the rock it replaces; with no known rock around it keeps its own
 * complete texture, like Ancient Debris. Blocks saved before this property existed load as {@link Host#NONE}.
 */
public class EbonyDebrisBlock extends Block {
    public static final EnumProperty<Host> HOST = EnumProperty.create("host", Host.class);

    public enum Host implements StringRepresentable {
        NONE("none"), NETHERRACK("netherrack"), BASALT("basalt"), BLACKSTONE("blackstone"),
        FLESH("flesh"), POROUS_FLESH("porous_flesh"), CHARRED_FLESH("charred_flesh");

        private final String id;
        Host(String id) { this.id = id; }
        @Override public String getSerializedName() { return id; }
    }

    public EbonyDebrisBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HOST, Host.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HOST);
    }

    private static Host hostOf(BlockState state) {
        if (state.is(Blocks.NETHERRACK)) return Host.NETHERRACK;
        if (state.is(Blocks.BASALT)) return Host.BASALT;
        if (state.is(Blocks.BLACKSTONE)) return Host.BLACKSTONE;
        if (state.is(ModBlocks.FLESH_BLOCK.get())) return Host.FLESH;
        if (state.is(ModBlocks.POROUS_FLESH.get())) return Host.POROUS_FLESH;
        if (state.is(ModBlocks.CHARRED_FLESH.get())) return Host.CHARRED_FLESH;
        return Host.NONE;
    }

    /** The rock that makes up most of the six neighbours (the first one met wins a tie), or NONE. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Map<Host, Integer> count = new EnumMap<>(Host.class);
        Host best = Host.NONE;
        int bestCount = 0;
        for (Direction dir : Direction.values()) {
            Host host = hostOf(level.getBlockState(pos.relative(dir)));
            if (host == Host.NONE) continue;
            int n = count.merge(host, 1, Integer::sum);
            if (n > bestCount) {
                bestCount = n;
                best = host;
            }
        }
        return defaultBlockState().setValue(HOST, best);
    }
}
