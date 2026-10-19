package dev.zsskayr.merlins_inferno.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock;

/**
 * Lets pipes and tanks pour lava into the forge through a Hell Forge Vault in fuel mode: one tank
 * that only fills (never drains) and only with lava, backed straight by the forge's fuel tank
 * ({@link HellForgeBlockEntity#FUEL_PER_MB} ticks of burn time per mB, so it holds exactly one
 * bucket). Like {@link HellForgeVaultHandler}, it re-resolves the forge and the mode on every call.
 */
public final class HellForgeVaultFluidHandler implements IFluidHandler {
    private static final int CAPACITY_MB = HellForgeBlockEntity.FUEL_CAPACITY / HellForgeBlockEntity.FUEL_PER_MB;

    private final Level level;
    private final BlockPos vaultPos;

    private HellForgeVaultFluidHandler(Level level, BlockPos vaultPos) {
        this.level = level;
        this.vaultPos = vaultPos.immutable();
    }

    public static IFluidHandler of(Level level, BlockPos vaultPos) {
        return new HellForgeVaultFluidHandler(level, vaultPos);
    }

    @Nullable
    private HellForgeBlockEntity forge() {
        BlockState state = this.level.getBlockState(this.vaultPos);
        if (!(state.getBlock() instanceof HellForgeVaultBlock) || state.getValue(HellForgeVaultBlock.MODE) != HellForgeVaultBlock.Mode.FUEL) {
            return null;
        }
        return HellForgeVaultHandler.findForge(this.level, this.vaultPos);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        HellForgeBlockEntity forge = this.forge();
        return forge == null ? FluidStack.EMPTY : new FluidStack(Fluids.LAVA, forge.storedFuel / HellForgeBlockEntity.FUEL_PER_MB);
    }

    @Override
    public int getTankCapacity(int tank) {
        return CAPACITY_MB;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid().isSame(Fluids.LAVA);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        HellForgeBlockEntity forge = this.forge();
        if (forge == null || resource.isEmpty() || !this.isFluidValid(0, resource)) {
            return 0;
        }
        int accepted = Math.min(resource.getAmount(), (HellForgeBlockEntity.FUEL_CAPACITY - forge.storedFuel) / HellForgeBlockEntity.FUEL_PER_MB);
        if (accepted > 0 && action.execute()) {
            forge.storedFuel += accepted * HellForgeBlockEntity.FUEL_PER_MB;
            forge.setChanged();
        }
        return Math.max(accepted, 0);
    }

    /** The tank only fills: lava is burned, not stored to be taken back out. */
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
