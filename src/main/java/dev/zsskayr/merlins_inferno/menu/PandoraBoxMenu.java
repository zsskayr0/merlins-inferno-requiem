package dev.zsskayr.merlins_inferno.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import org.jetbrains.annotations.Nullable;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import dev.zsskayr.merlins_inferno.registry.ModMenuTypes;
import dev.zsskayr.merlins_inferno.menu.PandoraRecipeRules.Ingredient;
import dev.zsskayr.merlins_inferno.menu.PandoraRecipeRules.Recipe;

/** Four inputs, one forge action. The server owns the timer, validation, consumption and result. */
public class PandoraBoxMenu extends AbstractContainerMenu {
    public static final int BUTTON_FORGE = 0;
    public static final int CRAFT_TICKS = 60;
    private static final int INPUTS = 4;
    public static final int RESULT_SLOT = 4, INVENTORY_START = 5, INVENTORY_END = 41;
    private final Container container = new SimpleContainer(INPUTS);
    private final Container resultContainer = new SimpleContainer(1);
    private final Player player;
    private final ContainerData data;
    private final ItemStack[] workingInputs = new ItemStack[INPUTS];
    private int progress, completionSerial;
    private Recipe workingRecipe = Recipe.NONE, lastRecipe = Recipe.NONE;
    private long lastCraftTick = Long.MIN_VALUE;

    /** Where the placed Box this menu was opened from sits; null when opened from the item in the inventory. */
    private final @Nullable BlockPos boxPos;

