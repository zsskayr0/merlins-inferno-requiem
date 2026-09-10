package dev.zsskayr.merlins_inferno.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.block.HellForgePartBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;

/**
 * The dumb "shell" half of the Hell Forge multiblock - see {@link HellForgePartBlock}'s javadoc.
 * Only carries a back-reference to the real block's position so a part knows which structure to
 * tear down (and where to drop the item) when it's the one that gets broken.
 */
public class HellForgePartBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos corePos;

    public HellForgePartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.HELL_FORGE_PART.get(), pos, state);
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
