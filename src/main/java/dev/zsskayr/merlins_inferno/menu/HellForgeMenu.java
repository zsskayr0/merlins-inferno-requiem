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
import dev.zsskayr.merlins_inferno.registry.ModRecipeTypes;

/**
 * The Hell Forge's menu. Slot layout is a custom arrangement (not vanilla furnace's) built around
 * the fuel tank bar: the feed slot sits ON the tank bar itself, at the orb baked into the middle
 * of the gauge (between the two halves it fills) - not just visually adjacent, that's genuinely
 * where the item goes, separate from the cook input/output pair - see
 * {@link dev.zsskayr.merlins_inferno.client.HellForgeScreen} for the matching visuals (drawn over
 * the mod's own custom panel art, {@code hell_forge.png}). The player-inventory grid below sits at
 * vanilla's own standard coordinates - the custom panel's art was drawn to match those exactly, so
 * no offset/adjustment is needed for it. No recipe-book integration, unlike vanilla's furnace
 * menus - not requested, and would need its own recipe-book category/tag to work properly anyway.
 */
public class HellForgeMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;

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
        // INPUT_SLOT/FUEL_SLOT/RESULT_SLOT constants. INPUT/RESULT match the two sockets flanking
        // the burn gauge in hell_forge.png; FUEL sits on the orb baked into the middle of the tank
        // bar itself, between its two halves - not decoration, that's genuinely where the feed
        // slot lives, so the lava bucket drops right into the gauge it's filling.
        this.addSlot(new Slot(container, INPUT_SLOT, 48, 50));
        this.addSlot(new FuelSlot(container, FUEL_SLOT, 79, 20));
        this.addSlot(new ResultSlot(container, RESULT_SLOT, 110, 50));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
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
        // Same lookup order as HellForgeBlockEntity#getRecipe - the forge-exclusive type first,
        // otherwise Demonblood Scrap (which no furnace recipe covers) would never quick-move in.
        return this.level.getRecipeManager().getRecipeFor(ModRecipeTypes.HELL_FORGE_SMELTING.get(), input, this.level).isPresent()
                || this.level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, this.level).isPresent()
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
