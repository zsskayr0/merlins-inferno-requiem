package dev.zsskayr.merlins_inferno.blockentity;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import dev.zsskayr.merlins_inferno.recipe.HellForgeRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.menu.HellForgeMenu;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModRecipeTypes;

/**
 * The Hell Forge's block entity - a furnace/blast-furnace hybrid: it accepts either recipe type
 * for its input (checking {@link ModRecipeTypes#HELL_FORGE_SMELTING} first, then
 * {@link RecipeType#SMELTING}, then {@link RecipeType#BLASTING}) and cooks 30% faster than any of
 * them ({@link #COOK_TIME_MULTIPLIER}). Fuel is restricted to lava buckets and blaze powder
 * ({@link #getBurnDuration}).
 * <p>
 * {@code HELL_FORGE_SMELTING} exists specifically for recipes that must NOT also work in a plain
 * furnace/blast furnace (e.g. Demonblood Bar) - vanilla furnaces only ever look up
 * {@code RecipeType.SMELTING}/{@code BLASTING}/{@code SMOKING}, so a recipe registered under this
 * mod's own type is invisible to them. Recipes that SHOULD also work in a normal furnace (e.g.
 * Rowanwood Bar) keep using vanilla's {@code minecraft:smelting} type as before - this block
 * still honors those too, it just checks its own type first.
 * <p>
 * Unlike a vanilla furnace, fuel doesn't sit in its slot being burned down one item at a time -
 * whatever's placed there is immediately converted into {@link #storedFuel} (a tank, capped at
 * {@link #FUEL_CAPACITY}) the moment there's room for its full value, same as dropping infuse
 * material into a Mekanism infuser. A lava bucket still leaves an empty bucket behind. The tank
 * only drains while actually cooking something - it doesn't waste away sitting idle.
 * <p>
 * Structurally this mirrors vanilla's {@code AbstractFurnaceBlockEntity} (same 3-slot layout)
 * rather than extending it, since that class is hardwired to a single {@code RecipeType} for both
 * fuel lookups and recipe matching - this needs two recipe types, plus the tank behavior above.
 * Recipe-book integration and XP-on-collect are intentionally left out for now (not requested).
 */
