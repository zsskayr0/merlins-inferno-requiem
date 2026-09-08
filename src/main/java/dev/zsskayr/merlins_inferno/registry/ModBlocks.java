package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.worldgen.ModTreeGrowers;

/**
 * Central registry for every {@code Block} the mod adds.
 * <p>
 * Remember: registering a block here does NOT give it an inventory item.
 * Pair each block with a {@code BlockItem} registration in {@link ModItems}
 * (e.g. via {@code ModItems.ITEMS.registerSimpleBlockItem(...)}) if the player
 * should be able to hold/place it.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Merlins_inferno.MODID);

    // --- Ashwood: the Hallowed Grove's common tree. Full building set (Rowanwood & Hallowed
    // Grove design doc, 3.1) - no gates, cuts with any axe like vanilla wood. Only the "vibe
    // wood" basics are here for now (log/wood/planks/leaves/sapling/stairs/slab/fence); door,
    // trapdoor, pressure plate, button and sign/hanging sign are a fast, separate follow-up -
    // they need their own WoodType/BlockSetType and (for signs) a BlockEntity, better done as
    // their own reviewable pass than folded into this one. ---

    public static final DeferredBlock<RotatedPillarBlock> ASHWOOD_LOG = BLOCKS.registerBlock("ashwood_log",
            props -> new RotatedPillarBlock(props), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());
    public static final DeferredBlock<RotatedPillarBlock> ASHWOOD_WOOD = BLOCKS.registerBlock("ashwood_wood",
            props -> new RotatedPillarBlock(props), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());
    public static final DeferredBlock<Block> ASHWOOD_PLANKS = BLOCKS.registerSimpleBlock("ashwood_planks",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());
    public static final DeferredBlock<LeavesBlock> ASHWOOD_LEAVES = BLOCKS.registerBlock("ashwood_leaves",
            props -> new LeavesBlock(props), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
                    .isRedstoneConductor((state, level, pos) -> false));
    public static final DeferredBlock<SaplingBlock> ASHWOOD_SAPLING = BLOCKS.registerBlock("ashwood_sapling",
            props -> new SaplingBlock(ModTreeGrowers.ASHWOOD, props), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS));
    public static final DeferredBlock<StairBlock> ASHWOOD_STAIRS = BLOCKS.registerBlock("ashwood_stairs",
            props -> new StairBlock(ASHWOOD_PLANKS.get().defaultBlockState(), props),
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));
    public static final DeferredBlock<SlabBlock> ASHWOOD_SLAB = BLOCKS.registerBlock("ashwood_slab",
            props -> new SlabBlock(props),
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<FenceBlock> ASHWOOD_FENCE = BLOCKS.registerBlock("ashwood_fence",
            props -> new FenceBlock(props),
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());

    // --- Rowanwood: the mystical tree (3.2). No building set - just the tree itself. The log's
    // hardness matches Obsidian (long break time), but with NO tool-tier requirement (any axe
    // works, per the doc: "é sobre tempo de quebra, não tool tier mínimo"), so no
    // requiresCorrectToolForDrops() call here.
    //
    // TODO(Toque do Druida): per 3.2's drop rule, this should only drop the raw Rowanwood
    // material when broken with an axe enchanted with "Toque do Druida" - without it, breaking
    // should yield only plain wood. That enchantment doesn't exist yet (design doc item 2), so
    // for now the loot table (see ModBlockLootProvider) just drops the log itself like a normal
    // tree. Revisit once the enchantment and the raw-material item exist. ---

    public static final DeferredBlock<RotatedPillarBlock> ROWANWOOD_LOG = BLOCKS.registerBlock("rowanwood_log",
            props -> new RotatedPillarBlock(props), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(50.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());
    public static final DeferredBlock<LeavesBlock> ROWANWOOD_LEAVES = BLOCKS.registerBlock("rowanwood_leaves",
            props -> new LeavesBlock(props), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
                    .isRedstoneConductor((state, level, pos) -> false));
    public static final DeferredBlock<SaplingBlock> ROWANWOOD_SAPLING = BLOCKS.registerBlock("rowanwood_sapling",
            props -> new SaplingBlock(ModTreeGrowers.ROWANWOOD, props), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS));

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
