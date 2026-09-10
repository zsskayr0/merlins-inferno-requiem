package dev.zsskayr.merlins_inferno.menu;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity;
import dev.zsskayr.merlins_inferno.registry.ModMenuTypes;

/**
 * The Hell Forge's menu. Slot layout is a custom arrangement (not vanilla furnace's) built around
 * the fuel tank bar: a small feed slot sits right next to the bar it fills, separate from the
 * cook input/output pair - see {@link dev.zsskayr.merlins_inferno.client.HellForgeScreen} for the
 * matching visuals (still drawn over vanilla's furnace panel texture, just repositioned/taller,
 * since no custom GUI art exists yet). No recipe-book integration, unlike vanilla's furnace menus -
 * not requested, and would need its own recipe-book category/tag to work properly anyway.
 */
public class HellForgeMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;

    /**
     * Extra vertical room added at the top of the panel (over vanilla furnace's 166-tall one) so
     * the tank bar has space below the title text instead of running straight into it - see
     * {@link dev.zsskayr.merlins_inferno.client.HellForgeScreen}, which adds this to every
     * background/bar coordinate to match these slot positions.
     */
    public static final int Y_OFFSET = 14;

    private final Container container;
    private final ContainerData data;
    private final Level level;

    /** Client-side: constructed from just the menu-open packet, with a blank container/data that gets synced right after. */
    public HellForgeMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(3), new SimpleContainerData(4));
    }

    /** Server-side: backed by the real block entity. */
    public HellForgeMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.HELL_FORGE.get(), containerId);
        checkContainerSize(container, 3);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;
        this.level = playerInventory.player.level();

        // Call order must stay INPUT, FUEL, RESULT - it's what fixes each Slot's position in
        // this.slots (and therefore the `index` quickMoveStack below receives) to match the
        // INPUT_SLOT/FUEL_SLOT/RESULT_SLOT constants; only the pixel coordinates changed to move
        // the feed slot next to the tank bar (bar occupies x=8-24, y=8-66 in panel space) instead
        // of stacked under the input slot like vanilla - it's a quick "pour it in" slot, not part
        // of the input->output line.
        this.addSlot(new Slot(container, INPUT_SLOT, 56, 17 + Y_OFFSET));
        this.addSlot(new FuelSlot(container, FUEL_SLOT, 30, 28 + Y_OFFSET));
        this.addSlot(new ResultSlot(container, RESULT_SLOT, 116, 35 + Y_OFFSET));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + Y_OFFSET + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142 + Y_OFFSET));
        }

        this.addDataSlots(data);
        container.startOpen(playerInventory.player);
    }

    /** Fraction of the fuel tank that's currently full - drives the fuel bar. */
    public float getFuelLevel() {
        int capacity = this.data.get(1);
        return capacity == 0 ? 0.0F : Mth.clamp((float) this.data.get(0) / capacity, 0.0F, 1.0F);
    }

    public int getStoredFuel() {
        return this.data.get(0);
    }

    public int getFuelCapacity() {
        return this.data.get(1);
    }

    public float getCookProgress() {
        int progress = this.data.get(2);
        int total = this.data.get(3);
        return total != 0 && progress != 0 ? Mth.clamp((float) progress / total, 0.0F, 1.0F) : 0.0F;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();
            if (index == RESULT_SLOT) {
                if (!this.moveItemStackTo(slotStack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, result);
            } else if (index != FUEL_SLOT && index != INPUT_SLOT) {
                if (canSmelt(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, INPUT_SLOT, FUEL_SLOT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isFuel(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, FUEL_SLOT, RESULT_SLOT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= INV_SLOT_START && index < USE_ROW_SLOT_START) {
                    if (!this.moveItemStackTo(slotStack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= USE_ROW_SLOT_START && index < USE_ROW_SLOT_END
                        && !this.moveItemStackTo(slotStack, INV_SLOT_START, USE_ROW_SLOT_START, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, INV_SLOT_START, USE_ROW_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
        }
        return result;
    }

    private boolean canSmelt(ItemStack stack) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        return this.level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, this.level).isPresent()
                || this.level.getRecipeManager().getRecipeFor(RecipeType.BLASTING, input, this.level).isPresent();
    }

    private static boolean isFuel(ItemStack stack) {
        return stack.is(Items.LAVA_BUCKET) || stack.is(Items.BLAZE_POWDER);
    }

    private static class FuelSlot extends Slot {
        FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isFuel(stack);
        }
    }

    private static class ResultSlot extends Slot {
        ResultSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