public class HellForgeBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    private static final int SLOT_COUNT = 3;

    // Below only ever pulls the result - without WorldlyContainer, a plain Container lets a hopper
    // underneath pull from whichever slot it reaches first in index order (0, the input slot),
    // which was the original bug this fixes. Every other face is a feed face: FUEL_SLOT is tried
    // first (so a lava bucket/blaze powder lands in the tank feed, not the input), and INPUT_SLOT
    // second if the item isn't fuel - vanilla's own hopper insertion already walks this array in
    // order and only advances past a slot once canPlaceItemThroughFace rejects it there, so this
    // alone gives the "try fuel, then input, else stays in the hopper" fallback chain.
    private static final int[] SLOTS_FOR_DOWN = new int[]{RESULT_SLOT};
    private static final int[] SLOTS_FOR_FEED = new int[]{FUEL_SLOT, INPUT_SLOT};

    /** How much faster than a normal furnace/blast furnace this cooks - 30% faster. */
    private static final float COOK_TIME_MULTIPLIER = 0.7F;
    private static final int DEFAULT_COOK_TIME = 200;
    private static final int BURN_COOL_SPEED = 2;

    /** Tank size, in ticks of burn time - exactly one lava bucket's worth. */
    public static final int FUEL_CAPACITY = 20000;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    int storedFuel;
    int cookingProgress;
    int cookingTotalTime;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> HellForgeBlockEntity.this.storedFuel;
                case 1 -> FUEL_CAPACITY;
                case 2 -> HellForgeBlockEntity.this.cookingProgress;
                case 3 -> HellForgeBlockEntity.this.cookingTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> HellForgeBlockEntity.this.storedFuel = value;
                case 2 -> HellForgeBlockEntity.this.cookingProgress = value;
                case 3 -> HellForgeBlockEntity.this.cookingTotalTime = value;
                default -> {
                    // index 1 (capacity) is a constant, nothing to set
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public HellForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.HELL_FORGE.get(), pos, state);
    }

    /** Only lava buckets and blaze powder count as fuel here - not vanilla's full fuel tag. */
    private static int getBurnDuration(ItemStack fuel) {
        if (fuel.is(Items.LAVA_BUCKET)) {
            return 20000; // same as a vanilla furnace - exactly fills an empty tank
        } else if (fuel.is(Items.BLAZE_POWDER)) {
            return 1200; // half a blaze rod's worth - blaze powder isn't a vanilla furnace fuel at all
        } else {
            return 0;
        }
    }

    // One cached lookup per recipe type, like vanilla's furnaces keep: each remembers the last recipe
    // that matched and tries it first, so an unchanged input costs a single match() instead of a
    // scan of every recipe of the type on every call.
    private final RecipeManager.CachedCheck<SingleRecipeInput, HellForgeRecipe> forgeCheck =
            RecipeManager.createCheck(ModRecipeTypes.HELL_FORGE_SMELTING.get());
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> smeltingCheck = RecipeManager.createCheck(RecipeType.SMELTING);
    private final RecipeManager.CachedCheck<SingleRecipeInput, BlastingRecipe> blastingCheck = RecipeManager.createCheck(RecipeType.BLASTING);

    @SuppressWarnings("unchecked")
    private Optional<RecipeHolder<AbstractCookingRecipe>> getRecipe(Level level, ItemStack input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> found = this.forgeCheck.getRecipeFor(recipeInput, level);
        if (found.isEmpty()) {
            found = this.smeltingCheck.getRecipeFor(recipeInput, level);
        }
        if (found.isEmpty()) {
            found = this.blastingCheck.getRecipeFor(recipeInput, level);
        }
        return found.map(holder -> (RecipeHolder<AbstractCookingRecipe>) holder);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HellForgeBlockEntity blockEntity) {
        boolean dirty = false;

        // Absorb whatever's in the fuel slot into the tank the instant there's room for its full
        // value - it disappears from the slot right away rather than sitting there being burned
        // down over time.
        ItemStack fuel = blockEntity.items.get(FUEL_SLOT);
        int fuelValue = getBurnDuration(fuel);
        if (fuelValue > 0 && blockEntity.storedFuel + fuelValue <= FUEL_CAPACITY) {
            blockEntity.storedFuel += fuelValue;
            ItemStack remainder = fuel.hasCraftingRemainingItem() ? fuel.getCraftingRemainingItem() : ItemStack.EMPTY;
            fuel.shrink(1);
            if (!remainder.isEmpty()) {
                ejectRemainder(level, pos, blockEntity, remainder);
            }
            dirty = true;
        }

        // No fuel and nothing half-cooked: no recipe can progress, so skip the lookup entirely.
        if (blockEntity.storedFuel <= 0 && blockEntity.cookingProgress <= 0) {
            if (dirty) {
                setChanged(level, pos, state);
            }
            return;
        }

        ItemStack input = blockEntity.items.get(INPUT_SLOT);
        RecipeHolder<AbstractCookingRecipe> recipe = input.isEmpty() ? null : blockEntity.getRecipe(level, input).orElse(null);
        boolean canBurn = canBurn(recipe, blockEntity.items, blockEntity.getMaxStackSize(), level);

        if (canBurn && blockEntity.storedFuel > 0) {
            blockEntity.storedFuel--;
            blockEntity.cookingProgress++;
            if (blockEntity.cookingProgress >= blockEntity.cookingTotalTime) {
                blockEntity.cookingProgress = 0;
                blockEntity.cookingTotalTime = getTotalCookTime(recipe);
                burn(recipe, blockEntity.items, blockEntity.getMaxStackSize(), level);
            }
            dirty = true;
        } else if (blockEntity.cookingProgress > 0) {
            blockEntity.cookingProgress = Mth.clamp(blockEntity.cookingProgress - BURN_COOL_SPEED, 0, blockEntity.cookingTotalTime);
            dirty = true;
        }

        if (dirty) {
            setChanged(level, pos, state);
        }
    }

    /**
     * Where a spent container (the empty bucket a lava bucket leaves behind) goes. It used to sit
     * in the fuel slot, where no hopper could reach it (only the result slot is exposed on the
     * bottom face) and the next bucket could never be fed in - automation stalled after one
     * bucket. Now it's moved to the result slot if that has room, or dropped next to the block.
     */
    private static void ejectRemainder(Level level, BlockPos pos, HellForgeBlockEntity blockEntity, ItemStack remainder) {
        ItemStack output = blockEntity.items.get(RESULT_SLOT);
        if (output.isEmpty()) {
            blockEntity.items.set(RESULT_SLOT, remainder);
        } else if (ItemStack.isSameItemSameComponents(output, remainder)
                && output.getCount() + remainder.getCount() <= output.getMaxStackSize()) {
            output.grow(remainder.getCount());
        } else {
            Block.popResource(level, pos.above(), remainder);
        }
    }

    private static boolean canBurn(@Nullable RecipeHolder<AbstractCookingRecipe> recipe, NonNullList<ItemStack> items, int maxStackSize, Level level) {
        if (items.get(INPUT_SLOT).isEmpty() || recipe == null) {
            return false;
        }
        // getResultItem hands back the recipe's own stack (no per-tick copy like assemble); cooking
        // recipes have a fixed result, and this stack is only read here.
        ItemStack result = recipe.value().getResultItem(level.registryAccess());
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = items.get(RESULT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= maxStackSize
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private static void burn(@Nullable RecipeHolder<AbstractCookingRecipe> recipe, NonNullList<ItemStack> items, int maxStackSize, Level level) {
        if (recipe == null || !canBurn(recipe, items, maxStackSize, level)) {
            return;
        }
        ItemStack input = items.get(INPUT_SLOT);
        ItemStack result = recipe.value().assemble(new SingleRecipeInput(input), level.registryAccess());
        ItemStack output = items.get(RESULT_SLOT);
        if (output.isEmpty()) {
            items.set(RESULT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }
        input.shrink(1);
    }

    private static int getTotalCookTime(@Nullable RecipeHolder<AbstractCookingRecipe> recipe) {
        int baseTime = recipe == null ? DEFAULT_COOK_TIME : recipe.value().getCookingTime();
        return Math.max(1, Math.round(baseTime * COOK_TIME_MULTIPLIER));
    }

    // --- Container / BaseContainerBlockEntity ---
    // isEmpty/getItem/removeItem/removeItemNoUpdate/stillValid/clearContent all come from
    // BaseContainerBlockEntity via getItems() below - only setItem needs a small addition
    // (resetting cook progress when the input slot's item actually changes).

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack existing = this.items.get(slot);
        boolean sameItemStacked = !stack.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack);
        super.setItem(slot, stack);
        if (slot == INPUT_SLOT && !sameItemStacked && this.level != null) {
            this.cookingTotalTime = getTotalCookTime(this.getRecipe(this.level, stack).orElse(null));
            this.cookingProgress = 0;
            this.setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case FUEL_SLOT -> getBurnDuration(stack) > 0;
            case INPUT_SLOT -> this.level != null && this.getRecipe(this.level, stack).isPresent();
            default -> false; // RESULT_SLOT
        };
    }

    // --- WorldlyContainer (hopper/automation access, restricted by the side it's coming from) ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_FOR_DOWN : SLOTS_FOR_FEED;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return this.canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        if (index == RESULT_SLOT) {
            return true;
        }
        // The tank absorbs fuel instantly, leaving only a spent bucket behind (see serverTick) -
        // that empty bucket can be pulled back out, mirroring vanilla's furnace fuel-slot
        // exception, but unconverted fuel/input itself can't be siphoned back out mid-feed.
        return index == FUEL_SLOT && stack.is(Items.BUCKET);
    }

    // --- MenuProvider / BaseContainerBlockEntity ---

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.merlins_inferno.hell_forge");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new HellForgeMenu(containerId, playerInventory, this, this.dataAccess);
    }

    // --- NBT ---

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.storedFuel = tag.getInt("StoredFuel");
        this.cookingProgress = tag.getInt("CookTime");
        this.cookingTotalTime = tag.getInt("CookTimeTotal");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("StoredFuel", this.storedFuel);
        tag.putInt("CookTime", this.cookingProgress);
        tag.putInt("CookTimeTotal", this.cookingTotalTime);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }
}
