package dev.zsskayr.merlins_inferno.blockentity;

import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** How many things a Mouth Block has eaten since it last woke up, and until when it stays full. */
public class MouthBlockEntity extends BlockEntity {
    private int fed;
    private long fullUntil;

    public MouthBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.MOUTH_BLOCK.get(), pos, state);
    }

    public int fed() {
        return fed;
    }

    public void addFed() {
        fed++;
        setChanged();
    }

    public long fullUntil() {
        return fullUntil;
    }

    public void sate(long until) {
        fullUntil = until;
        setChanged();
    }

    public void wake() {
        fed = 0;
        fullUntil = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fed", fed);
        tag.putLong("FullUntil", fullUntil);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fed = tag.getInt("Fed");
        fullUntil = tag.getLong("FullUntil");
    }
}
