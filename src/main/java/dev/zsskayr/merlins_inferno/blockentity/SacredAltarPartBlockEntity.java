package dev.zsskayr.merlins_inferno.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.block.SacredAltarPartBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;

/** The part's only memory: where the altar it belongs to stands - see {@link SacredAltarPartBlock}. */
public class SacredAltarPartBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos corePos;

    public SacredAltarPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SACRED_ALTAR_PART.get(), pos, state);
    }

    public void setCorePos(BlockPos corePos) {
        this.corePos = corePos;
        this.setChanged();
    }

    @Nullable
    public BlockPos getCorePos() {
        return this.corePos;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.corePos != null) {
            tag.putLong("CorePos", this.corePos.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.corePos = tag.contains("CorePos") ? BlockPos.of(tag.getLong("CorePos")) : null;
    }
}
