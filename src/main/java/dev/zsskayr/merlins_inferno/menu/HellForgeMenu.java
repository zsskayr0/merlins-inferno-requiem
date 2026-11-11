package dev.zsskayr.merlins_inferno.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity;
import dev.zsskayr.merlins_inferno.forge.ForgeThermalData;
import dev.zsskayr.merlins_inferno.registry.ModMenuTypes;

public class HellForgeMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = HellForgeBlockEntity.DATA_COUNT;
    /** Smelting lanes shown at once; the forge has nine, so the list no longer scrolls. */
    public static final int LANE_ROWS = 9;
    private final Player owner;
    private int firstLane;
    public int firstLane() { return firstLane; }
    private final Container container;
    private final ContainerData data;
    private final net.minecraft.world.level.Level level;
    private static final int FORGE_SLOTS = HellForgeBlockEntity.SLOT_COUNT;

    public HellForgeMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(FORGE_SLOTS), new SimpleContainerData(DATA_COUNT));
    }
    public HellForgeMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModMenuTypes.HELL_FORGE.get(), id);
        checkContainerSize(container, FORGE_SLOTS);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        this.level = inventory.player.level();
        this.owner = inventory.player;
        // Menu indices follow the container indices, including the legacy first three slots.
        addLaneSlot(0, false);
        addSlot(new Slot(container, HellForgeBlockEntity.FUEL_SLOT, 320, 246) {
            @Override public boolean mayPlace(ItemStack stack) { return ForgeThermalData.fuel(level, stack) != null; }
        });
        addLaneSlot(0, true);
        for (int lane = 1; lane < 9; lane++) { addLaneSlot(lane, false); addLaneSlot(lane, true); }
        addOutput(HellForgeBlockEntity.DROSS_SLOT, 229, 264);
        addOutput(HellForgeBlockEntity.SCRAP_SLOT, 259, 264);
        // Empty buckets are not shown: they go straight to the player's inventory (see returnBuckets).
        addSlot(new Slot(container, HellForgeBlockEntity.BUCKET_SLOT, 0, 0) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean isActive() { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 39 + col * 16, 220 + row * 16));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 39 + col * 16, 274));
        addDataSlots(data);
        container.startOpen(inventory.player);
    }
    private void addOutput(int index, int x, int y) {
        addSlot(new Slot(container, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
    }
    private void addLaneSlot(int lane, boolean output) {
        int index = output ? HellForgeBlockEntity.outputSlot(lane) : HellForgeBlockEntity.inputSlot(lane);
        int x = output ? 289 : 207;
        int y = 39 + (lane - firstLane) * 22;
        Slot laneSlot = new Slot(container, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return !output && lane < getProcessingSlots() && ForgeThermalData.recipe(level, stack).isPresent();
            }
            @Override public boolean isActive() { return lane >= firstLane && lane < firstLane + LANE_ROWS && (lane < getProcessingSlots() || hasItem()); }
        };
        if (slots.size() > index) { laneSlot.index = index; slots.set(index, laneSlot); }
        else addSlot(laneSlot);
    }
    public void scrollLanes(int first) {
        int count = getProcessingSlots();
        for (int lane = 0; lane < 9; lane++) if (slots.get(HellForgeBlockEntity.inputSlot(lane)).hasItem()
                || slots.get(HellForgeBlockEntity.outputSlot(lane)).hasItem()) count = Math.max(count, lane + 1);
        firstLane = Math.clamp(first, 0, Math.max(0, count - LANE_ROWS));
        for (int lane = 0; lane < 9; lane++) { addLaneSlot(lane, false); addLaneSlot(lane, true); }
    }
    /** Moves any empty bucket left by lava fuel into the player's inventory, dropping it if there is no room. */
    private void returnBuckets() {
        if (owner.level().isClientSide) return;
        ItemStack bucket = container.getItem(HellForgeBlockEntity.BUCKET_SLOT);
        if (bucket.isEmpty()) return;
        container.setItem(HellForgeBlockEntity.BUCKET_SLOT, ItemStack.EMPTY);
        if (!owner.getInventory().add(bucket)) owner.drop(bucket, false);
    }
    @Override public void broadcastChanges() { returnBuckets(); super.broadcastChanges(); }
    public int value(int index) { return data.get(index); }
    public int getStoredFuel() { return value(0); }
    public int getFuelCapacity() { return value(1); }
    public int getProcessingSlots() { return value(4); }
    public float progress(int lane) { return Math.clamp(value(20 + lane * 5) / 1000F, 0, 1); }
    public int status(int lane) { return value(21 + lane * 5); }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (!(container instanceof HellForgeBlockEntity forge) || !stillValid(player)) return false;
        if (button == 0) forge.dumpFuel();
        else if (button == 1) forge.toggleAutoSort();
        else if (button >= 10 && button < 304) forge.configureVault(button - 10, -1);
        else if (button >= 400 && button < 694) forge.configureVault(button - 400, -2);
        else if (button >= 1000 && button < 3646) forge.configureVault((button - 1000) / 9, (button - 1000) % 9);
        else if (button >= 4000 && button < 4882) {
            int port = (button - 4000) / 3;
            int desired = (button - 4000) % 3;
            for (int attempt = 0; attempt < 3 && data.get(65 + port * 3) >= 0 && data.get(65 + port * 3) != desired; attempt++)
                forge.configureVault(port, -1);
        }
        else return false;
        broadcastChanges();
        return true;
    }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = false;
        if (index < FORGE_SLOTS) moved = moveItemStackTo(stack, FORGE_SLOTS, slots.size(), true);
        else if (ForgeThermalData.fuel(level, stack) != null) moved = moveItemStackTo(stack, 1, 2, false);
        else if (ForgeThermalData.recipe(level, stack).isPresent()) {
            for (int lane = 0; lane < getProcessingSlots() && !stack.isEmpty(); lane++) {
                int input = HellForgeBlockEntity.inputSlot(lane);
                moved |= moveItemStackTo(stack, input, input + 1, false);
            }
        } else {
            int hotbar = FORGE_SLOTS + 27;
            moved = index < hotbar ? moveItemStackTo(stack, hotbar, slots.size(), false)
                    : moveItemStackTo(stack, FORGE_SLOTS, hotbar, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
