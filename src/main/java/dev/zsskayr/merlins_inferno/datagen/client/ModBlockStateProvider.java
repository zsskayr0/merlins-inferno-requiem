package dev.zsskayr.merlins_inferno.datagen.client;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModItems;

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
        // fenceBlock() only generates "<name>_post"/"<name>_side" models - vanilla fences don't
        // use either of those as their item icon (that was the bug: a lone post looks wrong both
        // in the inventory and in hand). They use a dedicated "<name>_inventory" model built on
        // vanilla's own "block/fence_inventory" template (post + crossbars, like a fence actually
        // looks), which datagen has no built-in helper for - hence generating it by hand here.
        models().singleTexture("ashwood_fence_inventory", mcLoc("block/fence_inventory"), blockTexture(ModBlocks.ASHWOOD_PLANKS.get()));
        itemModels().withExistingParent("ashwood_fence", modLoc("block/ashwood_fence_inventory"));

        registerAshwoodWoodwork();

        // Rowanwood
        logBlock(ModBlocks.ROWANWOOD_LOG.get());
        itemModels().simpleBlockItem(ModBlocks.ROWANWOOD_LOG.get());
        simpleBlock(ModBlocks.ROWANWOOD_LEAVES.get());
        itemModels().simpleBlockItem(ModBlocks.ROWANWOOD_LEAVES.get());

    }

    /** Stripped variants, gate, door, trapdoor, plate, button and both signs (design doc, 3.1). */
    private void registerAshwoodWoodwork() {
        ResourceLocation planks = blockTexture(ModBlocks.ASHWOOD_PLANKS.get());

        logBlock(ModBlocks.STRIPPED_ASHWOOD_LOG.get());
        itemModels().simpleBlockItem(ModBlocks.STRIPPED_ASHWOOD_LOG.get());
        ResourceLocation strippedSide = blockTexture(ModBlocks.STRIPPED_ASHWOOD_LOG.get());
        axisBlock(ModBlocks.STRIPPED_ASHWOOD_WOOD.get(), strippedSide, strippedSide);
        itemModels().simpleBlockItem(ModBlocks.STRIPPED_ASHWOOD_WOOD.get());

        fenceGateBlock(ModBlocks.ASHWOOD_FENCE_GATE.get(), planks);
        itemModels().withExistingParent("ashwood_fence_gate", modLoc("block/ashwood_fence_gate"));

        doorBlockWithRenderType(ModBlocks.ASHWOOD_DOOR.get(), modLoc("block/ashwood_door_bottom"), modLoc("block/ashwood_door_top"), "cutout");
        itemModels().basicItem(ModItems.ASHWOOD_DOOR_ITEM.get());

        trapdoorBlockWithRenderType(ModBlocks.ASHWOOD_TRAPDOOR.get(), modLoc("block/ashwood_trapdoor"), true, "cutout");
        itemModels().withExistingParent("ashwood_trapdoor", modLoc("block/ashwood_trapdoor_bottom"));

        pressurePlateBlock(ModBlocks.ASHWOOD_PRESSURE_PLATE.get(), planks);
        itemModels().withExistingParent("ashwood_pressure_plate", modLoc("block/ashwood_pressure_plate"));

        buttonBlock(ModBlocks.ASHWOOD_BUTTON.get(), planks);
        models().singleTexture("ashwood_button_inventory", mcLoc("block/button_inventory"), planks);
        itemModels().withExistingParent("ashwood_button", modLoc("block/ashwood_button_inventory"));

        // Signs render through a block entity renderer - the block models only carry the particle texture.
        signBlock(ModBlocks.ASHWOOD_SIGN.get(), ModBlocks.ASHWOOD_WALL_SIGN.get(), models().sign("ashwood_sign", planks));
        itemModels().basicItem(ModItems.ASHWOOD_SIGN_ITEM.get());

        ModelFile hangingSign = models().getBuilder("ashwood_hanging_sign").texture("particle", strippedSide);
        anyState(ModBlocks.ASHWOOD_HANGING_SIGN.get(), hangingSign);
        anyState(ModBlocks.ASHWOOD_WALL_HANGING_SIGN.get(), hangingSign);
        itemModels().basicItem(ModItems.ASHWOOD_HANGING_SIGN_ITEM.get());
    }

    private void anyState(Block block, ModelFile model) {
        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());
    }
}
