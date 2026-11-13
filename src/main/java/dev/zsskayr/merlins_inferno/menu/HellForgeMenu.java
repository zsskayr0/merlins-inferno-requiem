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
            @Override public boolean mayPlace(ItemStack stack) { return ForgeThermalData.fuel(level, stack) != null || ForgeThermalData.chill(level, stack) > 0; }
        });
        addLaneSlot(0, true);
        for (int lane = 1; lane < 9; lane++) { addLaneSlot(lane, false); addLaneSlot(lane, true); }
        // The Demonic Core fills the dross slot; the Blighted Core eats from it, so there the player may put dross in.
        addSlot(new Slot(container, HellForgeBlockEntity.DROSS_SLOT, 229, 264) {
            @Override public boolean mayPlace(ItemStack stack) { return eatsDross() && stack.is(dev.zsskayr.merlins_inferno.registry.ModItems.DUSK_DROSS.get()); }
            @Override public boolean isActive() { return !magmatic(); }
        });
        addSlot(new Slot(container, HellForgeBlockEntity.SCRAP_SLOT, 259, 264) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean isActive() { return !magmatic(); }
        });
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
                if (magmatic() && lane == 0)
                    return output ? stack.is(net.minecraft.world.item.Items.BUCKET) : ForgeThermalData.melting(level, stack) != null;
                return !output && lane < getProcessingSlots()
                        && (ForgeThermalData.recipe(level, stack).isPresent() || ForgeThermalData.chill(level, stack) > 0);
            }
            @Override public int getMaxStackSize() { return magmatic() && output && lane == 0 ? 1 : super.getMaxStackSize(); }
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
    /** The screen's fuel cap and the cap in force, 0-1. */
    public float fillLimit() { return value(HellForgeBlockEntity.DATA_EXTRA) / 1000F; }
    public float effectiveFillLimit() { return value(HellForgeBlockEntity.DATA_EXTRA + 1) / 1000F; }
    public int redstoneLevel() { return value(HellForgeBlockEntity.DATA_EXTRA + 2); }
    public float progress(int lane) { return Math.clamp(value(20 + lane * 5) / 1000F, 0, 1); }
    public int status(int lane) { return value(21 + lane * 5); }

    // Button ids: one block per port action, sized for every port of the largest forge.
    private static final int PORTS = HellForgeBlockEntity.MAX_PORTS;
    public static final int CYCLE = 10, AUTO = 1000, MASK = 2000, SET_MODE = 6000, FILL_LIMIT = 8000;
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!(container instanceof HellForgeBlockEntity forge) || !stillValid(player)) return false;
        if (button == 0) forge.dumpFuel();
        else if (button == 1) forge.toggleAutoSort();
        else if (button >= CYCLE && button < CYCLE + PORTS) forge.configureVault(button - CYCLE, -1);
        else if (button >= AUTO && button < AUTO + PORTS) forge.configureVault(button - AUTO, -2);
        else if (button >= MASK && button < MASK + PORTS * 9) forge.configureVault((button - MASK) / 9, (button - MASK) % 9);
        else if (button >= FILL_LIMIT && button <= FILL_LIMIT + 1000) forge.setFillLimit((button - FILL_LIMIT) / 1000.0);
        else if (button >= SET_MODE && button < SET_MODE + PORTS * 3) {
            int port = (button - SET_MODE) / 3;
            int desired = (button - SET_MODE) % 3;
            for (int attempt = 0; attempt < 3 && data.get(65 + port * 3) >= 0 && data.get(65 + port * 3) != desired; attempt++)
                forge.configureVault(port, -1);
        }
        else return false;
        broadcastChanges();
        return true;
    }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }
    /** A Blighted forge (core tier 2 and up) burns the dross in its slot instead of producing it. */
    public boolean eatsDross() { return (data.get(19) & 15) >= 2 && !magmatic(); }
    /** A Magmatic forge: one melting input, one bucket, no lanes or residues. */
    public boolean magmatic() { return (data.get(19) >> 4 & 1) == 1; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = false;
        if (index < FORGE_SLOTS) moved = moveItemStackTo(stack, FORGE_SLOTS, slots.size(), true);
        else if (ForgeThermalData.fuel(level, stack) != null) moved = moveItemStackTo(stack, 1, 2, false);
        else if (!magmatic() && ForgeThermalData.chill(level, stack) > 0) {
            // ice goes in with the raw items: the first free lane takes it
            for (int lane = 0; lane < getProcessingSlots() && !moved; lane++) {
                int input = HellForgeBlockEntity.inputSlot(lane);
                if (!slots.get(input).hasItem() || ItemStack.isSameItemSameComponents(slots.get(input).getItem(), stack))
                    moved = moveItemStackTo(stack, input, input + 1, false);
            }
        }
        else if (magmatic() && ForgeThermalData.melting(level, stack) != null)
            moved = moveItemStackTo(stack, HellForgeBlockEntity.inputSlot(0), HellForgeBlockEntity.inputSlot(0) + 1, false);
        else if (magmatic() && stack.is(net.minecraft.world.item.Items.BUCKET))
            moved = moveItemStackTo(stack, HellForgeBlockEntity.outputSlot(0), HellForgeBlockEntity.outputSlot(0) + 1, false);
        else if (eatsDross() && stack.is(dev.zsskayr.merlins_inferno.registry.ModItems.DUSK_DROSS.get()))
            moved = moveItemStackTo(stack, HellForgeBlockEntity.DROSS_SLOT, HellForgeBlockEntity.DROSS_SLOT + 1, false);
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
