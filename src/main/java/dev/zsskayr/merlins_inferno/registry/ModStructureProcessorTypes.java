package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.worldgen.feature.BlockPaletteSwapProcessor;

/** Custom {@code StructureProcessorType}s this mod adds. */
public final class ModStructureProcessorTypes {
    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSOR_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, Merlins_inferno.MODID);

    // Remaps blocks in a pasted structure (e.g. vanilla spruce_log -> ashwood_log) while keeping
    // matching block-state properties (axis, distance, persistent, ...) - see BlockPaletteSwapProcessor.
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<BlockPaletteSwapProcessor>> BLOCK_PALETTE_SWAP =
            STRUCTURE_PROCESSOR_TYPES.register("block_palette_swap", () -> () -> BlockPaletteSwapProcessor.CODEC);

    private ModStructureProcessorTypes() {
    }

    public static void register(IEventBus modEventBus) {
        STRUCTURE_PROCESSOR_TYPES.register(modEventBus);
    }
}
