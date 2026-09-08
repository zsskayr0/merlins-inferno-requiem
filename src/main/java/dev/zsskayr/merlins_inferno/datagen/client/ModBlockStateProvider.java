package dev.zsskayr.merlins_inferno.datagen.client;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * Blockstates + block/item models for every block this mod adds, except the two saplings
 * (their cross-shaped model is hand-written under {@code assets/.../models/block/*_sapling.json}
 * - there's no built-in helper for that shape here, and it's only two tiny files).
 */
public final class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Merlins_inferno.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Ashwood
        logBlock(ModBlocks.ASHWOOD_LOG.get());
        itemModels().simpleBlockItem(ModBlocks.ASHWOOD_LOG.get());
        axisBlock(ModBlocks.ASHWOOD_WOOD.get(), blockTexture(ModBlocks.ASHWOOD_LOG.get()), blockTexture(ModBlocks.ASHWOOD_LOG.get()));
        itemModels().simpleBlockItem(ModBlocks.ASHWOOD_WOOD.get());
        simpleBlock(ModBlocks.ASHWOOD_PLANKS.get());
        itemModels().simpleBlockItem(ModBlocks.ASHWOOD_PLANKS.get());
        simpleBlock(ModBlocks.ASHWOOD_LEAVES.get());
        itemModels().simpleBlockItem(ModBlocks.ASHWOOD_LEAVES.get());
        stairsBlock(ModBlocks.ASHWOOD_STAIRS.get(), blockTexture(ModBlocks.ASHWOOD_PLANKS.get()));
        itemModels().simpleBlockItem(ModBlocks.ASHWOOD_STAIRS.get());
        slabBlock(ModBlocks.ASHWOOD_SLAB.get(), blockTexture(ModBlocks.ASHWOOD_PLANKS.get()), blockTexture(ModBlocks.ASHWOOD_PLANKS.get()));
        itemModels().simpleBlockItem(ModBlocks.ASHWOOD_SLAB.get());
        fenceBlock(ModBlocks.ASHWOOD_FENCE.get(), blockTexture(ModBlocks.ASHWOOD_PLANKS.get()));
        // fenceBlock() only generates "<name>_post"/"<name>_side" models, no plain "<name>" one -
        // the post model is what vanilla fence items use as their icon too.
        itemModels().withExistingParent("ashwood_fence", modLoc("block/ashwood_fence_post"));

        // Rowanwood
        logBlock(ModBlocks.ROWANWOOD_LOG.get());
        itemModels().simpleBlockItem(ModBlocks.ROWANWOOD_LOG.get());
        simpleBlock(ModBlocks.ROWANWOOD_LEAVES.get());
        itemModels().simpleBlockItem(ModBlocks.ROWANWOOD_LEAVES.get());
    }
}
