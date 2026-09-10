package dev.zsskayr.merlins_inferno.worldgen.feature;

import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import dev.zsskayr.merlins_inferno.registry.ModStructureProcessorTypes;

/**
 * Swaps blocks in a pasted structure by exact {@link Block} match (e.g. every
 * {@code minecraft:spruce_log} becomes {@code merlins_inferno:ashwood_log}), carrying over any
 * block-state property the replacement shares with the original (axis, distance, persistent, ...).
 * <p>
 * This is how the Ashwood tree structures (ported from a friend's spruce-based tree pack, see
 * {@code ModTreeProvider}) get their own wood/leaves without needing 36 hand-edited NBT files -
 * the source files are untouched vanilla spruce_log/spruce_wood/spruce_leaves, and this processor
 * retextures them at paste time. Works because Ashwood's blocks use the exact same property
 * objects as their vanilla counterparts (RotatedPillarBlock's AXIS, LeavesBlock's DISTANCE/
 * PERSISTENT), so a plain identity match ({@link BlockState#hasProperty}) is all copying needs.
 */
public class BlockPaletteSwapProcessor extends StructureProcessor {
    public static final MapCodec<BlockPaletteSwapProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.unboundedMap(BuiltInRegistries.BLOCK.byNameCodec(), BuiltInRegistries.BLOCK.byNameCodec())
                    .fieldOf("swaps").forGetter(processor -> processor.swaps)
    ).apply(instance, BlockPaletteSwapProcessor::new));

    private final Map<Block, Block> swaps;

    public BlockPaletteSwapProcessor(Map<Block, Block> swaps) {
        this.swaps = swaps;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
            StructureTemplate.StructureBlockInfo blockInfo, StructureTemplate.StructureBlockInfo relativeBlockInfo, StructurePlaceSettings settings) {
        BlockState oldState = relativeBlockInfo.state();
        Block replacement = swaps.get(oldState.getBlock());
        if (replacement == null) {
            return relativeBlockInfo;
        }

        BlockState newState = replacement.defaultBlockState();
        for (Property<?> property : oldState.getProperties()) {
            if (newState.hasProperty(property)) {
                newState = copyProperty(newState, oldState, property);
            }
        }
        return new StructureTemplate.StructureBlockInfo(relativeBlockInfo.pos(), newState, relativeBlockInfo.nbt());
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState target, BlockState source, Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructureProcessorTypes.BLOCK_PALETTE_SWAP.get();
    }
}
