package dev.zsskayr.merlins_inferno.datagen.client;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
        registerLyrium();
        registerSacredChurch();
        registerCompressedNetherrack();
        registerBlightstone();
        registerArganiteAndPorcelain();
        registerGraceFire();
        registerFleshAndNetherFlora();

        // Rowanwood
        logBlock(ModBlocks.ROWANWOOD_LOG.get());
        itemModels().simpleBlockItem(ModBlocks.ROWANWOOD_LOG.get());
        simpleBlock(ModBlocks.ROWANWOOD_LEAVES.get());
        itemModels().simpleBlockItem(ModBlocks.ROWANWOOD_LEAVES.get());

        simpleBlock(ModBlocks.CORRUPTED_OBSIDIAN.get());
        itemModels().simpleBlockItem(ModBlocks.CORRUPTED_OBSIDIAN.get());

        simpleBlock(ModBlocks.PETRIFIED_SKULL.get());
        itemModels().simpleBlockItem(ModBlocks.PETRIFIED_SKULL.get());

        simpleBlock(ModBlocks.WITHERED_OBSIDIAN.get());
        itemModels().simpleBlockItem(ModBlocks.WITHERED_OBSIDIAN.get());
        // Purified Obsidian borrows the Withered Obsidian texture until it gets its own
        ModelFile purifiedObsidian = models().cubeAll("purified_obsidian", modLoc("block/withered_obsidian"));
        simpleBlock(ModBlocks.PURIFIED_OBSIDIAN.get(), purifiedObsidian);
        itemModels().simpleBlockItem(ModBlocks.PURIFIED_OBSIDIAN.get());
    }

    /**
     * The set around Compressed Netherrack. The plain block's blockstate/model are hand-written
     * (its texture is named {@code netherrack_compressed}), so the double slab reuses that model
     * and every other model points at the same texture.
     */
    private void registerCompressedNetherrack() {
        ResourceLocation texture = modLoc("block/netherrack_compressed");
        ModelFile full = models().getExistingFile(modLoc("block/compressed_netherrack"));

        stairsBlock(ModBlocks.COMPRESSED_NETHERRACK_STAIRS.get(), texture);
        itemModels().simpleBlockItem(ModBlocks.COMPRESSED_NETHERRACK_STAIRS.get());
        slabBlock(ModBlocks.COMPRESSED_NETHERRACK_SLAB.get(), models().slab("compressed_netherrack_slab", texture, texture, texture),
                models().slabTop("compressed_netherrack_slab_top", texture, texture, texture), full);
        itemModels().simpleBlockItem(ModBlocks.COMPRESSED_NETHERRACK_SLAB.get());
        wallBlock(ModBlocks.COMPRESSED_NETHERRACK_WALL.get(), texture);
        models().singleTexture("compressed_netherrack_wall_inventory", mcLoc("block/wall_inventory"), "wall", texture);
        itemModels().withExistingParent("compressed_netherrack_wall", modLoc("block/compressed_netherrack_wall_inventory"));
    }

    /** Blightstone: plain and brick forms get the full set, cracked and chiseled are plain cubes. */
    private void registerBlightstone() {
        registerStoneSet(ModBlocks.BLIGHTSTONE.get(), ModBlocks.BLIGHTSTONE_STAIRS.get(), ModBlocks.BLIGHTSTONE_SLAB.get(),
                ModBlocks.BLIGHTSTONE_WALL.get(), "blightstone");
        registerStoneSet(ModBlocks.BLIGHTSTONE_BRICKS.get(), ModBlocks.BLIGHTSTONE_BRICK_STAIRS.get(),
                ModBlocks.BLIGHTSTONE_BRICK_SLAB.get(), ModBlocks.BLIGHTSTONE_BRICK_WALL.get(), "blightstone_bricks");
        registerStoneSet(ModBlocks.POLISHED_BLIGHTSTONE.get(), ModBlocks.POLISHED_BLIGHTSTONE_STAIRS.get(),
                ModBlocks.POLISHED_BLIGHTSTONE_SLAB.get(), ModBlocks.POLISHED_BLIGHTSTONE_WALL.get(), "polished_blightstone");
        simpleBlock(ModBlocks.CHISELED_POLISHED_BLIGHTSTONE.get());
        itemModels().simpleBlockItem(ModBlocks.CHISELED_POLISHED_BLIGHTSTONE.get());
        registerStoneSet(ModBlocks.BLIGHTSTONE_SMALL_BRICKS.get(), ModBlocks.BLIGHTSTONE_SMALL_BRICK_STAIRS.get(),
                ModBlocks.BLIGHTSTONE_SMALL_BRICK_SLAB.get(), ModBlocks.BLIGHTSTONE_SMALL_BRICK_WALL.get(), "blightstone_small_bricks");
        simpleBlock(ModBlocks.CRACKED_BLIGHTSTONE_BRICKS.get());
        itemModels().simpleBlockItem(ModBlocks.CRACKED_BLIGHTSTONE_BRICKS.get());
    }

    /** Grace Fire: vanilla's fire shapes (floor, side and alternate side flames) in white, two animated textures chosen at random. */
    private void registerGraceFire() {
        var part = getMultipartBuilder(ModBlocks.GRACE_FIRE.get());
        for (String shape : new String[] {"template_fire_floor", "template_fire_side", "template_fire_side_alt"}) {
            String name = shape.replace("template_fire_", "grace_fire_");
            ModelFile[] variants = new ModelFile[2];
            for (int i = 0; i < 2; i++) {
                variants[i] = models().withExistingParent(name + i, mcLoc("block/" + shape))
                        .texture("fire", modLoc("block/grace_fire_" + i)).renderType("cutout");
            }
            int[] turns = shape.equals("template_fire_floor") ? new int[] {0} : new int[] {0, 90, 180, 270};
            for (int turn : turns) {
                part.part().modelFile(variants[0]).rotationY(turn).weight(1).nextModel().modelFile(variants[1]).rotationY(turn).weight(1).addModel().end();
            }
        }
    }

    /** Arganite and Quartzite as plain stone, Porcelain with three weighted variants, Polished Porcelain (connected at bake time) and Tiled Porcelain. */
    private void registerArganiteAndPorcelain() {
        registerStoneSet(ModBlocks.ARGANITE.get(), ModBlocks.ARGANITE_STAIRS.get(), ModBlocks.ARGANITE_SLAB.get(),
                ModBlocks.ARGANITE_WALL.get(), "arganite");
        registerStoneSet(ModBlocks.ARGANITE_BRICKS.get(), ModBlocks.ARGANITE_BRICKS_STAIRS.get(),
                ModBlocks.ARGANITE_BRICKS_SLAB.get(), ModBlocks.ARGANITE_BRICKS_WALL.get(), "arganite_bricks");
        simpleBlock(ModBlocks.QUARTZITE.get());
        itemModels().simpleBlockItem(ModBlocks.QUARTZITE.get());
        simpleBlock(ModBlocks.TILED_PORCELAIN.get());
        itemModels().simpleBlockItem(ModBlocks.TILED_PORCELAIN.get());

        // Porcelain: three equally weighted slabs, no rotation, so a big floor never repeats visibly. The item uses the first.
        ModelFile[] variants = new ModelFile[3];
        for (int i = 0; i < 3; i++) {
            String name = i == 0 ? "porcelain" : "porcelain_" + i;
            variants[i] = models().cubeAll(name, modLoc("block/" + name));
        }
        getVariantBuilder(ModBlocks.PORCELAIN.get()).partialState().setModels(
                new ConfiguredModel(variants[0]), new ConfiguredModel(variants[1]), new ConfiguredModel(variants[2]));
        itemModels().withExistingParent("porcelain", modLoc("block/porcelain"));
        registerStairsAndSlab(ModBlocks.PORCELAIN.get(), ModBlocks.PORCELAIN_STAIRS.get(), ModBlocks.PORCELAIN_SLAB.get(),
                variants[0], "porcelain");

        ResourceLocation polished = modLoc("block/polished_porcelain");
        ModelFile polishedCube = models().cubeAll("polished_porcelain", polished);
        simpleBlock(ModBlocks.POLISHED_PORCELAIN.get(), polishedCube);
        itemModels().simpleBlockItem(ModBlocks.POLISHED_PORCELAIN.get());
        registerStairsAndSlab(ModBlocks.POLISHED_PORCELAIN.get(), ModBlocks.POLISHED_PORCELAIN_STAIRS.get(),
                ModBlocks.POLISHED_PORCELAIN_SLAB.get(), polishedCube, "polished_porcelain", "polished_porcelain_step");

        ResourceLocation clusterTexture = modLoc("block/quartz_cluster");
        directionalBlock(ModBlocks.QUARTZ_CLUSTER.get(), models().cross("quartz_cluster", clusterTexture).renderType("cutout"));
        itemModels().withExistingParent("quartz_cluster", "item/generated").texture("layer0", clusterTexture);
    }

    private void registerStairsAndSlab(Block full, net.minecraft.world.level.block.StairBlock stairs,
                                       net.minecraft.world.level.block.SlabBlock slab, ModelFile fullModel, String textureName) {
        registerStairsAndSlab(full, stairs, slab, fullModel, textureName, textureName);
    }

    /** The stairs and slab wear {@code partTexture}: a block with a framed tile would have it cut across their partial faces. */
    private void registerStairsAndSlab(Block full, net.minecraft.world.level.block.StairBlock stairs,
                                       net.minecraft.world.level.block.SlabBlock slab, ModelFile fullModel, String textureName,
                                       String partTexture) {
        ResourceLocation texture = modLoc("block/" + partTexture);
        stairsBlock(stairs, texture);
        itemModels().simpleBlockItem(stairs);
        slabBlock(slab, models().slab(textureName + "_slab", texture, texture, texture),
                models().slabTop(textureName + "_slab_top", texture, texture, texture), fullModel);
        itemModels().simpleBlockItem(slab);
    }

    private void registerStoneSet(Block full, net.minecraft.world.level.block.StairBlock stairs,
                                  net.minecraft.world.level.block.SlabBlock slab,
                                  net.minecraft.world.level.block.WallBlock wall, String textureName) {
        ResourceLocation texture = modLoc("block/" + textureName);
        String setName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(slab).getPath().replace("_slab", "");
        ModelFile cube = models().cubeAll(textureName, texture);
        simpleBlock(full, cube);
        itemModels().simpleBlockItem(full);
        stairsBlock(stairs, texture);
        itemModels().simpleBlockItem(stairs);
        slabBlock(slab, models().slab(setName + "_slab", texture, texture, texture),
                models().slabTop(setName + "_slab_top", texture, texture, texture), cube);
        itemModels().simpleBlockItem(slab);
        wallBlock(wall, texture);
        models().singleTexture(setName + "_wall_inventory", mcLoc("block/wall_inventory"), "wall", texture);
        itemModels().withExistingParent(setName + "_wall", modLoc("block/" + setName + "_wall_inventory"));
    }

    /** Mouth block: flesh cube with an idle maw on top; the floor of the open maw sinks in 2 px steps, with the mob. */
    private void registerMouthBlock(ResourceLocation flesh) {
        ResourceLocation maw = modLoc("block/mouth_block_open");
        ModelFile[] models = new ModelFile[dev.zsskayr.merlins_inferno.block.MouthBlock.MAX_DEPTH + 1];
        models[0] = models().cubeBottomTop("mouth_block", flesh, flesh, modLoc("block/mouth_block_closed"));
        for (int d = 1; d < models.length; d++) {
            int floor = 16 - 2 * d;
            var m = models().getBuilder("mouth_block_open_" + d)
                    .texture("flesh", flesh).texture("maw", maw).texture("particle", flesh);
            m.element().from(0, 0, 0).to(16, floor, 16)
                    .allFaces((dir, face) -> face.texture(dir == net.minecraft.core.Direction.UP ? "#maw" : "#flesh")).end();
            m.element().from(0, floor, 0).to(2, 16, 16).allFaces((dir, face) -> face.texture("#flesh")).end();
            m.element().from(14, floor, 0).to(16, 16, 16).allFaces((dir, face) -> face.texture("#flesh")).end();
            m.element().from(2, floor, 0).to(14, 16, 2).allFaces((dir, face) -> face.texture("#flesh")).end();
            m.element().from(2, floor, 14).to(14, 16, 16).allFaces((dir, face) -> face.texture("#flesh")).end();
            models[d] = m;
        }
        ModelFile sated = models().cubeBottomTop("mouth_block_full", flesh, flesh, modLoc("block/mouth_block_full"));
        getVariantBuilder(ModBlocks.MOUTH_BLOCK.get()).forAllStatesExcept(state -> {
            int yRot = ((int) state.getValue(dev.zsskayr.merlins_inferno.block.MouthBlock.FACING).toYRot() + 180) % 360;
            return ConfiguredModel.builder()
                    .modelFile(state.getValue(dev.zsskayr.merlins_inferno.block.MouthBlock.FULL) ? sated
                            : models[state.getValue(dev.zsskayr.merlins_inferno.block.MouthBlock.DEPTH)])
                    .rotationY(yRot).build();
        }, dev.zsskayr.merlins_inferno.block.MouthBlock.CHEW);
        itemModels().withExistingParent("mouth_block", modLoc("block/mouth_block"));
    }

    /** Bracket fungus on haunted trunks: three caps stacked up the wall, two texture variants. */
    private void registerShelfShroom() {
        ModelFile[] models = new ModelFile[2];
        for (int v = 0; v < 2; v++) {
            var tex = modLoc("block/haunted_shelf_shroom_" + v);
            var m = models().getBuilder("haunted_shelf_shroom_" + v).renderType("cutout")
                    .texture("cap", tex).texture("particle", tex);
            float[][] caps = {{2, 2, 9, 9, 2.5f, 16}, {8, 6, 10, 15, 6.5f, 16}, {4, 10, 11, 9, 10.5f, 16}};
            for (float[] c : caps) {
                m.element().from(c[0], c[1], c[2]).to(c[3], c[4], c[5])
                        .allFaces((dir, face) -> face.uvs(7, 7, 14, 14).texture("#cap")).end();
            }
            models[v] = m;
        }
        itemModels().withExistingParent("haunted_shelf_shroom", mcLoc("item/generated"))
                .texture("layer0", modLoc("block/haunted_shelf_shroom_0"));
        getVariantBuilder(ModBlocks.HAUNTED_SHELF_SHROOM.get()).forAllStates(state -> {
            int yRot = ((int) state.getValue(dev.zsskayr.merlins_inferno.block.ShelfShroomBlock.FACING).toYRot() + 180) % 360;
            return new ConfiguredModel[] {
                    new ConfiguredModel(models[0], 0, yRot, false, 1),
                    new ConfiguredModel(models[1], 0, yRot, false, 1)};
        });
    }

    /** Flesh vein: one thin plane per face, like glow lichen. */
    private void registerFleshVein() {
        var plane = models().getBuilder("viscera").renderType("cutout")
                .texture("vein", modLoc("block/viscera")).texture("particle", modLoc("block/viscera"));
        plane.element().from(0, 0, 15.9f).to(16, 16, 15.9f)
                .face(net.minecraft.core.Direction.NORTH).uvs(0, 0, 16, 16).texture("#vein").end()
                .face(net.minecraft.core.Direction.SOUTH).uvs(0, 0, 16, 16).texture("#vein").end().end();
        var mp = getMultipartBuilder(ModBlocks.VISCERA.get());
        record Side(net.minecraft.world.level.block.state.properties.BooleanProperty p, int x, int y) {}
        for (Side s : new Side[]{
                new Side(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(net.minecraft.core.Direction.SOUTH), 0, 0),
                new Side(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(net.minecraft.core.Direction.WEST), 0, 90),
                new Side(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(net.minecraft.core.Direction.NORTH), 0, 180),
                new Side(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(net.minecraft.core.Direction.EAST), 0, 270),
                new Side(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(net.minecraft.core.Direction.UP), 90, 0),
                new Side(net.minecraft.world.level.block.MultifaceBlock.getFaceProperty(net.minecraft.core.Direction.DOWN), 270, 0)}) {
            mp.part().modelFile(plane).rotationX(s.x()).rotationY(s.y()).uvLock(true).addModel().condition(s.p(), true).end();
        }
        itemModels().withExistingParent("viscera", mcLoc("item/generated")).texture("layer0", modLoc("block/viscera"));
    }

    /** Flesh (block/slab/carpet), the red chain, black nylium and the Nether plants (texture names differ from block names). */
    private void registerFleshAndNetherFlora() {
        ResourceLocation flesh = modLoc("block/flesh");
        ModelFile full = models().cubeAll("flesh_block", flesh);
        simpleBlock(ModBlocks.FLESH_BLOCK.get(), full);
        itemModels().simpleBlockItem(ModBlocks.FLESH_BLOCK.get());
        registerMouthBlock(flesh);
        registerFleshVein();
        registerShelfShroom();
        slabBlock(ModBlocks.FLESH_SLAB.get(), models().slab("flesh_slab", flesh, flesh, flesh),
                models().slabTop("flesh_slab_top", flesh, flesh, flesh), full);
        itemModels().simpleBlockItem(ModBlocks.FLESH_SLAB.get());
        simpleBlock(ModBlocks.FLESH_CARPET.get(), models().singleTexture("flesh_carpet", mcLoc("block/carpet"), "wool", flesh));
        itemModels().withExistingParent("flesh_carpet", modLoc("block/flesh_carpet"));

        // Hand-written model (models/block/red_chain.json), imported from the "Better Lanterns and Chains" pack.
        ModelFile chain = models().getExistingFile(modLoc("block/red_chain"));
        getVariantBuilder(ModBlocks.RED_CHAIN.get())
                .partialState().with(BlockStateProperties.AXIS, Direction.Axis.Y).modelForState().modelFile(chain).addModel()
                .partialState().with(BlockStateProperties.AXIS, Direction.Axis.X).modelForState().modelFile(chain).rotationX(90).rotationY(90).addModel()
                .partialState().with(BlockStateProperties.AXIS, Direction.Axis.Z).modelForState().modelFile(chain).rotationX(90).addModel();
        itemModels().basicItem(ModItems.RED_CHAIN_ITEM.get());

        ModelFile nylium = models().cubeBottomTop("black_nylium", modLoc("block/black_nylium_side"),
                mcLoc("block/netherrack"), modLoc("block/black_nylium_top"));
        simpleBlock(ModBlocks.BLACK_NYLIUM.get(), nylium);
        itemModels().simpleBlockItem(ModBlocks.BLACK_NYLIUM.get());
        ModelFile blightNylium = models().cubeBottomTop("blightstone_nylium", modLoc("block/blightstone_nylium_side"),
                modLoc("block/blightstone"), modLoc("block/black_nylium_top"));
        simpleBlock(ModBlocks.BLIGHTSTONE_NYLIUM.get(), blightNylium);
        itemModels().simpleBlockItem(ModBlocks.BLIGHTSTONE_NYLIUM.get());

        ModelFile lust = models().cross("lust_flower", modLoc("block/lust_rose")).renderType("cutout");
        simpleBlock(ModBlocks.LUST_FLOWER.get(), lust);
        itemModels().withExistingParent("lust_flower", "item/generated").texture("layer0", modLoc("block/lust_rose"));

        doublePlant(ModBlocks.TALL_CRIMSON_ROOTS.get(), "tall_crimson_roots");
        doublePlant(ModBlocks.TALL_BLACK_GRASS.get(), "tall_black_grass");
    }

    private void doublePlant(Block block, String name) {
        ModelFile bottom = models().cross(name + "_bottom", modLoc("block/" + name + "_bottom")).renderType("cutout");
        ModelFile top = models().cross(name + "_top", modLoc("block/" + name + "_top")).renderType("cutout");
        getVariantBuilder(block)
                .partialState().with(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER).modelForState().modelFile(bottom).addModel()
                .partialState().with(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER).modelForState().modelFile(top).addModel();
        itemModels().withExistingParent(name, "item/generated").texture("layer0", modLoc("block/" + name + "_top"));
    }

    /** Lining block and the directional crystal cluster. */
    private void registerLyrium() {
        simpleBlock(ModBlocks.LYRIUM_BLOCK.get());
        itemModels().simpleBlockItem(ModBlocks.LYRIUM_BLOCK.get());
        simpleBlock(ModBlocks.LUMINITE_BLOCK.get());
        itemModels().simpleBlockItem(ModBlocks.LUMINITE_BLOCK.get());

        // Same cross-shaped, cutout model + facing rotations as vanilla's amethyst cluster.
        ResourceLocation clusterTexture = modLoc("block/lyrium_cluster");
        ModelFile cluster = models().cross("lyrium_cluster", clusterTexture).renderType("cutout");
        directionalBlock(ModBlocks.LYRIUM_CLUSTER.get(), cluster);
        itemModels().withExistingParent("lyrium_cluster", "item/generated").texture("layer0", clusterTexture);
    }

    /** The Sacred Church's altar (vanilla chiselled-quartz look) and its gold crystal (the Lyrium cluster's model). */
    private void registerSacredChurch() {
        ModelFile altar = models().cubeColumn("sacred_altar",
                ResourceLocation.withDefaultNamespace("block/chiseled_quartz_block"),
                ResourceLocation.withDefaultNamespace("block/chiseled_quartz_block_top"));
        simpleBlock(ModBlocks.SACRED_ALTAR.get(), altar);
        itemModels().withExistingParent("sacred_altar", modLoc("block/sacred_altar"));
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
