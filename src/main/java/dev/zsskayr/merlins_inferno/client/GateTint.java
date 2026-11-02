package dev.zsskayr.merlins_inferno.client;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.block.GateBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Dims the runes of a Gate as it fades: the overlay's faces carry tint index 0 and this hands them a grey that gets darker with
 * the stage of the gate's look, so the one rune strip serves every stage (the runes are translucent over the stone, and a darker
 * rune is a dimmer one). Solid is full white, the ghost stage has no rune quads at all.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class GateTint {
    /** The grey of each stage of the look, 0 (solid) to 4 (a ghost). */
    private static final int[] STAGE_TINT = {0xFFFFFF, 0xB3B3B3, 0x666666, 0x262626, 0x000000};
    /** The frame dims less, and keeps a faint glow on a ghost. */
    private static final int[] FRAME_TINT = {0xFFFFFF, 0xE0E0E0, 0xB8B8B8, 0x909090, 0x606060};

    private GateTint() {}

    @SubscribeEvent
    public static void onColors(RegisterColorHandlersEvent.Block event) {
        event.register((BlockState state, net.minecraft.world.level.BlockAndTintGetter level, net.minecraft.core.BlockPos pos, int index) ->
                        index == 0 ? STAGE_TINT[GateBlock.stage(state)] : index == 1 ? FRAME_TINT[GateBlock.stage(state)] : -1,
                ModBlocks.RUNIC_GATE.get(), ModBlocks.MUNDANE_GATE.get(), ModBlocks.DEMONIC_GATE.get(),
                ModBlocks.ANGEL_GATE.get(), ModBlocks.FAERIC_GATE.get());
    }
}
