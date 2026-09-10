package dev.zsskayr.merlins_inferno.datagen.loot;

import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * Block loot tables for every block this mod adds. Without one, breaking the block drops
 * nothing at all.
 * <p>
 * TODO(Toque do Druida): per the design doc (3.2), Rowanwood's log should only drop the raw
 * Rowanwood material when broken with an axe enchanted with "Toque do Druida" - otherwise it
 * should drop plain wood. That enchantment (and the raw-material item) don't exist yet, so for
 * now it's a plain {@link #dropSelf}, same as any other log. Revisit once both exist.
 */
public final class ModBlockLootProvider extends BlockLootSubProvider {
    private static final float[] LEAVES_SAPLING_CHANCES = {0.05F, 0.0625F, 0.083333336F, 0.1F};

    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.VANILLA_SET, registries);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        // The default is EVERY registered block (vanilla included), and generate() must supply a
        // loot table for every one of them - restrict it to just this mod's blocks.
        return List.of(
                ModBlocks.ASHWOOD_LOG.get(), ModBlocks.ASHWOOD_WOOD.get(), ModBlocks.ASHWOOD_PLANKS.get(),
                ModBlocks.ASHWOOD_LEAVES.get(), ModBlocks.ASHWOOD_SAPLING.get(), ModBlocks.ASHWOOD_STAIRS.get(),
                ModBlocks.ASHWOOD_SLAB.get(), ModBlocks.ASHWOOD_FENCE.get(),
                ModBlocks.ROWANWOOD_LOG.get(), ModBlocks.ROWANWOOD_LEAVES.get(),
                ModBlocks.COMPRESSED_NETHERRACK.get(), ModBlocks.HELL_FORGE.get());
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.ASHWOOD_LOG.get());
        dropSelf(ModBlocks.ASHWOOD_WOOD.get());
        dropSelf(ModBlocks.ASHWOOD_PLANKS.get());
        dropSelf(ModBlocks.ASHWOOD_SAPLING.get());
        dropSelf(ModBlocks.ASHWOOD_STAIRS.get());
        dropSelf(ModBlocks.ASHWOOD_SLAB.get());
        dropSelf(ModBlocks.ASHWOOD_FENCE.get());
        add(ModBlocks.ASHWOOD_LEAVES.get(),
                createLeavesDrops(ModBlocks.ASHWOOD_LEAVES.get(), ModBlocks.ASHWOOD_SAPLING.get(), LEAVES_SAPLING_CHANCES));

        dropSelf(ModBlocks.ROWANWOOD_LOG.get()); // see class javadoc - Toque do Druida gating comes later
        // No sapling on purpose: Rowanwood's shape is a 48x25x39 hand-placed structure paste (see
        // ModTreeProvider), not something a bonemealed sapling could reasonably grow - letting
        // players farm the landmark tree would be wildly overpowered. Leaves just drop nothing
        // without Silk Touch/Shears (no sapling item to drop instead).
        add(ModBlocks.ROWANWOOD_LEAVES.get(), createSilkTouchOnlyTable(ModBlocks.ROWANWOOD_LEAVES.get()));

        dropSelf(ModBlocks.COMPRESSED_NETHERRACK.get());
        dropSelf(ModBlocks.HELL_FORGE.get());
    }
}
