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
    /** Burning a soul fuel: the glow turns blue. */
    public static final BooleanProperty SOUL = BooleanProperty.create("soul");

    private final int tier;
    private final Layout maxLayout;

    public HellForgeCoreBlock(Properties properties, int tier, Layout maxLayout) {
        super(properties);
        this.tier = tier;
        this.maxLayout = maxLayout;
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false).setValue(SOUL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, SOUL);
    }

    /**
     * A Magmatic Core breeds lava instead of smelting: it takes either shell (Compressed Netherrack or Blightsteel), and the
     * shell sets what it can make. See {@code HellForgeStructure#validate}.
     */
    public boolean magmatic() {
        return false;
    }

    public int getTier() {
        return this.tier;
    }

    public dev.zsskayr.merlins_inferno.forge.ForgeThermalData.Core thermalProfile(net.minecraft.world.level.Level level) {
        return dev.zsskayr.merlins_inferno.forge.ForgeThermalData.core(level, this);
    }

    /** The shell this forge is built from: Compressed Netherrack for the Demonic Core, Blightsteel for the Blighted Core. */
    public boolean isShell(BlockState state) {
        return state.is(this.shellBlock());
    }

    /** The controller this core answers to: the Netherrack Hellforge Controller, or the Blighted one. */
    public Block controllerBlock() {
        return this.tier >= 2 ? dev.zsskayr.merlins_inferno.registry.ModBlocks.BLIGHTED_HELLFORGE_CONTROLLER.get()
                : dev.zsskayr.merlins_inferno.registry.ModBlocks.HELL_FORGE_CONTROLLER.get();
    }

    public Block shellBlock() {
        return this.tier >= 2 ? dev.zsskayr.merlins_inferno.registry.ModBlocks.BLIGHTSTEEL_CASING.get()
                : dev.zsskayr.merlins_inferno.registry.ModBlocks.COMPRESSED_NETHERRACK.get();
    }

    /**
     * Which catalyst an interior block counts as for this core: 0 speed, 1 yield (Demonic); 2 stabilizer, 3 bellows,
     * 4 economizer, 5 ward (Blighted); -1 not a catalyst; -2 a catalyst of the other core.
     */
    public int catalystKind(BlockState state) {
        int kind = catalystOf(state);
        if (kind < 0) return -1;
        return (this.tier >= 2) == (kind >= 2) ? kind : -2;
    }

    public static int catalystOf(BlockState state) {
        if (state.is(dev.zsskayr.merlins_inferno.registry.ModTags.Blocks.HELL_FORGE_SPEED_CATALYSTS)) return 0;
        if (state.is(dev.zsskayr.merlins_inferno.registry.ModTags.Blocks.HELL_FORGE_YIELD_CATALYSTS)) return 1;
        if (state.is(dev.zsskayr.merlins_inferno.registry.ModTags.Blocks.BLIGHTED_STABILIZERS)) return 2;
        if (state.is(dev.zsskayr.merlins_inferno.registry.ModTags.Blocks.BLIGHTED_BELLOWS)) return 3;
        if (state.is(dev.zsskayr.merlins_inferno.registry.ModTags.Blocks.BLIGHTED_ECONOMIZERS)) return 4;
        if (state.is(dev.zsskayr.merlins_inferno.registry.ModTags.Blocks.BLIGHTED_WARDS)) return 5;
        return -1;
    }

    public boolean supports(Layout layout) {
        return layout.radius() <= this.maxLayout.radius();
    }
}
