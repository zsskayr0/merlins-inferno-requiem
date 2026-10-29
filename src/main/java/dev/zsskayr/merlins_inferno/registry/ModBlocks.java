package dev.zsskayr.merlins_inferno.registry;

import java.util.function.Supplier;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.ChainBlock;
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
import dev.zsskayr.merlins_inferno.block.BlackNyliumBlock;
import dev.zsskayr.merlins_inferno.block.BlightstoneNyliumBlock;
import dev.zsskayr.merlins_inferno.block.CorruptedObsidianBlock;
import dev.zsskayr.merlins_inferno.block.DryingRackBlock;
import dev.zsskayr.merlins_inferno.block.PandoraBoxBlock;
import dev.zsskayr.merlins_inferno.block.EdenweedBushBlock;
import dev.zsskayr.merlins_inferno.block.HellForgeControllerBlock;
import dev.zsskayr.merlins_inferno.block.HellForgeCoreBlock;
import dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock;
import dev.zsskayr.merlins_inferno.block.LyriumBlock;
import dev.zsskayr.merlins_inferno.block.SoulShroomlightBlock;
import dev.zsskayr.merlins_inferno.block.NetherDoublePlantBlock;
import dev.zsskayr.merlins_inferno.block.NetherFlowerBlock;
import dev.zsskayr.merlins_inferno.block.SacredAltarBlock;
import dev.zsskayr.merlins_inferno.block.SacredAltarPartBlock;
import dev.zsskayr.merlins_inferno.block.LyriumClusterBlock;
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

    // --- Haunted wood: the Haunted Forest's full building set (log and leaves are defined further up). ---
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_HAUNTED_LOG = BLOCKS.registerBlock("stripped_haunted_log",
            props -> new RotatedPillarBlock(props), hauntedWood(2.0F));
    public static final DeferredBlock<RotatedPillarBlock> HAUNTED_WOOD = BLOCKS.registerBlock("haunted_wood",
            props -> new RotatedPillarBlock(props), hauntedWood(2.0F));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_HAUNTED_WOOD = BLOCKS.registerBlock("stripped_haunted_wood",
            props -> new RotatedPillarBlock(props), hauntedWood(2.0F));
    public static final DeferredBlock<Block> HAUNTED_PLANKS = BLOCKS.registerSimpleBlock("haunted_planks", hauntedWood(2.0F).strength(2.0F, 3.0F));
    public static final DeferredBlock<StairBlock> HAUNTED_STAIRS = BLOCKS.registerBlock("haunted_stairs",
            props -> new StairBlock(HAUNTED_PLANKS.get().defaultBlockState(), props), hauntedWood(2.0F).strength(2.0F, 3.0F));
    public static final DeferredBlock<SlabBlock> HAUNTED_SLAB = BLOCKS.registerBlock("haunted_slab",
            props -> new SlabBlock(props), hauntedWood(2.0F).strength(2.0F, 3.0F));
    public static final DeferredBlock<FenceBlock> HAUNTED_FENCE = BLOCKS.registerBlock("haunted_fence",
            props -> new FenceBlock(props), hauntedWood(2.0F).strength(2.0F, 3.0F));
    public static final DeferredBlock<FenceGateBlock> HAUNTED_FENCE_GATE = BLOCKS.registerBlock("haunted_fence_gate",
            props -> new FenceGateBlock(ModWoodTypes.HAUNTED, props), hauntedWood(2.0F).forceSolidOn().strength(2.0F, 3.0F));
    public static final DeferredBlock<DoorBlock> HAUNTED_DOOR = BLOCKS.registerBlock("haunted_door",
            props -> new DoorBlock(ModWoodTypes.HAUNTED_SET, props),
            hauntedWood(3.0F).strength(3.0F).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<TrapDoorBlock> HAUNTED_TRAPDOOR = BLOCKS.registerBlock("haunted_trapdoor",
            props -> new TrapDoorBlock(ModWoodTypes.HAUNTED_SET, props),
            hauntedWood(3.0F).strength(3.0F).noOcclusion().isValidSpawn((state, level, pos, type) -> false));
    public static final DeferredBlock<PressurePlateBlock> HAUNTED_PRESSURE_PLATE = BLOCKS.registerBlock("haunted_pressure_plate",
            props -> new PressurePlateBlock(ModWoodTypes.HAUNTED_SET, props),
            hauntedWood(0.5F).forceSolidOn().noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<ButtonBlock> HAUNTED_BUTTON = BLOCKS.registerBlock("haunted_button",
            props -> new ButtonBlock(ModWoodTypes.HAUNTED_SET, 30, props),
            BlockBehaviour.Properties.of().noCollission().strength(0.5F).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<StandingSignBlock> HAUNTED_SIGN = BLOCKS.registerBlock("haunted_sign",
            props -> new StandingSignBlock(ModWoodTypes.HAUNTED, props), hauntedSignProperties());
    public static final DeferredBlock<WallSignBlock> HAUNTED_WALL_SIGN = BLOCKS.registerBlock("haunted_wall_sign",
            props -> new WallSignBlock(ModWoodTypes.HAUNTED, props), hauntedSignProperties().lootFrom(HAUNTED_SIGN));
    public static final DeferredBlock<CeilingHangingSignBlock> HAUNTED_HANGING_SIGN = BLOCKS.registerBlock("haunted_hanging_sign",
            props -> new CeilingHangingSignBlock(ModWoodTypes.HAUNTED, props), hauntedSignProperties());
    public static final DeferredBlock<WallHangingSignBlock> HAUNTED_WALL_HANGING_SIGN = BLOCKS.registerBlock("haunted_wall_hanging_sign",
            props -> new WallHangingSignBlock(ModWoodTypes.HAUNTED, props), hauntedSignProperties().lootFrom(HAUNTED_HANGING_SIGN));

    private static BlockBehaviour.Properties hauntedSignProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).forceSolidOn().instrument(NoteBlockInstrument.BASS)
                .noCollission().strength(1.0F).sound(SoundType.WOOD);
    }

    private static BlockBehaviour.Properties hauntedWood(float strength) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).instrument(NoteBlockInstrument.BASS)
                .strength(strength).sound(SoundType.WOOD);
    }

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

    // --- Blightstone: Oblivion's charcoal-grey building stone (plain, bricks, cracked, chiseled, plus
    // stairs/slab/wall for the plain and brick forms). Blackstone-tough, no special tool needed. ---
    public static final DeferredBlock<Block> BLIGHTSTONE = BLOCKS.registerBlock("blightstone",
            Block::new, blightstoneProperties());
    public static final DeferredBlock<StairBlock> BLIGHTSTONE_STAIRS = BLOCKS.registerBlock("blightstone_stairs",
            props -> new StairBlock(BLIGHTSTONE.get().defaultBlockState(), props), blightstoneProperties());
    public static final DeferredBlock<SlabBlock> BLIGHTSTONE_SLAB = BLOCKS.registerBlock("blightstone_slab",
            SlabBlock::new, blightstoneProperties());
    public static final DeferredBlock<WallBlock> BLIGHTSTONE_WALL = BLOCKS.registerBlock("blightstone_wall",
            WallBlock::new, blightstoneProperties().forceSolidOn());
    public static final DeferredBlock<Block> BLIGHTSTONE_BRICKS = BLOCKS.registerBlock("blightstone_bricks",
            Block::new, blightstoneProperties());
    public static final DeferredBlock<StairBlock> BLIGHTSTONE_BRICK_STAIRS = BLOCKS.registerBlock("blightstone_brick_stairs",
            props -> new StairBlock(BLIGHTSTONE_BRICKS.get().defaultBlockState(), props), blightstoneProperties());
    public static final DeferredBlock<SlabBlock> BLIGHTSTONE_BRICK_SLAB = BLOCKS.registerBlock("blightstone_brick_slab",
            SlabBlock::new, blightstoneProperties());
    public static final DeferredBlock<WallBlock> BLIGHTSTONE_BRICK_WALL = BLOCKS.registerBlock("blightstone_brick_wall",
            WallBlock::new, blightstoneProperties().forceSolidOn());
    public static final DeferredBlock<Block> POLISHED_BLIGHTSTONE = BLOCKS.registerBlock("polished_blightstone",
            Block::new, blightstoneProperties());
    public static final DeferredBlock<StairBlock> POLISHED_BLIGHTSTONE_STAIRS = BLOCKS.registerBlock("polished_blightstone_stairs",
            props -> new StairBlock(POLISHED_BLIGHTSTONE.get().defaultBlockState(), props), blightstoneProperties());
    public static final DeferredBlock<SlabBlock> POLISHED_BLIGHTSTONE_SLAB = BLOCKS.registerBlock("polished_blightstone_slab",
            SlabBlock::new, blightstoneProperties());
    public static final DeferredBlock<WallBlock> POLISHED_BLIGHTSTONE_WALL = BLOCKS.registerBlock("polished_blightstone_wall",
            WallBlock::new, blightstoneProperties().forceSolidOn());
    public static final DeferredBlock<Block> CHISELED_POLISHED_BLIGHTSTONE = BLOCKS.registerBlock("chiseled_polished_blightstone",
            Block::new, blightstoneProperties());
    public static final DeferredBlock<Block> BLIGHTSTONE_SMALL_BRICKS = BLOCKS.registerBlock("blightstone_small_bricks",
            Block::new, blightstoneProperties());
    public static final DeferredBlock<StairBlock> BLIGHTSTONE_SMALL_BRICK_STAIRS = BLOCKS.registerBlock("blightstone_small_brick_stairs",
            props -> new StairBlock(BLIGHTSTONE_SMALL_BRICKS.get().defaultBlockState(), props), blightstoneProperties());
    public static final DeferredBlock<SlabBlock> BLIGHTSTONE_SMALL_BRICK_SLAB = BLOCKS.registerBlock("blightstone_small_brick_slab",
            SlabBlock::new, blightstoneProperties());
    public static final DeferredBlock<WallBlock> BLIGHTSTONE_SMALL_BRICK_WALL = BLOCKS.registerBlock("blightstone_small_brick_wall",
            WallBlock::new, blightstoneProperties().forceSolidOn());
    public static final DeferredBlock<Block> CRACKED_BLIGHTSTONE_BRICKS = BLOCKS.registerBlock("cracked_blightstone_bricks",
            Block::new, blightstoneProperties());

    // --- Arganite & Porcelain: the angelic realm's pale rock (Arganite, Quartzite) and the porcelain built from it
    // (ground, mixed into dusts, smelted). Arganite hurts to land on (x2), porcelain is gentle (x0.2). ---
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.ArganiteBlock> ARGANITE = BLOCKS.registerBlock("arganite",
            dev.zsskayr.merlins_inferno.block.ArganiteBlock::new, arganiteProperties());
    public static final DeferredBlock<StairBlock> ARGANITE_STAIRS = BLOCKS.registerBlock("arganite_stairs",
            props -> new StairBlock(ARGANITE.get().defaultBlockState(), props), arganiteProperties());
    public static final DeferredBlock<SlabBlock> ARGANITE_SLAB = BLOCKS.registerBlock("arganite_slab",
            SlabBlock::new, arganiteProperties());
    public static final DeferredBlock<WallBlock> ARGANITE_WALL = BLOCKS.registerBlock("arganite_wall",
            WallBlock::new, arganiteProperties().forceSolidOn());
    public static final DeferredBlock<Block> ARGANITE_BRICKS = BLOCKS.registerBlock("arganite_bricks",
            Block::new, arganiteProperties());
    public static final DeferredBlock<StairBlock> ARGANITE_BRICKS_STAIRS = BLOCKS.registerBlock("arganite_bricks_stairs",
            props -> new StairBlock(ARGANITE_BRICKS.get().defaultBlockState(), props), arganiteProperties());
    public static final DeferredBlock<SlabBlock> ARGANITE_BRICKS_SLAB = BLOCKS.registerBlock("arganite_bricks_slab",
            SlabBlock::new, arganiteProperties());
    public static final DeferredBlock<WallBlock> ARGANITE_BRICKS_WALL = BLOCKS.registerBlock("arganite_bricks_wall",
            WallBlock::new, arganiteProperties().forceSolidOn());
    public static final DeferredBlock<Block> QUARTZITE = BLOCKS.registerBlock("quartzite",
            Block::new, quartziteProperties());
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.PorcelainBlock> PORCELAIN = BLOCKS.registerBlock("porcelain",
            dev.zsskayr.merlins_inferno.block.PorcelainBlock::new, porcelainProperties());
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.PorcelainStairBlock> PORCELAIN_STAIRS = BLOCKS.registerBlock("porcelain_stairs",
            props -> new dev.zsskayr.merlins_inferno.block.PorcelainStairBlock(PORCELAIN.get().defaultBlockState(), props), porcelainProperties());
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.PorcelainSlabBlock> PORCELAIN_SLAB = BLOCKS.registerBlock("porcelain_slab",
            dev.zsskayr.merlins_inferno.block.PorcelainSlabBlock::new, porcelainProperties());
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.PorcelainBlock> POLISHED_PORCELAIN = BLOCKS.registerBlock("polished_porcelain",
            dev.zsskayr.merlins_inferno.block.PorcelainBlock::new, porcelainProperties());
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.PorcelainStairBlock> POLISHED_PORCELAIN_STAIRS = BLOCKS.registerBlock("polished_porcelain_stairs",
            props -> new dev.zsskayr.merlins_inferno.block.PorcelainStairBlock(POLISHED_PORCELAIN.get().defaultBlockState(), props), porcelainProperties());
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.PorcelainSlabBlock> POLISHED_PORCELAIN_SLAB = BLOCKS.registerBlock("polished_porcelain_slab",
            dev.zsskayr.merlins_inferno.block.PorcelainSlabBlock::new, porcelainProperties());
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.PorcelainBlock> TILED_PORCELAIN = BLOCKS.registerBlock("tiled_porcelain",
            dev.zsskayr.merlins_inferno.block.PorcelainBlock::new, tiledPorcelainProperties());
    // Crystal on Quartzite: an amethyst cluster's shape, no light, no regrowth. Loot table is hand-authored.
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.QuartzClusterBlock> QUARTZ_CLUSTER = BLOCKS.registerBlock("quartz_cluster",
            props -> new dev.zsskayr.merlins_inferno.block.QuartzClusterBlock(7.0F, 3.0F, props),
            BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel(state -> 0).mapColor(MapColor.QUARTZ));

    // Sacred white fire: infinite over Arganite (infiniburn tag), burns out elsewhere, never spreads. Lit with flint and steel on Arganite.
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.GraceFireBlock> GRACE_FIRE = BLOCKS.registerBlock("grace_fire",
            dev.zsskayr.merlins_inferno.block.GraceFireBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).replaceable()
                    .noCollission().instabreak().lightLevel(state -> 10).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY));

    private static BlockBehaviour.Properties arganiteProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties quartziteProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties porcelainProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.DEEPSLATE_BRICKS);
    }

    private static BlockBehaviour.Properties tiledPorcelainProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.DEEPSLATE_BRICKS);
    }

    private static BlockBehaviour.Properties blightstoneProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(1.5F, 6.0F)
                .sound(SoundType.DEEPSLATE_BRICKS);
    }

    private static BlockBehaviour.Properties compressedNetherrackProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NETHER)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .strength(2.0F, 1200.0F)
                .sound(SoundType.NETHERRACK);
    }

    // --- Lyrium (celestial): where Raw Lyrium comes from. Formed like amethyst (a geode lined with
    // Lyrium Block and studded with Lyrium Clusters) in mountains -
    // see datagen.worldgen.ModLyriumProvider. Mined stats copy the amethyst equivalents. ---
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
                    .noLootTable().lightLevel(state -> 8).pushReaction(PushReaction.BLOCK).noOcclusion());

    // The invisible parts that give the altar its 3 x 3 x 2 hitbox, see SacredAltarPartBlock. No item, never placed by hand.
    public static final DeferredBlock<SacredAltarPartBlock> SACRED_ALTAR_PART = BLOCKS.registerBlock("sacred_altar_part",
            SacredAltarPartBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_QUARTZ_BLOCK).strength(-1.0F, 3600000.0F)
                    .noLootTable().pushReaction(PushReaction.BLOCK).noOcclusion());

    // The film of an Oblivion portal; lit with the Key of Oblivion (see OblivionKeyItem). No item, no drops.
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.OblivionPortalBlock> OBLIVION_PORTAL = BLOCKS.registerBlock("oblivion_portal",
            dev.zsskayr.merlins_inferno.block.OblivionPortalBlock::new, BlockBehaviour.Properties.of().noCollission().strength(-1.0F)
                    .sound(SoundType.GLASS).lightLevel(state -> 5).pushReaction(PushReaction.BLOCK).noLootTable());

    // --- Hell Forge multiblock (3x3x3, see HellForgeStructure): Controller (GUI + inventory, front
    // face centre), Core (centre, sets the forge tier) and Vault (configurable automation port).
    // The 20 edge/corner blocks are plain Compressed Netherrack. ---
    public static final DeferredBlock<HellForgeControllerBlock> HELL_FORGE_CONTROLLER = BLOCKS.registerBlock("hell_forge_controller",
            HellForgeControllerBlock::new, hellForgePartProperties().lightLevel(state -> 7)); // faint ambient glow, matches the "forge" theme

    public static final DeferredBlock<HellForgeCoreBlock> DEMONIC_CORE = BLOCKS.registerBlock("demonic_core",
            props -> new HellForgeCoreBlock(props, 1), hellForgePartProperties());

    public static final DeferredBlock<HellForgeVaultBlock> HELL_FORGE_VAULT = BLOCKS.registerBlock("hell_forge_vault",
            HellForgeVaultBlock::new, hellForgePartProperties());

    /** Shared by every structural Hell Forge block: Obsidian-tier blast resistance, meant to feel like a serious, rare structure. */
    private static BlockBehaviour.Properties hellForgePartProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(50.0F, 1200.0F)
                .sound(SoundType.NETHERRACK);
    }

    // --- Corrupted Obsidian: crying obsidian's demonic cousin, red instead of purple. Andras' Ancient Battlefield's boss room
    // uses it for its portal frame - decorative only for now (the Oblivion portal frame, which it is
    // meant to become, is wired up separately). Unbreakable in Circle 1 (see CorruptedObsidianBlock); from Circle 2 on it's minable, at
    // 3x Obsidian's hardness and gated behind a tool tier above Netherite that doesn't exist yet (see
    // data/minecraft/tags/block/incorrect_for_netherite_tool.json). Not craftable - only found generated. ---
    public static final DeferredBlock<CorruptedObsidianBlock> CORRUPTED_OBSIDIAN = BLOCKS.registerBlock("corrupted_obsidian",
            CorruptedObsidianBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CRYING_OBSIDIAN)
                    .strength(CorruptedObsidianBlock.HARDNESS, 1200.0F).requiresCorrectToolForDrops());

    // --- Ebony Debris: Circle 2's answer to Ancient Debris, found in Oblivion. Drops Ebony Scrap (Silk Touch keeps the block). ---
    public static final DeferredBlock<Block> EBONY_DEBRIS = BLOCKS.registerBlock("ebony_debris",
            dev.zsskayr.merlins_inferno.block.EbonyDebrisBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.ANCIENT_DEBRIS));

    // --- Petrified Skull: crafted from Wither Skulls, Withered Bone and Compressed Netherrack. Obsidian's own
    // hardness/blast resistance ("a resistência de obsidian"), nothing else special yet. ---
    public static final DeferredBlock<Block> PETRIFIED_SKULL = BLOCKS.registerBlock("petrified_skull",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN));

    // --- Withered Obsidian: crafted from Petrified Skull, Rowanwood, Lyrium and Otherworld Essence (see the recipe).
    // Also builds the Withered Hills' spires. Far tougher than Obsidian: 5x its hardness and a blast resistance
    // so high that no explosion, Wither or Ender Dragon breaks it, and pistons can't move it. ---
    public static final DeferredBlock<Block> WITHERED_OBSIDIAN = BLOCKS.registerBlock("withered_obsidian",
            Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)
                    .strength(250.0F, 3600000.0F).requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK));

    // --- Flesh: Netherrack-like, the Nether's answer to Moss Block (slab and carpet included). Not craftable -
    // only found in the world; the slab/carpet recipes go from and back to the block. ---
    public static final DeferredBlock<Block> FLESH_BLOCK = BLOCKS.registerBlock("flesh_block",
            Block::new, fleshProperties());
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.MouthBlock> MOUTH_BLOCK =
            BLOCKS.registerBlock("mouth_block", dev.zsskayr.merlins_inferno.block.MouthBlock::new,
                    fleshProperties().noOcclusion().isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));
    public static final DeferredBlock<SlabBlock> FLESH_SLAB = BLOCKS.registerBlock("flesh_slab",
            SlabBlock::new, fleshProperties());
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.OrnatedCarpetBlock> ORNATED_CARPET =
            BLOCKS.registerBlock("ornated_carpet", dev.zsskayr.merlins_inferno.block.OrnatedCarpetBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.RED_CARPET).noOcclusion());

    public static final DeferredBlock<CarpetBlock> FLESH_CARPET = BLOCKS.registerBlock("flesh_carpet",
            CarpetBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.1F)
                    .sound(SoundType.NETHERRACK)
                    .pushReaction(PushReaction.DESTROY));

    // --- Carnal Heap (the Oblivion's flesh biome): pitted flesh, hanging tendons, hair and the eyebulb. ---
    public static final DeferredBlock<Block> POROUS_FLESH = BLOCKS.registerBlock("porous_flesh",
            Block::new, fleshProperties());
    // Haunted Forest trees (the Oblivion Forest biome): logs and the weeping leaves.
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> HAUNTED_LOG = BLOCKS.registerBlock("haunted_log",
            net.minecraft.world.level.block.RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_STEM)
                    .mapColor(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.ShelfShroomBlock> HAUNTED_SHELF_SHROOM = BLOCKS.registerBlock("haunted_shelf_shroom",
            dev.zsskayr.merlins_inferno.block.ShelfShroomBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .noCollission().instabreak().sound(SoundType.FUNGUS).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<net.minecraft.world.level.block.LeavesBlock> HAUNTED_LEAVES = BLOCKS.registerBlock("haunted_leaves",
            net.minecraft.world.level.block.LeavesBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.2F).randomTicks()
                    .sound(SoundType.GRASS).noOcclusion().isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)
                    .isRedstoneConductor((state, level, pos) -> false).pushReaction(PushReaction.DESTROY));
    // Pale grave dust of the Carnal Heap. Smelts into Crypt Glass, which mixes with netherrack or flesh into Hellglass.
    public static final DeferredBlock<net.minecraft.world.level.block.ColoredFallingBlock> GRAVE_SAND = BLOCKS.registerBlock("grave_sand",
            props -> new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(0xA89F94), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).instrument(NoteBlockInstrument.SNARE)
                    .strength(0.5F).sound(SoundType.SAND));
    public static final DeferredBlock<Block> GRAVE_SANDSTONE = BLOCKS.registerBlock("grave_sandstone",
            Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(0.8F));
    public static final DeferredBlock<net.minecraft.world.level.block.TransparentBlock> CRYPT_GLASS = BLOCKS.registerBlock("crypt_glass",
            net.minecraft.world.level.block.TransparentBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((s, l, p, t) -> false).isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));
    // Obsidian-tough, and it does not need Silk Touch: a glass meant for building whole bases.
    public static final DeferredBlock<net.minecraft.world.level.block.TransparentBlock> HELLGLASS = BLOCKS.registerBlock("hellglass",
            net.minecraft.world.level.block.TransparentBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .instrument(NoteBlockInstrument.HAT).strength(5.0F, 1200.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 3)
                    .isValidSpawn((s, l, p, t) -> false).isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));
    public static final DeferredBlock<net.minecraft.world.level.block.IronBarsBlock> CRYPT_GLASS_PANE = BLOCKS.registerBlock("crypt_glass_pane",
            net.minecraft.world.level.block.IronBarsBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion());
    public static final DeferredBlock<net.minecraft.world.level.block.IronBarsBlock> HELLGLASS_PANE = BLOCKS.registerBlock("hellglass_pane",
            net.minecraft.world.level.block.IronBarsBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .instrument(NoteBlockInstrument.HAT).strength(5.0F, 1200.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 3));
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.ChurchCandelabreBlock> CHURCH_CANDELABRE = BLOCKS.registerBlock("church_candelabre",
            dev.zsskayr.merlins_inferno.block.ChurchCandelabreBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(1.0F).sound(SoundType.LANTERN).noOcclusion().lightLevel(s -> 14).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.ChurchTombstoneBlock> CHURCH_TOMBSTONE = BLOCKS.registerBlock("church_tombstone",
            dev.zsskayr.merlins_inferno.block.ChurchTombstoneBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(1.5F, 6.0F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> CHARRED_FLESH = BLOCKS.registerBlock("charred_flesh",
            Block::new, fleshProperties().mapColor(MapColor.COLOR_BLACK).strength(0.6F));
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.FleshTendonsBlock> TENDONS = BLOCKS.registerBlock("tendons",
            dev.zsskayr.merlins_inferno.block.FleshTendonsBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).noCollission().instabreak().sound(SoundType.WEEPING_VINES)
                    .pushReaction(PushReaction.DESTROY));
    // Lichen-like veins of flesh that cling to any face (Oblivion, mostly Carnal Heaps). Drops nothing for now.
    public static final DeferredBlock<net.minecraft.world.level.block.GlowLichenBlock> VISCERA = BLOCKS.registerBlock("viscera",
            net.minecraft.world.level.block.GlowLichenBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).replaceable().noCollission().strength(0.2F).sound(SoundType.WEEPING_VINES)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<net.minecraft.world.level.block.NetherSproutsBlock> HAIR = BLOCKS.registerBlock("hair",
            net.minecraft.world.level.block.NetherSproutsBlock::new, netherPlantProperties(MapColor.COLOR_RED));
    public static final DeferredBlock<NetherDoublePlantBlock> EYEBULB = BLOCKS.registerBlock("eyebulb",
            NetherDoublePlantBlock::new, netherPlantProperties(MapColor.COLOR_RED).lightLevel(state -> 6));
    // Short version of the eyebulb (one block): same stalk, no extra length.
    public static final DeferredBlock<net.minecraft.world.level.block.NetherSproutsBlock> SMALL_EYEBULB = BLOCKS.registerBlock("small_eyebulb",
            net.minecraft.world.level.block.NetherSproutsBlock::new, netherPlantProperties(MapColor.COLOR_RED).lightLevel(state -> 5));

    // Carnal Heap undergrowth: skin ground, spines and body-part plants (textures recoloured from packs).
    public static final DeferredBlock<Block> SKIN = BLOCKS.registerBlock("skin",
            Block::new, fleshProperties().sound(SoundType.NYLIUM));
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> SPINE = BLOCKS.registerBlock("spine",
            net.minecraft.world.level.block.RotatedPillarBlock::new, fleshProperties().strength(1.0F));
    public static final DeferredBlock<net.minecraft.world.level.block.NetherSproutsBlock> NERVES = BLOCKS.registerBlock("nerves",
            net.minecraft.world.level.block.NetherSproutsBlock::new, netherPlantProperties(MapColor.COLOR_RED));
    public static final DeferredBlock<net.minecraft.world.level.block.NetherSproutsBlock> HEART = BLOCKS.registerBlock("heart",
            net.minecraft.world.level.block.NetherSproutsBlock::new, netherPlantProperties(MapColor.COLOR_RED));
    public static final DeferredBlock<net.minecraft.world.level.block.NetherSproutsBlock> CAPILLARIES = BLOCKS.registerBlock("capillaries",
            net.minecraft.world.level.block.NetherSproutsBlock::new, netherPlantProperties(MapColor.COLOR_RED));
    public static final DeferredBlock<net.minecraft.world.level.block.NetherSproutsBlock> ARTERIES = BLOCKS.registerBlock("arteries",
            net.minecraft.world.level.block.NetherSproutsBlock::new, netherPlantProperties(MapColor.COLOR_RED));

    private static BlockBehaviour.Properties fleshProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(0.4F)
                .sound(SoundType.NETHERRACK);
    }

    // --- Edenweed: a shrub that grows at the roots of the Hallowed Grove's Rowanwood trees (see RowanwoodTreePiece) and
    // regrows when picked. Not an item: it is found, not planted. The drying rack turns its buds into Dried Edenweed. ---
    public static final DeferredBlock<EdenweedBushBlock> EDENWEED_BUSH = BLOCKS.registerBlock("edenweed_bush",
            EdenweedBushBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.SWEET_BERRY_BUSH)
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.BenchBlock> BENCH = BLOCKS.registerBlock("bench",
            dev.zsskayr.merlins_inferno.block.BenchBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion());

    public static final DeferredBlock<DryingRackBlock> DRYING_RACK = BLOCKS.registerBlock("drying_rack",
            DryingRackBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(1.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .ignitedByLava());

    /** The placed Pandora Box (GeckoLib-animated lid). Its item is {@code ModItems.PANDORA_BOX}. */
    public static final DeferredBlock<PandoraBoxBlock> PANDORA_BOX = BLOCKS.registerBlock("pandora_box",
            PandoraBoxBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    // Chain dyed with Nether Wart - same behaviour as the vanilla one.
    public static final DeferredBlock<ChainBlock> RED_CHAIN = BLOCKS.registerBlock("red_chain",
            ChainBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN));

    // --- Black nylium, the third nylium: joins the minecraft:nylium tag (roots/fungi grow on it, see
    // NetherPlants) and grows this mod's own plants under bonemeal. ---
    public static final DeferredBlock<BlackNyliumBlock> BLACK_NYLIUM = BLOCKS.registerBlock("black_nylium",
            BlackNyliumBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(0.4F)
                    .sound(SoundType.NYLIUM)
                    .randomTicks());

    // Black nylium's twin over Blightstone, the ground of the Oblivion: it turns back into Blightstone, not netherrack.
    public static final DeferredBlock<BlightstoneNyliumBlock> BLIGHTSTONE_NYLIUM = BLOCKS.registerBlock("blightstone_nylium",
            BlightstoneNyliumBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(0.4F)
                    .sound(SoundType.NYLIUM)
                    .randomTicks());

    // --- The Oblivion Forest's fungus: crimson's stem and wart block in black, plus black roots (see TallFungusFeature). ---
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> BLACK_STEM = BLOCKS.registerBlock("black_stem",
            net.minecraft.world.level.block.RotatedPillarBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.STEM));
    public static final DeferredBlock<Block> BLACK_WART_BLOCK = BLOCKS.registerBlock("black_wart_block",
            Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0F).sound(SoundType.WART_BLOCK));
    public static final DeferredBlock<net.minecraft.world.level.block.RootsBlock> BLACK_ROOTS = BLOCKS.registerBlock("black_roots",
            net.minecraft.world.level.block.RootsBlock::new, netherPlantProperties(MapColor.COLOR_BLACK));

    // Planted by bonemeal into the Oblivion Forest's tall fungus (worldgen/configured_feature/tall_black_fungus.json).
    public static final DeferredBlock<net.minecraft.world.level.block.FungusBlock> BLACK_FUNGUS = BLOCKS.registerBlock("black_fungus",
            props -> new dev.zsskayr.merlins_inferno.block.BlackFungusBlock(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.CONFIGURED_FEATURE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("merlins_inferno", "tall_black_fungus")),
                    BLACK_NYLIUM.get(), props),
            netherPlantProperties(MapColor.COLOR_BLACK).lightLevel(state -> 3));
    // Short black grass (the texture was already in the pack as black_nylium_grass.png); the low plant of the undergrowth.
    public static final DeferredBlock<net.minecraft.world.level.block.NetherSproutsBlock> BLACK_GRASS = BLOCKS.registerBlock("black_grass",
            net.minecraft.world.level.block.NetherSproutsBlock::new, netherPlantProperties(MapColor.COLOR_BLACK));
    public static final DeferredBlock<net.minecraft.world.level.block.NetherSproutsBlock> BLACK_SPROUTS = BLOCKS.registerBlock("black_sprouts",
            net.minecraft.world.level.block.NetherSproutsBlock::new, netherPlantProperties(MapColor.COLOR_BLACK));

    // Black vines: weeping vines in black, hanging under the Oblivion Forest's fungus; and a shroomlight of the Oblivion.
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.BlackVinesBlock> BLACK_VINES = BLOCKS.registerBlock("black_vines",
            dev.zsskayr.merlins_inferno.block.BlackVinesBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).randomTicks().noCollission().instabreak()
                    .sound(SoundType.WEEPING_VINES).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<dev.zsskayr.merlins_inferno.block.BlackVinesPlantBlock> BLACK_VINES_PLANT = BLOCKS.registerBlock("black_vines_plant",
            dev.zsskayr.merlins_inferno.block.BlackVinesPlantBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).noCollission().instabreak()
                    .sound(SoundType.WEEPING_VINES).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> OBLIVION_SHROOMLIGHT = BLOCKS.registerBlock("oblivion_shroomlight",
            SoulShroomlightBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0F)
                    .sound(SoundType.SHROOMLIGHT).lightLevel(state -> 12));

    // The ground of the Cinder Dunes: a soft, dry grey ash that does not fall (unlike sand).
    public static final DeferredBlock<Block> ASH_BLOCK = BLOCKS.registerBlock("ash_block",
            Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.5F).sound(SoundType.SAND));

    public static final DeferredBlock<NetherDoublePlantBlock> TALL_CRIMSON_ROOTS = BLOCKS.registerBlock("tall_crimson_roots",
            NetherDoublePlantBlock::new, netherPlantProperties(MapColor.COLOR_RED));
    public static final DeferredBlock<NetherDoublePlantBlock> TALL_BLACK_GRASS = BLOCKS.registerBlock("tall_black_grass",
            NetherDoublePlantBlock::new, netherPlantProperties(MapColor.COLOR_BLACK));
    public static final DeferredBlock<NetherFlowerBlock> LUST_FLOWER = BLOCKS.registerBlock("lust_flower",
            props -> new NetherFlowerBlock(MobEffects.DAMAGE_BOOST, 6.0F, props), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .pushReaction(PushReaction.DESTROY));

    private static BlockBehaviour.Properties netherPlantProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .replaceable()
                .noCollission()
                .instabreak()
                .sound(SoundType.ROOTS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY);
    }

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
