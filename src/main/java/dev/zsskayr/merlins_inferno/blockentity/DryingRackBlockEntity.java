package dev.zsskayr.merlins_inferno.blockentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/** Three stacked slots holding one item each. Raw Edenweed in a slot dries into Dried Edenweed over time. */
public class DryingRackBlockEntity extends BlockEntity {
    public static final int SLOTS = 3;
    /** Two minutes for a bud to dry. */
    public static final int DRY_TICKS = 2400;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final int[] progress = new int[SLOTS];

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.DRYING_RACK.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity rack) {
        boolean changed = false;
        for (int i = 0; i < SLOTS; i++) {
            if (rack.items.get(i).is(ModItems.RAW_EDENWEED.get()) && ++rack.progress[i] >= DRY_TICKS) {
                rack.items.set(i, new ItemStack(ModItems.DRIED_EDENWEED.get()));
                rack.progress[i] = 0;
                changed = true;
            }
        }
        if (changed) {
            rack.sync();
        }
    }

    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    public void place(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        this.progress[slot] = 0;
        this.sync();
    }

    public ItemStack take(int slot) {
        ItemStack stack = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        this.progress[slot] = 0;
        this.sync();
        return stack;
    }

    /** What falls out when the rack is broken. */
    public List<ItemStack> dropContents() {
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
        return drops;
    }

    private void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putIntArray("Progress", this.progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        int[] saved = tag.getIntArray("Progress");
        for (int i = 0; i < SLOTS; i++) {
            this.progress[i] = i < saved.length ? saved[i] : 0;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
