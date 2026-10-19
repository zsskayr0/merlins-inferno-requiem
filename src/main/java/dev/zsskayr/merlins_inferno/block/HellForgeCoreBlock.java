package dev.zsskayr.merlins_inferno.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The Hell Forge's centre block. It is inert on its own (no GUI, no interaction): its only job is
 * to set the forge's {@link #getTier tier}, so a stronger Core later means a stronger forge. The
 * Demonic Core (tier 1) is the first and covers every Circle 1 recipe. {@link #ACTIVE} only drives
 * its animated texture and is set by the Controller while the forge cooks.
 */
public class HellForgeCoreBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final int tier;

    public HellForgeCoreBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    public int getTier() {
        return this.tier;
    }
}
