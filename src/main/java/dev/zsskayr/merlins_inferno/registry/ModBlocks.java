package dev.zsskayr.merlins_inferno.registry;

import java.util.function.Supplier;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.block.HellForgeBlock;
import dev.zsskayr.merlins_inferno.block.LyriumBlock;
import dev.zsskayr.merlins_inferno.block.SacredAltarBlock;
import dev.zsskayr.merlins_inferno.block.LyriumClusterBlock;
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

    // --- Rest of Ashwood's building set (design doc, 3.1): stripped variants, fence gate, door,
    // trapdoor, pressure plate, button and both kinds of sign. Wall signs have no item of their
    // own and drop the standing sign's (see wallSignProperties). Doors/signs need a
    // BlockSetType/WoodType - see ModWoodTypes. ---

    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_ASHWOOD_LOG = BLOCKS.registerBlock("stripped_ashwood_log",
            props -> new RotatedPillarBlock(props), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_ASHWOOD_WOOD = BLOCKS.registerBlock("stripped_ashwood_wood",
            props -> new RotatedPillarBlock(props), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());
    public static final DeferredBlock<FenceGateBlock> ASHWOOD_FENCE_GATE = BLOCKS.registerBlock("ashwood_fence_gate",
            props -> new FenceGateBlock(ModWoodTypes.ASHWOOD, props),
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<DoorBlock> ASHWOOD_DOOR = BLOCKS.registerBlock("ashwood_door",
            props -> new DoorBlock(ModWoodTypes.ASHWOOD_SET, props),
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<TrapDoorBlock> ASHWOOD_TRAPDOOR = BLOCKS.registerBlock("ashwood_trapdoor",
            props -> new TrapDoorBlock(ModWoodTypes.ASHWOOD_SET, props),
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()
                    .isValidSpawn((state, level, pos, type) -> false));
    public static final DeferredBlock<PressurePlateBlock> ASHWOOD_PRESSURE_PLATE = BLOCKS.registerBlock("ashwood_pressure_plate",
            props -> new PressurePlateBlock(ModWoodTypes.ASHWOOD_SET, props),
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS)
                    .noCollission().strength(0.5F).sound(SoundType.WOOD).ignitedByLava().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<ButtonBlock> ASHWOOD_BUTTON = BLOCKS.registerBlock("ashwood_button",
            props -> new ButtonBlock(ModWoodTypes.ASHWOOD_SET, 30, props),
            BlockBehaviour.Properties.of().noCollission().strength(0.5F).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<StandingSignBlock> ASHWOOD_SIGN = BLOCKS.registerBlock("ashwood_sign",
            props -> new StandingSignBlock(ModWoodTypes.ASHWOOD, props), signProperties());
    public static final DeferredBlock<WallSignBlock> ASHWOOD_WALL_SIGN = BLOCKS.registerBlock("ashwood_wall_sign",
            props -> new WallSignBlock(ModWoodTypes.ASHWOOD, props), wallSignProperties(ASHWOOD_SIGN));
    public static final DeferredBlock<CeilingHangingSignBlock> ASHWOOD_HANGING_SIGN = BLOCKS.registerBlock("ashwood_hanging_sign",
            props -> new CeilingHangingSignBlock(ModWoodTypes.ASHWOOD, props), signProperties());
    public static final DeferredBlock<WallHangingSignBlock> ASHWOOD_WALL_HANGING_SIGN = BLOCKS.registerBlock("ashwood_wall_hanging_sign",
            props -> new WallHangingSignBlock(ModWoodTypes.ASHWOOD, props), wallSignProperties(ASHWOOD_HANGING_SIGN));

    private static BlockBehaviour.Properties signProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS)
                .noCollission().strength(1.0F).sound(SoundType.WOOD).ignitedByLava();
    }

    /** A wall sign has no item of its own - it breaks into the standing sign's item (same loot table). */
    private static BlockBehaviour.Properties wallSignProperties(Supplier<? extends Block> standingSign) {
        return signProperties().lootFrom(standingSign);
    }

    // --- Rowanwood: the mystical tree (3.2). No building set - just the tree itself.
    // "Bem parrudo" (per project decision): hardness AND blast resistance both match Obsidian, and
    // - since there's no tool-tier requirement at all, any axe eventually gets through it, per the
    // doc's "é sobre tempo de quebra, não tool tier mínimo" - see RowanwoodLogBlock, which doubles
    // that already-Obsidian-matching break time unless the axe has "Toque do Druida". The other
    // half of the enchant requirement (without the enchant it drops plain Ashwood) is a loot
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

    // --- Compressed Netherrack: a Hell Forge ingredient and, with its building set (stairs, slab,
    // wall, button, pressure plate), a build block. Blast resistance matches Obsidian (1200) so it
    // holds up against explosions; hardness stays low enough to mine comfortably. ---
    public static final DeferredBlock<Block> COMPRESSED_NETHERRACK = BLOCKS.registerBlock("compressed_netherrack",
            Block::new, compressedNetherrackProperties());
    public static final DeferredBlock<StairBlock> COMPRESSED_NETHERRACK_STAIRS = BLOCKS.registerBlock("compressed_netherrack_stairs",
            props -> new StairBlock(COMPRESSED_NETHERRACK.get().defaultBlockState(), props), compressedNetherrackProperties());
    public static final DeferredBlock<SlabBlock> COMPRESSED_NETHERRACK_SLAB = BLOCKS.registerBlock("compressed_netherrack_slab",
            SlabBlock::new, compressedNetherrackProperties());
    public static final DeferredBlock<WallBlock> COMPRESSED_NETHERRACK_WALL = BLOCKS.registerBlock("compressed_netherrack_wall",
            WallBlock::new, compressedNetherrackProperties().forceSolidOn());

    private static BlockBehaviour.Properties compressedNetherrackProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NETHER)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .strength(2.0F, 1200.0F)
                .sound(SoundType.NETHERRACK);
    }

    // --- Lyrium (celestial): where Raw Lyrium comes from. Formed like amethyst (a geode lined with
    // Lyrium Block and studded with Lyrium Clusters) and as rare emerald-style ores in mountains -
    // see datagen.worldgen.ModLyriumProvider. Mined stats copy the emerald/amethyst equivalents. ---
    public static final DeferredBlock<DropExperienceBlock> LYRIUM_ORE = BLOCKS.registerBlock("lyrium_ore",
            props -> new DropExperienceBlock(UniformInt.of(3, 7), props), BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_LYRIUM_ORE = BLOCKS.registerBlock("deepslate_lyrium_ore",
            props -> new DropExperienceBlock(UniformInt.of(3, 7), props), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_EMERALD_ORE));
    // The geode blocks blaze at the maximum light level and sanctify undead/demons around them
    // (random ticks - see block.LyriumRadiance) - but only the world's own blocks: see LyriumBlock#PLACED.
    // The lining is immovable for pistons so a natural block can't be shoved into a base.
    // Deliberately no budding variant: a geode is finite.
    public static final DeferredBlock<LyriumBlock> LYRIUM_BLOCK = BLOCKS.registerBlock("lyrium_block",
            LyriumBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).lightLevel(state -> 15).pushReaction(PushReaction.BLOCK));
    // Same shape/size as an amethyst cluster (height 7, offset 3), drops Raw Lyrium - see its loot table.
    public static final DeferredBlock<LyriumClusterBlock> LYRIUM_CLUSTER = BLOCKS.registerBlock("lyrium_cluster",
            props -> new LyriumClusterBlock(7.0F, 3.0F, props), BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel(state -> 15));

    // --- Sacred Church (Elias's). The altar holds the church's state, so it can't be broken in survival and
    // drops nothing. The Great Bell that empowers Elias is built from vanilla blocks (see GreatBell). ---
    public static final DeferredBlock<SacredAltarBlock> SACRED_ALTAR = BLOCKS.registerBlock("sacred_altar",
            SacredAltarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_QUARTZ_BLOCK).strength(-1.0F, 3600000.0F)
                    .noLootTable().lightLevel(state -> 8).pushReaction(PushReaction.BLOCK));

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
