package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.block.HellForgeControllerBlock;
import dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Inventory migration and fuel locking regressions. These tests require an explicitly launched GameTest server. */
@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class HellForgeGameTests {
    private static HellForgeBlockEntity forge(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(4, 2, 4));
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1)))
            level.setBlock(pos, ModBlocks.COMPRESSED_NETHERRACK.get().defaultBlockState(), 3);
        level.setBlock(centre, ModBlocks.DEMONIC_CORE.get().defaultBlockState(), 3);
        BlockPos controller = centre.north();
        level.setBlock(controller, ModBlocks.HELL_FORGE_CONTROLLER.get().defaultBlockState()
                .setValue(HellForgeControllerBlock.FACING, Direction.NORTH), 3);
        var forge = (HellForgeBlockEntity) level.getBlockEntity(controller);
        forge.refreshStructure();
        helper.assertTrue(forge.isFormed(), "The original 3x3x3 layout forms");
        return forge;
    }
    @GameTest(template = "bench_empty", batch = "hell_forge")
    public static void fuelTypeMustBeEmptiedBeforeSwitching(GameTestHelper helper) {
        var forge = forge(helper);
        forge.setItem(HellForgeBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        HellForgeBlockEntity.serverTick(helper.getLevel(), forge.getBlockPos(), forge.getBlockState(), forge);
        helper.assertTrue(forge.getItem(HellForgeBlockEntity.FUEL_SLOT).isEmpty(), "Coal feeds the reserve");
        helper.assertTrue(forge.fillLava(1000, true) == 0, "Simulated pipe input rejects lava while Coal remains");
        helper.assertTrue(forge.fillLava(1000, false) == 0, "Executed pipe input rejects lava while Coal remains");
        forge.dumpFuel();
        helper.assertTrue(forge.fillLava(1000, true) == 1000 && forge.lavaAmount() == 0, "Simulation does not mutate fuel");
        helper.assertTrue(forge.fillLava(1000, false) == 1000, "Empty reserve accepts Lava");
        helper.assertTrue(forge.lavaAmount() == 1000, "Lava amount is preserved");
        helper.succeed();
    }
    @GameTest(template = "bench_empty", batch = "hell_forge")
    public static void oldInventoryAndFuelSurviveMigration(GameTestHelper helper) {
        var forge = forge(helper);
        forge.setItem(0, new ItemStack(Items.RAW_IRON, 12));
        forge.setItem(1, new ItemStack(Items.LAVA_BUCKET));
        forge.setItem(2, new ItemStack(Items.IRON_INGOT, 7));
        CompoundTag tag = forge.saveWithoutMetadata(helper.getLevel().registryAccess());
        tag.putInt("StoredFuel", 1200);
        tag.putInt("CookTime", 20);
        tag.remove("FuelType");
        tag.remove("Temperature");
        for (int lane = 0; lane < 9; lane++) tag.remove("Lane" + lane);
        forge.loadWithComponents(tag, helper.getLevel().registryAccess());
        forge.refreshStructure();
        helper.assertTrue(forge.getContainerSize() == 22, "All new channels and residual outputs exist");
        helper.assertTrue(forge.getItem(0).getCount() == 12 && forge.getItem(1).is(Items.LAVA_BUCKET)
                && forge.getItem(2).getCount() == 7, "Legacy inventory retains exact slots and counts");
        helper.assertTrue(forge.lavaAmount() == 60, "Legacy fuel migrates to Lava-equivalent energy");
        helper.succeed();
    }
    @GameTest(template = "bench_empty", batch = "hell_forge")
    public static void dormantChannelsRemainRecoverable(GameTestHelper helper) {
        var forge = forge(helper);
        int input = HellForgeBlockEntity.inputSlot(8);
        int output = HellForgeBlockEntity.outputSlot(8);
        forge.setItem(input, new ItemStack(Items.RAW_GOLD, 17));
        forge.setItem(output, new ItemStack(Items.GOLD_INGOT, 11));
        CompoundTag saved = forge.saveWithoutMetadata(helper.getLevel().registryAccess());
        forge.loadWithComponents(saved, helper.getLevel().registryAccess());
        forge.refreshStructure();
        helper.assertTrue(forge.getProcessingSlots() == 1, "Small forge exposes one active channel");
        helper.assertTrue(forge.removeItem(input, 64).getCount() == 17, "Dormant input is recoverable");
        helper.assertTrue(forge.removeItem(output, 64).getCount() == 11, "Dormant output is recoverable");
        helper.succeed();
    }
    private static void restoreThermalState(GameTestHelper helper, HellForgeBlockEntity forge,
                                            double temperature, double progress, int exposure) {
        CompoundTag saved = forge.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.putDouble("Temperature", temperature);
        CompoundTag lane = saved.getCompound("Lane0");
        lane.putDouble("Progress", progress);
        lane.putInt("HeatedCount", forge.getItem(0).getCount());
        lane.putInt("Critical", exposure);
        lane.putInt("Overheat", 100);
        forge.loadWithComponents(saved, helper.getLevel().registryAccess());
        forge.refreshStructure();
    }
    @GameTest(template = "bench_empty", batch = "hell_forge")
    public static void coldInputsPreserveProgressAndItemComponents(GameTestHelper helper) {
        var forge = forge(helper);
        forge.setItem(0, new ItemStack(Items.RAW_IRON, 2));
        restoreThermalState(helper, forge, 473.15, 40, 0);
        HellForgeBlockEntity.serverTick(helper.getLevel(), forge.getBlockPos(), forge.getBlockState(), forge);
        var saved = forge.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(saved.getCompound("Lane0").getDouble("Progress") == 40, "Below-minimum heat preserves progress");
        helper.assertTrue(ItemStack.isSameItemSameComponents(forge.getItem(0), new ItemStack(Items.RAW_IRON)),
                "Heating never adds components that interfere with stacking or automation");
        helper.succeed();
    }
    @GameTest(template = "bench_empty", batch = "hell_forge")
    public static void criticalExposureConvertsOnlyOneIngredient(GameTestHelper helper) {
        var forge = forge(helper);
        forge.setItem(0, new ItemStack(Items.RAW_IRON, 2));
        restoreThermalState(helper, forge, 2500, 40, 399);
        HellForgeBlockEntity.serverTick(helper.getLevel(), forge.getBlockPos(), forge.getBlockState(), forge);
        helper.assertTrue(forge.getItem(0).getCount() == 1, "Only one ingredient is damaged at the exposure threshold");
        helper.assertTrue(forge.getItem(HellForgeBlockEntity.SCRAP_SLOT).is(
                dev.zsskayr.merlins_inferno.registry.ModItems.FORGED_SCRAP.get()), "Damaged ingredient becomes Forged Scrap");
        var saved = forge.saveWithoutMetadata(helper.getLevel().registryAccess()).getCompound("Lane0");
        helper.assertTrue(saved.getInt("Critical") == 0 && saved.getDouble("Progress") == 0,
                "The next ingredient gets a fresh exposure window");
        helper.succeed();
    }
    @GameTest(template = "bench_empty", batch = "hell_forge")
    public static void fullScrapOutputCannotDeleteInputs(GameTestHelper helper) {
        var forge = forge(helper);
        forge.setItem(0, new ItemStack(Items.RAW_IRON, 2));
        forge.setItem(HellForgeBlockEntity.SCRAP_SLOT,
                new ItemStack(dev.zsskayr.merlins_inferno.registry.ModItems.FORGED_SCRAP.get(), 64));
        restoreThermalState(helper, forge, 2500, 40, 399);
        HellForgeBlockEntity.serverTick(helper.getLevel(), forge.getBlockPos(), forge.getBlockState(), forge);
        helper.assertTrue(forge.getItem(0).getCount() == 2, "A blocked scrap output preserves ingredients");
        helper.assertTrue(forge.getItem(HellForgeBlockEntity.SCRAP_SLOT).getCount() == 64, "Scrap stack never overflows");
        helper.succeed();
    }
}
