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
import dev.zsskayr.merlins_inferno.block.HellForgeBlock;
import dev.zsskayr.merlins_inferno.block.HellForgePartBlock;
import dev.zsskayr.merlins_inferno.block.RowanwoodLogBlock;
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

    // --- Rowanwood: the mystical tree (3.2). No building set - just the tree itself.
    // "Bem parrudo" (per project decision): hardness AND blast resistance both match Obsidian, and
    // - since there's no tool-tier requirement at all, any axe eventually gets through it, per the
    // doc's "é sobre tempo de quebra, não tool tier mínimo" - see RowanwoodLogBlock, which doubles
    // that already-Obsidian-matching break time unless the axe has "Toque do Druida". The other
    // half of the enchant requirement (it drops nothing at all without the enchant) is a loot
    // table condition, same as vanilla gates ore self-drops behind Silk Touch - see
    // data/merlins_inferno/loot_table/blocks/rowanwood_log.json. ---

    public static final DeferredBlock<RowanwoodLogBlock> ROWANWOOD_LOG = BLOCKS.registerBlock("rowanwood_log",
            RowanwoodLogBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(50.0F, 1200.0F) // matches Obsidian on both hardness and blast resistance
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
    // No Rowanwood sapling on purpose - see ModBlockLootProvider's comment: it'd let players farm
    // a 48x25x39 landmark structure paste from a sapling, wildly overpowered.

    // --- Compressed Netherrack: a Hell Forge ingredient. Just a plain solid block for now, no
    // special behavior. ---
    public static final DeferredBlock<Block> COMPRESSED_NETHERRACK = BLOCKS.registerSimpleBlock("compressed_netherrack",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER)
                    .strength(1.0F, 6.0F) // matches vanilla netherrack's blast resistance, a bit tougher to mine
                    .sound(SoundType.NETHERRACK));

    // --- Hell Forge: see HellForgeBlock's javadoc - block/blockentity exist for now purely to
    // carry HellForgeModel's oversized (~2.5x2.5x2 block) geometry via a BlockEntityRenderer.
    // No furnace/crafting-station behavior yet - that's a separate pass once this one (getting
    // the block itself craftable and rendering) is confirmed working. ---
    public static final DeferredBlock<HellForgeBlock> HELL_FORGE = BLOCKS.registerBlock("hell_forge",
            HellForgeBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(50.0F, 1200.0F) // Obsidian-tier blast resistance - meant to feel like a serious, rare structure
                    .sound(SoundType.NETHERRACK)
                    .lightLevel(state -> 7) // faint ambient glow, matches the "forge" theme
                    .noOcclusion()); // its real silhouette (via the BER) doesn't fill the block's own cube, so neighbors shouldn't be culled against it

    // --- Hell Forge part: invisible collision filler placed automatically around HELL_FORGE, see
    // HellForgePartBlock's javadoc. No item - never placed/obtained by hand. ---
    public static final DeferredBlock<HellForgePartBlock> HELL_FORGE_PART = BLOCKS.registerBlock("hell_forge_part",
            HellForgePartBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(50.0F, 1200.0F) // matches HELL_FORGE so every face takes the same effort to break
                    .sound(SoundType.NETHERRACK)
                    .noOcclusion()
                    .noLootTable()); // breaking a part drops via HellForgePartBlock's own logic, not loot tables

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
