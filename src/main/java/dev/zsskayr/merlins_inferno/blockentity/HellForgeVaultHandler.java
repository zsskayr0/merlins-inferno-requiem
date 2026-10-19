package dev.zsskayr.merlins_inferno.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import dev.zsskayr.merlins_inferno.block.HellForgeControllerBlock;
import dev.zsskayr.merlins_inferno.block.HellForgeCoreBlock;
import dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock;

/**
 * What a hopper or pipe sees when it touches a {@link HellForgeVaultBlock}: a single slot that
 * maps onto one slot of the forge's Controller, picked by the vault's mode - input and fuel only
 * accept, output only gives out (a fuel vault also gives back the empty bucket). Does nothing
 * while the structure is not formed.
 * <p>
 * The handler never keeps the Controller or the mode: every call looks both up again. Pipe mods and
 * cable managers cache capabilities, and a cache is only invalidated when the vault's own block
 * changes - not when the Core/Controller around it is placed, broken or replaced - so anything
 * captured at lookup time could go stale.
 */
public final class HellForgeVaultHandler implements IItemHandler {
    /** How many items one automatic transfer moves at most. */
    private static final int AUTO_AMOUNT = 8;
    /** How much lava (mB) one automatic transfer pulls from an adjacent tank at most. */
    private static final int AUTO_FLUID_MB = 250;

    private final Level level;
    private final BlockPos vaultPos;

    private HellForgeVaultHandler(Level level, BlockPos vaultPos) {
        this.level = level;
        this.vaultPos = vaultPos.immutable();
    }

    /** Capability provider: always hands out a handler; it simply refuses everything while not formed. */
    public static IItemHandler of(Level level, BlockPos vaultPos) {
        return new HellForgeVaultHandler(level, vaultPos);
    }

    @Nullable
    private HellForgeBlockEntity forge() {
        return findForge(this.level, this.vaultPos);
    }

    /** The formed Controller the vault at {@code vaultPos} currently belongs to, or {@code null}. */
    @Nullable
    static HellForgeBlockEntity findForge(Level level, BlockPos vaultPos) {
        for (Direction toCore : Direction.values()) {
            BlockPos corePos = vaultPos.relative(toCore);
            if (!(level.getBlockState(corePos).getBlock() instanceof HellForgeCoreBlock)) {
                continue;
            }
            // A vault is a face centre, so the core is always the one neighbour towards the middle;
            // the Controller then sits on the core's far side facing outwards.
            for (Direction toController : Direction.Plane.HORIZONTAL) {
                BlockPos controllerPos = corePos.relative(toController);
                BlockState controller = level.getBlockState(controllerPos);
                if (controller.getBlock() instanceof HellForgeControllerBlock
                        && controller.getValue(HellForgeControllerBlock.FACING) == toController
                        && level.getBlockEntity(controllerPos) instanceof HellForgeBlockEntity forge
                        && forge.isFormed()) {
                    return forge;
                }
            }
        }
        return null;
    }

    @Nullable
    private HellForgeVaultBlock.Mode mode() {
        BlockState state = this.level.getBlockState(this.vaultPos);
        return state.getBlock() instanceof HellForgeVaultBlock ? state.getValue(HellForgeVaultBlock.MODE) : null;
    }

    private static int slotFor(HellForgeVaultBlock.Mode mode) {
        return switch (mode) {
            case INPUT -> HellForgeBlockEntity.INPUT_SLOT;
            case FUEL -> HellForgeBlockEntity.FUEL_SLOT;
            case OUTPUT -> HellForgeBlockEntity.RESULT_SLOT;
        };
    }

