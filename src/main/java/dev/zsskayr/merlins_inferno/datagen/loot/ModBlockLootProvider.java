package dev.zsskayr.merlins_inferno.datagen.loot;

import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import dev.zsskayr.merlins_inferno.block.GreatDoorBlock;
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
        java.util.List<Block> known = new java.util.ArrayList<>(List.of(
                ModBlocks.ASHWOOD_LOG.get(), ModBlocks.ASHWOOD_WOOD.get(), ModBlocks.ASHWOOD_PLANKS.get(),
                ModBlocks.ASHWOOD_LEAVES.get(), ModBlocks.ASHWOOD_SAPLING.get(), ModBlocks.ASHWOOD_STAIRS.get(),
                ModBlocks.ASHWOOD_SLAB.get(), ModBlocks.ASHWOOD_FENCE.get(),
                ModBlocks.STRIPPED_ASHWOOD_LOG.get(), ModBlocks.STRIPPED_ASHWOOD_WOOD.get(), ModBlocks.ASHWOOD_FENCE_GATE.get(),
                ModBlocks.ASHWOOD_DOOR.get(), ModBlocks.ASHWOOD_TRAPDOOR.get(), ModBlocks.ASHWOOD_PRESSURE_PLATE.get(),
                ModBlocks.ASHWOOD_BUTTON.get(), ModBlocks.ASHWOOD_SIGN.get(), ModBlocks.ASHWOOD_HANGING_SIGN.get(),
                ModBlocks.ROWANWOOD_LEAVES.get(),
                ModBlocks.COMPRESSED_NETHERRACK.get(), ModBlocks.COMPRESSED_NETHERRACK_STAIRS.get(),
                ModBlocks.COMPRESSED_NETHERRACK_SLAB.get(), ModBlocks.COMPRESSED_NETHERRACK_WALL.get(),
                ModBlocks.BLIGHTSTONE.get(), ModBlocks.BLIGHTSTONE_STAIRS.get(), ModBlocks.BLIGHTSTONE_SLAB.get(),
                ModBlocks.BLIGHTSTONE_WALL.get(), ModBlocks.BLIGHTSTONE_BRICKS.get(), ModBlocks.BLIGHTSTONE_BRICK_STAIRS.get(),
                ModBlocks.BLIGHTSTONE_BRICK_SLAB.get(), ModBlocks.BLIGHTSTONE_BRICK_WALL.get(),
                ModBlocks.POLISHED_BLIGHTSTONE.get(), ModBlocks.POLISHED_BLIGHTSTONE_STAIRS.get(),
                ModBlocks.POLISHED_BLIGHTSTONE_SLAB.get(), ModBlocks.POLISHED_BLIGHTSTONE_WALL.get(),
                ModBlocks.CHISELED_POLISHED_BLIGHTSTONE.get(),
                ModBlocks.BLIGHTSTONE_SMALL_BRICKS.get(), ModBlocks.BLIGHTSTONE_SMALL_BRICK_STAIRS.get(),
                ModBlocks.BLIGHTSTONE_SMALL_BRICK_SLAB.get(), ModBlocks.BLIGHTSTONE_SMALL_BRICK_WALL.get(),
                ModBlocks.CRACKED_BLIGHTSTONE_BRICKS.get(),
                ModBlocks.ARGANITE.get(),
                ModBlocks.ARGANITE_STAIRS.get(),
                ModBlocks.ARGANITE_WALL.get(),
                ModBlocks.ARGANITE_BRICKS.get(),
                ModBlocks.ARGANITE_BRICKS_STAIRS.get(),
                ModBlocks.ARGANITE_BRICKS_WALL.get(),
                ModBlocks.QUARTZITE.get(),
                ModBlocks.PORCELAIN.get(),
                ModBlocks.PORCELAIN_STAIRS.get(),
                ModBlocks.POLISHED_PORCELAIN.get(),
                ModBlocks.POLISHED_PORCELAIN_STAIRS.get(),
                ModBlocks.TILED_PORCELAIN.get(),
                ModBlocks.ARGANITE_SLAB.get(),
                ModBlocks.ARGANITE_BRICKS_SLAB.get(),
                ModBlocks.PORCELAIN_SLAB.get(),
                ModBlocks.POLISHED_PORCELAIN_SLAB.get(),
                ModBlocks.HELL_FORGE_CONTROLLER.get(), ModBlocks.DEMONIC_CORE.get(), ModBlocks.HELL_FORGE_VAULT.get(),
                ModBlocks.LYRIUM_BLOCK.get(),
                ModBlocks.CORRUPTED_OBSIDIAN.get(), ModBlocks.PETRIFIED_SKULL.get(), ModBlocks.WITHERED_OBSIDIAN.get(),
                ModBlocks.FLESH_BLOCK.get(), ModBlocks.MOUTH_BLOCK.get(), ModBlocks.VISCERA.get(), ModBlocks.FLESH_SLAB.get(), ModBlocks.FLESH_CARPET.get(), ModBlocks.RED_CHAIN.get(),
                ModBlocks.BLACK_NYLIUM.get(), ModBlocks.BLIGHTSTONE_NYLIUM.get(), ModBlocks.TALL_CRIMSON_ROOTS.get(), ModBlocks.TALL_BLACK_GRASS.get(),
                ModBlocks.LUST_FLOWER.get(),
                ModBlocks.SKIN.get(), ModBlocks.SPINE.get()));
        ModBlocks.GREAT_DOORS.forEach(door -> known.add(door.get()));
        return known;
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
        // One item for the whole 3 x 3 door: only the bottom middle cell drops it (the others hand it over, see GreatDoorBlock).
        for (var door : ModBlocks.GREAT_DOORS) {
            Block block = door.get();
            add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(block)
                            .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                    .setProperties(StatePropertiesPredicate.Builder.properties()
                                            .hasProperty(GreatDoorBlock.COL, 1).hasProperty(GreatDoorBlock.ROW, 0)))))));
        }
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
        dropSelf(ModBlocks.BLIGHTSTONE.get());
        dropSelf(ModBlocks.BLIGHTSTONE_STAIRS.get());
        add(ModBlocks.BLIGHTSTONE_SLAB.get(), createSlabItemTable(ModBlocks.BLIGHTSTONE_SLAB.get()));
        dropSelf(ModBlocks.BLIGHTSTONE_WALL.get());
        dropSelf(ModBlocks.BLIGHTSTONE_BRICKS.get());
        dropSelf(ModBlocks.BLIGHTSTONE_BRICK_STAIRS.get());
        add(ModBlocks.BLIGHTSTONE_BRICK_SLAB.get(), createSlabItemTable(ModBlocks.BLIGHTSTONE_BRICK_SLAB.get()));
        dropSelf(ModBlocks.BLIGHTSTONE_BRICK_WALL.get());
        dropSelf(ModBlocks.POLISHED_BLIGHTSTONE.get());
        dropSelf(ModBlocks.POLISHED_BLIGHTSTONE_STAIRS.get());
        add(ModBlocks.POLISHED_BLIGHTSTONE_SLAB.get(), createSlabItemTable(ModBlocks.POLISHED_BLIGHTSTONE_SLAB.get()));
        dropSelf(ModBlocks.POLISHED_BLIGHTSTONE_WALL.get());
        dropSelf(ModBlocks.CHISELED_POLISHED_BLIGHTSTONE.get());
        dropSelf(ModBlocks.BLIGHTSTONE_SMALL_BRICKS.get());
        dropSelf(ModBlocks.BLIGHTSTONE_SMALL_BRICK_STAIRS.get());
        add(ModBlocks.BLIGHTSTONE_SMALL_BRICK_SLAB.get(), createSlabItemTable(ModBlocks.BLIGHTSTONE_SMALL_BRICK_SLAB.get()));
        dropSelf(ModBlocks.BLIGHTSTONE_SMALL_BRICK_WALL.get());
        dropSelf(ModBlocks.CRACKED_BLIGHTSTONE_BRICKS.get());
        dropSelf(ModBlocks.ARGANITE.get());
        dropSelf(ModBlocks.ARGANITE_STAIRS.get());
        dropSelf(ModBlocks.ARGANITE_WALL.get());
        dropSelf(ModBlocks.ARGANITE_BRICKS.get());
        dropSelf(ModBlocks.ARGANITE_BRICKS_STAIRS.get());
        dropSelf(ModBlocks.ARGANITE_BRICKS_WALL.get());
        dropSelf(ModBlocks.QUARTZITE.get());
        dropSelf(ModBlocks.PORCELAIN.get());
        dropSelf(ModBlocks.PORCELAIN_STAIRS.get());
        dropSelf(ModBlocks.POLISHED_PORCELAIN.get());
        dropSelf(ModBlocks.POLISHED_PORCELAIN_STAIRS.get());
        dropSelf(ModBlocks.TILED_PORCELAIN.get());
        add(ModBlocks.ARGANITE_SLAB.get(), createSlabItemTable(ModBlocks.ARGANITE_SLAB.get()));
        add(ModBlocks.ARGANITE_BRICKS_SLAB.get(), createSlabItemTable(ModBlocks.ARGANITE_BRICKS_SLAB.get()));
        add(ModBlocks.PORCELAIN_SLAB.get(), createSlabItemTable(ModBlocks.PORCELAIN_SLAB.get()));
        add(ModBlocks.POLISHED_PORCELAIN_SLAB.get(), createSlabItemTable(ModBlocks.POLISHED_PORCELAIN_SLAB.get()));
        // quartz_cluster is hand-authored (data/merlins_inferno/loot_table/blocks/quartz_cluster.json).
        dropSelf(ModBlocks.CORRUPTED_OBSIDIAN.get());
        dropSelf(ModBlocks.PETRIFIED_SKULL.get());
        dropSelf(ModBlocks.WITHERED_OBSIDIAN.get());
        dropSelf(ModBlocks.FLESH_BLOCK.get());
        dropSelf(ModBlocks.MOUTH_BLOCK.get());
        add(ModBlocks.VISCERA.get(), noDrop());
        add(ModBlocks.FLESH_SLAB.get(), createSlabItemTable(ModBlocks.FLESH_SLAB.get()));
        dropSelf(ModBlocks.FLESH_CARPET.get());
        dropSelf(ModBlocks.SPINE.get());
        add(ModBlocks.SKIN.get(), createSingleItemTableWithSilkTouch(ModBlocks.SKIN.get(), Blocks.NETHERRACK));
        dropSelf(ModBlocks.RED_CHAIN.get());
        dropSelf(ModBlocks.LUST_FLOWER.get());
        // Like crimson nylium: netherrack unless Silk Touch.
        add(ModBlocks.BLACK_NYLIUM.get(), createSingleItemTableWithSilkTouch(ModBlocks.BLACK_NYLIUM.get(), Blocks.NETHERRACK));
        add(ModBlocks.BLIGHTSTONE_NYLIUM.get(), createSingleItemTableWithSilkTouch(ModBlocks.BLIGHTSTONE_NYLIUM.get(), ModBlocks.BLIGHTSTONE.get()));
        // Only the lower half drops, so a plant isn't refunded twice.
        add(ModBlocks.TALL_CRIMSON_ROOTS.get(), createSinglePropConditionTable(ModBlocks.TALL_CRIMSON_ROOTS.get(), DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        add(ModBlocks.TALL_BLACK_GRASS.get(), createSinglePropConditionTable(ModBlocks.TALL_BLACK_GRASS.get(), DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        // Ores drop Raw Lyrium (with Fortune) or themselves under Silk Touch, like emerald ore.
        dropSelf(ModBlocks.LYRIUM_BLOCK.get());
        dropSelf(ModBlocks.HELL_FORGE_CONTROLLER.get());
        dropSelf(ModBlocks.DEMONIC_CORE.get());
        dropSelf(ModBlocks.HELL_FORGE_VAULT.get());

    }
}