    public PandoraBoxMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public PandoraBoxMenu(int containerId, Inventory inventory, @Nullable BlockPos boxPos) {
        super(ModMenuTypes.PANDORA_BOX.get(), containerId);
        this.boxPos = boxPos;
        player = inventory.player;
        // Client reads the server's synchronized values, not its local progression attachment.
        data = player.level().isClientSide ? new SimpleContainerData(5) : new SimpleContainerData(5) {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> ProgressionHelper.circle(player);
                    case 1 -> progress;
                    case 2 -> workingRecipe.ordinal();
                    case 3 -> completionSerial;
                    case 4 -> lastRecipe.ordinal();
                    default -> 0;
                };
            }
        };
        for (int i = 0; i < INPUTS; i++) addSlot(new InputSlot(i));
        addSlot(new ResultSlot());
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9,
                    PandoraLayout.INVENTORY_X + col * 18, PandoraLayout.INVENTORY_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col,
                PandoraLayout.INVENTORY_X + col * 18, PandoraLayout.HOTBAR_Y));
        addDataSlots(data);
    }

    public @Nullable BlockPos getBoxPos() { return boxPos; }
    public int getCircle() { return data.get(0); }
    public boolean isCrafting() { return data.get(1) > 0; }
    public float getCraftProgress() { return Math.min(1, data.get(1) / (float) CRAFT_TICKS); }
    public int getCompletionSerial() { return data.get(3); }
    public Recipe getLastRecipe() { return recipeValue(data.get(4)); }
    private static Recipe recipeValue(int ordinal) {
        return ordinal >= 0 && ordinal < Recipe.values().length ? Recipe.values()[ordinal] : Recipe.NONE;
    }
    public Recipe getDisplayedRecipe() { return isCrafting() ? recipeValue(data.get(2)) : getMatchedRecipe(); }
    public boolean hasResult() { return !resultContainer.isEmpty(); }
    public boolean canForge() { return !hasResult() && !isCrafting() && getMatchedRecipe() != Recipe.NONE; }
    public int getOfferingCount() {
        int count = 0;
        for (int i = 0; i < 3; i++) if (getSlot(i).hasItem()) count++;
        return count;
    }

    public Recipe getMatchedRecipe() {
        Ingredient[] offerings = new Ingredient[3];
        int[] counts = new int[3];
        for (int i = 0; i < 3; i++) {
            ItemStack stack = container.getItem(i);
            offerings[i] = ingredient(stack);
            counts[i] = stack.getCount();
        }
        return PandoraRecipeRules.match(getCircle() >= ProgressionHelper.SECOND_CIRCLE,
                ingredient(container.getItem(3)), offerings, counts);
    }

    private static Ingredient ingredient(ItemStack stack) {
        if (stack.isEmpty()) return Ingredient.EMPTY;
        if (stack.is(ModItems.BOOK_OF_CONTRACTS.get())) return Ingredient.BOOK;
        if (stack.is(ModItems.EVES_SECRET.get())) return Ingredient.EVE;
        if (stack.is(ModItems.FLAME_OF_GOD.get())) return Ingredient.FLAME;
        if (stack.is(Items.NETHER_STAR)) return Ingredient.STAR;
        if (stack.is(ModItems.OTHERWORLD_ESSENCE.get())) return Ingredient.OTHERWORLD;
        if (stack.is(ModItems.INFERNAL_ESSENCE.get())) return Ingredient.INFERNAL;
        if (stack.is(ModItems.CELESTIAL_ESSENCE.get())) return Ingredient.CELESTIAL;
        if (stack.is(ModBlocks.VOID_BLOCK.get().asItem())) return Ingredient.VOID;
        if (stack.is(Items.NETHERITE_INGOT)) return Ingredient.NETHERITE;
        return Ingredient.OTHER;
    }

    public static ItemStack result(Recipe recipe) {
        return switch (recipe) {
            case OBLIVION -> new ItemStack(ModItems.OBLIVION_KEY.get());
            case PURGATORY -> new ItemStack(ModItems.PURGATORY_KEY.get());
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer) || id != BUTTON_FORGE || !stillValid(player) || !canForge()) return false;
        workingRecipe = getMatchedRecipe();
        for (int i = 0; i < INPUTS; i++) workingInputs[i] = container.getItem(i).copy();
        progress = 1;
        lastCraftTick = player.level().getGameTime();
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5F, 0.6F);
        broadcastChanges();
        return true;
    }

    @Override
    public void broadcastChanges() {
        // ServerPlayer broadcasts once per tick. Guard against additional calls in the same game tick.
        if (player instanceof ServerPlayer server && progress > 0 && lastCraftTick != player.level().getGameTime()) {
            lastCraftTick = player.level().getGameTime();
            if (hasResult() || !stillValid(player) || getMatchedRecipe() != workingRecipe || !inputsUnchanged()) cancelCraft();
            else if (++progress >= CRAFT_TICKS) finishCraft(server);
        }
        super.broadcastChanges();
    }

    private boolean inputsUnchanged() {
        for (int i = 0; i < INPUTS; i++) if (!ItemStack.matches(workingInputs[i], container.getItem(i))) return false;
        return true;
    }

    private void cancelCraft() { progress = 0; workingRecipe = Recipe.NONE; }

    private void finishCraft(ServerPlayer server) {
        Recipe completed = workingRecipe;
        // Exact recipe quantities, retaining surplus stacks.
        for (int i = 0; i < 3; i++) container.removeItem(i, PandoraRecipeRules.required(completed, ingredient(container.getItem(i))));
        container.removeItem(3, 1);
        lastRecipe = completed;
        completionSerial = (completionSerial + 1) & 0x7FFF;
        cancelCraft();
        if (completed == Recipe.AWAKENING) {
            ProgressionHelper.setCircle(server, ProgressionHelper.SECOND_CIRCLE);
            server.level().playSound(null, server.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1, 0.6F);
            boolean spared = server.level().getLevelData().isHardcore() || server.isCreative();
            server.displayClientMessage(Component.translatable(spared
                    ? "message.merlins_inferno.pandora_box.opened_spared" : "message.merlins_inferno.pandora_box.opened"), false);
            if (!spared) {
                server.closeContainer();
                server.kill();
            }
        } else {
            resultContainer.setItem(0, result(completed));
            server.level().playSound(null, server.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1, 0.8F);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < INVENTORY_START) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) return ItemStack.EMPTY;
        } else {
            if (isCrafting()) return ItemStack.EMPTY;
            Ingredient kind = ingredient(stack);
            // The Book is an offering for the Awakening, but the catalyst once the Box is open.
            boolean catalyst = kind == Ingredient.STAR || kind == Ingredient.VOID
                    || (kind == Ingredient.BOOK && getCircle() >= ProgressionHelper.SECOND_CIRCLE);
            if (catalyst && hasResult()) return ItemStack.EMPTY;
            if (!moveItemStackTo(stack, catalyst ? 3 : 0, catalyst ? 4 : 3, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (boxPos != null) {
            return player.level().getBlockState(boxPos).is(ModBlocks.PANDORA_BOX.get())
                    && player.distanceToSqr(boxPos.getX() + 0.5, boxPos.getY() + 0.5, boxPos.getZ() + 0.5) <= 64.0;
        }
        return player.getInventory().contains(stack -> stack.is(ModItems.PANDORA_BOX.get()));
    }

    @Override
    public void removed(Player player) {
        cancelCraft();
        super.removed(player);
        clearContainer(player, container);
        clearContainer(player, resultContainer);
    }

    private class InputSlot extends Slot {
        InputSlot(int index) {
            super(PandoraBoxMenu.this.container, index, PandoraLayout.slotX(index, PandoraLayout.INITIAL_ANGLE),
                    PandoraLayout.slotY(index, PandoraLayout.INITIAL_ANGLE));
        }
        @Override public boolean isActive() { return getContainerSlot() != 3 || !hasResult(); }
        @Override public boolean mayPlace(ItemStack stack) { return isActive() && !isCrafting(); }
        @Override public boolean mayPickup(Player player) { return isActive() && !isCrafting(); }
    }

    /** Shares the catalyst's screen position, but has its own storage and never accepts input. */
    private class ResultSlot extends Slot {
        ResultSlot() {
            super(PandoraBoxMenu.this.resultContainer, 0, PandoraLayout.CENTER_X - 8, PandoraLayout.CENTER_Y - 8);
        }
        @Override public boolean isActive() { return hasResult(); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return !isCrafting() && hasResult(); }
    }
}
