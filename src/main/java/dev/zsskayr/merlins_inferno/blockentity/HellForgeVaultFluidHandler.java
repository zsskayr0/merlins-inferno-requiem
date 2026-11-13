package dev.zsskayr.merlins_inferno.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock;
import dev.zsskayr.merlins_inferno.forge.ForgeThermalData;

/**
 * Lets pipes and tanks pour lava into the forge through a Hell Forge Vault in fuel mode: one tank
 * that only fills (never drains) and only with a fluid some fuel rule names, backed straight by the forge's fuel tank
 * ({@link HellForgeBlockEntity#FUEL_PER_MB} ticks of burn time per mB, so it holds exactly one
 * bucket). Like {@link HellForgeVaultHandler}, it re-resolves the forge and the mode on every call.
 */
public final class HellForgeVaultFluidHandler implements IFluidHandler {
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
        if (forge == null || forge.fuelFluid() == null) return FluidStack.EMPTY;
        return new FluidStack(forge.fuelFluid(), forge.fluidAmount());
    }

    @Override
    public int getTankCapacity(int tank) {
        HellForgeBlockEntity forge = this.forge();
        return forge == null ? HellForgeBlockEntity.FUEL_CAPACITY / HellForgeBlockEntity.FUEL_PER_MB : forge.fluidCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (stack.getFluid().isSame(Fluids.LAVA)) return true;
        return ForgeThermalData.fuelForFluid(this.level, stack.getFluid()) != null;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        HellForgeBlockEntity forge = this.forge();
        if (forge == null || resource.isEmpty() || !this.isFluidValid(0, resource)) {
            return 0;
        }
        return forge.fillFluid(resource.getFluid(), resource.getAmount(), action.execute());
    }

    /** A smelting forge's tank only fills; a Magmatic forge gives out what lies above its reserve. */
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        HellForgeBlockEntity forge = this.forge();
        if (forge == null || forge.fuelFluid() == null || !resource.getFluid().isSame(forge.fuelFluid())) return FluidStack.EMPTY;
        return this.drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        HellForgeBlockEntity forge = this.forge();
        if (forge == null || forge.fuelFluid() == null) return FluidStack.EMPTY;
        var fluid = forge.fuelFluid();
        int taken = forge.drainFluid(maxDrain, action.simulate());
        return taken <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, taken);
    }
}
