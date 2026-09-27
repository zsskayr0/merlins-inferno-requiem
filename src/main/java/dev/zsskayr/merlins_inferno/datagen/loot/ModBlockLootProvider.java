package dev.zsskayr.merlins_inferno.datagen.loot;

import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Block loot tables for every block this mod adds. Without one, breaking the block drops
 * nothing at all.
 * <p>
 * {@code rowanwood_log} is NOT here - it's hand-authored directly at
 * {@code data/merlins_inferno/loot_table/blocks/rowanwood_log.json} instead, since it's gated
 * behind "Toque do Druida" (see {@code block.RowanwoodLogBlock}), a custom, data-driven
 * enchantment this mod itself adds. Datagen's {@code HolderLookup.Provider} for loot tables only
 * has vanilla's own registries bootstrapped - it can't resolve
 * {@code merlins_inferno:druids_touch}, so building an {@code EnchantmentPredicate} against it
 * here throws "Missing element" and crashes the whole {@code runData} task (this mod's own custom
 * enchantments are hand-authored for the exact same reason - see
 * {@code data/merlins_inferno/enchantment/*.json}).
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
                ModBlocks.STRIPPED_ASHWOOD_LOG.get(), ModBlocks.STRIPPED_ASHWOOD_WOOD.get(), ModBlocks.ASHWOOD_FENCE_GATE.get(),
                ModBlocks.ASHWOOD_DOOR.get(), ModBlocks.ASHWOOD_TRAPDOOR.get(), ModBlocks.ASHWOOD_PRESSURE_PLATE.get(),
                ModBlocks.ASHWOOD_BUTTON.get(), ModBlocks.ASHWOOD_SIGN.get(), ModBlocks.ASHWOOD_HANGING_SIGN.get(),
                ModBlocks.ROWANWOOD_LEAVES.get(),
                ModBlocks.COMPRESSED_NETHERRACK.get(), ModBlocks.COMPRESSED_NETHERRACK_STAIRS.get(),
                ModBlocks.COMPRESSED_NETHERRACK_SLAB.get(), ModBlocks.COMPRESSED_NETHERRACK_WALL.get(),
                ModBlocks.HELL_FORGE.get(),
                ModBlocks.LYRIUM_ORE.get(), ModBlocks.DEEPSLATE_LYRIUM_ORE.get(), ModBlocks.LYRIUM_BLOCK.get());
        // lyrium_cluster is hand-authored (data/merlins_inferno/loot_table/blocks/lyrium_cluster.json).
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
        dropSelf(ModBlocks.STRIPPED_ASHWOOD_LOG.get());
        dropSelf(ModBlocks.STRIPPED_ASHWOOD_WOOD.get());
        dropSelf(ModBlocks.ASHWOOD_FENCE_GATE.get());
        add(ModBlocks.ASHWOOD_DOOR.get(), createDoorTable(ModBlocks.ASHWOOD_DOOR.get()));
        dropSelf(ModBlocks.ASHWOOD_TRAPDOOR.get());
        dropSelf(ModBlocks.ASHWOOD_PRESSURE_PLATE.get());
        dropSelf(ModBlocks.ASHWOOD_BUTTON.get());
        // The wall variants drop through these (see ModBlocks#wallSignProperties).
        dropSelf(ModBlocks.ASHWOOD_SIGN.get());
        dropSelf(ModBlocks.ASHWOOD_HANGING_SIGN.get());
        add(ModBlocks.ASHWOOD_LEAVES.get(),
                createLeavesDrops(ModBlocks.ASHWOOD_LEAVES.get(), ModBlocks.ASHWOOD_SAPLING.get(), LEAVES_SAPLING_CHANCES));

        // rowanwood_log: see class javadoc - hand-authored, not here.

        // No sapling on purpose: Rowanwood's shape is a 48x25x39 hand-placed structure paste (see
        // ModTreeProvider), not something a bonemealed sapling could reasonably grow - letting
        // players farm the landmark tree would be wildly overpowered. Leaves just drop nothing
        // without Silk Touch/Shears (no sapling item to drop instead).
        add(ModBlocks.ROWANWOOD_LEAVES.get(), createSilkTouchOnlyTable(ModBlocks.ROWANWOOD_LEAVES.get()));

        dropSelf(ModBlocks.COMPRESSED_NETHERRACK.get());
        dropSelf(ModBlocks.COMPRESSED_NETHERRACK_STAIRS.get());
        add(ModBlocks.COMPRESSED_NETHERRACK_SLAB.get(), createSlabItemTable(ModBlocks.COMPRESSED_NETHERRACK_SLAB.get()));
        dropSelf(ModBlocks.COMPRESSED_NETHERRACK_WALL.get());
        // Ores drop Raw Lyrium (with Fortune) or themselves under Silk Touch, like emerald ore.
        add(ModBlocks.LYRIUM_ORE.get(), createOreDrop(ModBlocks.LYRIUM_ORE.get(), ModItems.LYRIUM_RAW.get()));
        add(ModBlocks.DEEPSLATE_LYRIUM_ORE.get(), createOreDrop(ModBlocks.DEEPSLATE_LYRIUM_ORE.get(), ModItems.LYRIUM_RAW.get()));
        dropSelf(ModBlocks.LYRIUM_BLOCK.get());
        dropSelf(ModBlocks.HELL_FORGE.get());

    }
}
