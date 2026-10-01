package dev.zsskayr.merlins_inferno.blockentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.block.DryingRackBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/** Four hanging slots, each empty, drying (with a timer) or dry. */
public class DryingRackBlockEntity extends BlockEntity {
    public static final int SLOTS = 4;
    /** Two minutes for a bud to dry. */
    public static final int DRY_TICKS = 2400;

    private static final int EMPTY = 0;
    private static final int DRYING = 1;
    private static final int READY = 2;

    private final int[] states = new int[SLOTS];
    private final int[] progress = new int[SLOTS];

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.DRYING_RACK.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity rack) {
        boolean changed = false;
        for (int i = 0; i < SLOTS; i++) {
            if (rack.states[i] == DRYING && ++rack.progress[i] >= DRY_TICKS) {
                rack.states[i] = READY;
                changed = true;
            }
        }
        if (changed) {
            rack.setChanged();
            rack.refreshState();
        }
    }

    /** Hangs one bud in the first free slot; false when the rack is full. */
    public boolean hang() {
        for (int i = 0; i < SLOTS; i++) {
            if (this.states[i] == EMPTY) {
                this.states[i] = DRYING;
                this.progress[i] = 0;
                this.setChanged();
                return true;
            }
        }
        return false;
    }

    public boolean hasReady() {
        for (int state : this.states) {
            if (state == READY) {
                return true;
            }
        }
        return false;
    }

    /** Hands every dry bud to the player; returns how many. */
    public int takeDried(Player player) {
        int taken = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (this.states[i] == READY) {
                this.states[i] = EMPTY;
                taken++;
            }
        }
        if (taken > 0) {
            ItemStack dried = new ItemStack(ModItems.DRIED_EDENWEED.get(), taken);
            if (!player.getInventory().add(dried)) {
                player.drop(dried, false);
            }
            this.setChanged();
            this.refreshState();
        }
        return taken;
    }

    /** What falls out when the rack is broken. */
    public List<ItemStack> dropContents() {
        List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            if (this.states[i] == DRYING) {
                drops.add(new ItemStack(ModItems.RAW_EDENWEED.get()));
            } else if (this.states[i] == READY) {
                drops.add(new ItemStack(ModItems.DRIED_EDENWEED.get()));
            }
        }
        return drops;
    }

    /** Keeps the blockstate (and so the model) in line with the slots. */
    public void refreshState() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        DryingRackBlock.Contents contents = DryingRackBlock.Contents.EMPTY;
        for (int state : this.states) {
            if (state == READY) {
                contents = DryingRackBlock.Contents.READY;
                break;
            }
            if (state == DRYING) {
                contents = DryingRackBlock.Contents.DRYING;
            }
        }
        BlockState current = this.getBlockState();
        if (current.getValue(DryingRackBlock.CONTENTS) != contents) {
            this.level.setBlock(this.worldPosition, current.setValue(DryingRackBlock.CONTENTS, contents), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("States", this.states);
        tag.putIntArray("Progress", this.progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] savedStates = tag.getIntArray("States");
        int[] savedProgress = tag.getIntArray("Progress");
        for (int i = 0; i < SLOTS; i++) {
            this.states[i] = i < savedStates.length ? savedStates[i] : EMPTY;
            this.progress[i] = i < savedProgress.length ? savedProgress[i] : 0;
        }
    }
}
