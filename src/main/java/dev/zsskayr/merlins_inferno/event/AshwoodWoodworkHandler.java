package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * The two pieces of vanilla plumbing Ashwood's woodworking set needs that data alone can't do:
 * axes stripping its logs, and vanilla's sign block entity types accepting its sign blocks.
 */
public final class AshwoodWoodworkHandler {
    private AshwoodWoodworkHandler() {
    }

    /** Mod-bus: {@code BlockEntityType.SIGN}/{@code HANGING_SIGN} only accept the blocks they were built with. */
    @EventBusSubscriber(modid = Merlins_inferno.MODID)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        static void onAddBlockEntityBlocks(BlockEntityTypeAddBlocksEvent event) {
            event.modify(BlockEntityType.SIGN, ModBlocks.ASHWOOD_SIGN.get(), ModBlocks.ASHWOOD_WALL_SIGN.get());
            event.modify(BlockEntityType.HANGING_SIGN, ModBlocks.ASHWOOD_HANGING_SIGN.get(), ModBlocks.ASHWOOD_WALL_HANGING_SIGN.get());
        }
    }

    /** Game-bus: log -> stripped log, wood -> stripped wood, keeping the axis. */
    @EventBusSubscriber(modid = Merlins_inferno.MODID)
    public static final class GameBus {
        private GameBus() {
        }

        @SubscribeEvent
        static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
            if (event.getItemAbility() != ItemAbilities.AXE_STRIP) {
                return;
            }
            BlockState state = event.getState();
            if (state.is(ModBlocks.ASHWOOD_LOG.get())) {
                event.setFinalState(stripped(ModBlocks.STRIPPED_ASHWOOD_LOG.get(), state));
            } else if (state.is(ModBlocks.ASHWOOD_WOOD.get())) {
                event.setFinalState(stripped(ModBlocks.STRIPPED_ASHWOOD_WOOD.get(), state));
            }
        }

        private static BlockState stripped(RotatedPillarBlock target, BlockState original) {
            return target.defaultBlockState().setValue(RotatedPillarBlock.AXIS, original.getValue(RotatedPillarBlock.AXIS));
        }
    }
}