    /**
     * One automatic transfer for an {@link HellForgeVaultBlock#AUTO} vault: in output mode the
     * forge's result goes into the first adjacent inventory that takes it; in input/fuel mode the
     * first adjacent inventory with something the forge accepts is drained into it. The other
     * Hell Forge vaults are never treated as an adjacent inventory.
     */
    public static void autoTransfer(ServerLevel level, BlockPos vaultPos, BlockState vaultState) {
        IItemHandler own = of(level, vaultPos);
        HellForgeVaultBlock.Mode mode = vaultState.getValue(HellForgeVaultBlock.MODE);
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = vaultPos.relative(dir);
            if (level.getBlockState(neighborPos).getBlock() instanceof HellForgeVaultBlock) {
                continue;
            }
            IItemHandler neighbor = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
            if (neighbor != null) {
                // A fuel vault first hands the empty bucket back out, then takes the next fuel in.
                boolean moved = mode == HellForgeVaultBlock.Mode.OUTPUT ? move(own, neighbor)
                        : (mode == HellForgeVaultBlock.Mode.FUEL && move(own, neighbor)) || move(neighbor, own);
                if (moved) {
                    return;
                }
            }
            if (mode == HellForgeVaultBlock.Mode.FUEL) {
                IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
                if (tank != null && FluidUtil.tryFluidTransfer(HellForgeVaultFluidHandler.of(level, vaultPos), tank, AUTO_FLUID_MB, true).getAmount() > 0) {
                    return;
                }
            }
        }
    }

    /** Moves up to {@link #AUTO_AMOUNT} items from the first slot of {@code from} that {@code to} accepts. */
    private static boolean move(IItemHandler from, IItemHandler to) {
        for (int slot = 0; slot < from.getSlots(); slot++) {
            ItemStack offered = from.extractItem(slot, AUTO_AMOUNT, true);
            if (offered.isEmpty()) {
                continue;
            }
            ItemStack leftover = ItemHandlerHelper.insertItem(to, offered, true);
            int moved = offered.getCount() - leftover.getCount();
            if (moved <= 0) {
                continue;
            }
            ItemStack taken = from.extractItem(slot, moved, false);
            if (!taken.isEmpty()) {
                ItemHandlerHelper.insertItem(to, taken, false);
                return true;
            }
        }
        return false;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        HellForgeBlockEntity forge = this.forge();
        HellForgeVaultBlock.Mode mode = this.mode();
        return forge == null || mode == null ? ItemStack.EMPTY : forge.getItem(slotFor(mode));
    }

    @Override
    public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
        HellForgeBlockEntity forge = this.forge();
        HellForgeVaultBlock.Mode mode = this.mode();
        if (forge == null || mode == null || mode == HellForgeVaultBlock.Mode.OUTPUT || stack.isEmpty()) {
            return stack;
        }
        int slot = slotFor(mode);
        if (!forge.canPlaceItem(slot, stack)) {
            return stack;
        }
        ItemStack existing = forge.getItem(slot);
        int limit = Math.min(forge.getMaxStackSize(), stack.getMaxStackSize());
        if (existing.isEmpty()) {
            int moved = Math.min(limit, stack.getCount());
            if (!simulate) {
                forge.setItem(slot, stack.copyWithCount(moved));
            }
            return moved == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
        }
        if (!ItemStack.isSameItemSameComponents(existing, stack)) {
            return stack;
        }
        int moved = Math.min(limit - existing.getCount(), stack.getCount());
        if (moved <= 0) {
            return stack;
        }
        if (!simulate) {
            existing.grow(moved);
            forge.setChanged();
        }
        return moved == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
    }

    @Override
    public ItemStack extractItem(int index, int amount, boolean simulate) {
        HellForgeBlockEntity forge = this.forge();
        HellForgeVaultBlock.Mode mode = this.mode();
        if (forge == null || mode == null || mode == HellForgeVaultBlock.Mode.INPUT || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int slot = slotFor(mode);
        ItemStack existing = forge.getItem(slot);
        // Output vaults give out the result; fuel vaults only give back the empty bucket a lava bucket leaves.
        if (existing.isEmpty() || (mode == HellForgeVaultBlock.Mode.FUEL && !existing.is(Items.BUCKET))) {
            return ItemStack.EMPTY;
        }
        int taken = Math.min(amount, existing.getCount());
        ItemStack result = existing.copyWithCount(taken);
        if (!simulate) {
            forge.removeItem(slot, taken);
        }
        return result;
    }

    @Override
    public int getSlotLimit(int index) {
        return 64;
    }

    @Override
    public boolean isItemValid(int index, ItemStack stack) {
        HellForgeBlockEntity forge = this.forge();
        HellForgeVaultBlock.Mode mode = this.mode();
        return forge != null && mode != null && mode != HellForgeVaultBlock.Mode.OUTPUT && forge.canPlaceItem(slotFor(mode), stack);
    }
}
