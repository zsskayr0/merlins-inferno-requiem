package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import dev.zsskayr.merlins_inferno.blockentity.HellForgeVaultHandler;

/**
 * An automation port for the Hell Forge. It has no GUI and does nothing when clicked; its
 * {@link #MODE} (set with the Forge Wrench, shown by its texture) decides which slot of the
 * forge a hopper or pipe touching it can reach. With {@link #AUTO} on (Wrench, sneaking) it also
 * pushes the forge's result into, or pulls input/fuel from, an inventory next to it. The item handler itself is registered in
 * {@link dev.zsskayr.merlins_inferno.Merlins_inferno}'s capability event and only works while
 * the structure is formed.
 */
public class HellForgeVaultBlock extends Block {
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);
    /** When on, the vault itself moves items between the forge and an adjacent inventory (no hopper needed). */
    public static final BooleanProperty AUTO = BooleanProperty.create("auto");

    /** Ticks between automatic transfers while {@link #AUTO} is on - same pace as a hopper. */
    public static final int AUTO_INTERVAL = 8;

    public HellForgeVaultBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MODE, Mode.INPUT).setValue(AUTO, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE, AUTO);
    }

    /** Runs only while {@link #AUTO} is on: one transfer, then re-arms itself. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(AUTO)) {
            return;
        }
        HellForgeVaultHandler.autoTransfer(level, pos, state);
        level.scheduleTick(pos, this, AUTO_INTERVAL);
    }

    public enum Mode implements StringRepresentable {
        /** Accepts the item to cook. */
        INPUT("input"),
        /** Gives out the cooked result. */
        OUTPUT("output"),
        /** Accepts fuel for the tank. */
        FUEL("fuel");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public Mode next() {
            Mode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public Component displayName() {
            return Component.translatable("message.merlins_inferno.hell_forge.vault_mode." + this.name);
        }
    }
}
