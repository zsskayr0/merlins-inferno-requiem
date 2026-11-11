package dev.zsskayr.merlins_inferno.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import dev.zsskayr.merlins_inferno.multiblock.HellForgeStructure.Layout;

/**
 * The Hell Forge's centre block. It defines the forge's tier and structure layout. The
 * Demonic Core (tier 1) is the first and covers every Circle 1 recipe. {@link #ACTIVE} only drives
 * its animated texture and is set by the Controller while the forge cooks.
 */
public class HellForgeCoreBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final int tier;
    private final Layout maxLayout;

    public HellForgeCoreBlock(Properties properties, int tier, Layout maxLayout) {
        super(properties);
        this.tier = tier;
        this.maxLayout = maxLayout;
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    public int getTier() {
        return this.tier;
    }

    public dev.zsskayr.merlins_inferno.forge.ForgeThermalData.Core thermalProfile(net.minecraft.world.level.Level level) {
        return dev.zsskayr.merlins_inferno.forge.ForgeThermalData.core(level, this);
    }

    public boolean supports(Layout layout) {
        return layout.radius() <= this.maxLayout.radius();
    }
}
