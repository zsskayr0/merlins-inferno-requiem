package dev.zsskayr.merlins_inferno.blockentity;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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

import dev.zsskayr.merlins_inferno.block.HellForgeControllerBlock;
import dev.zsskayr.merlins_inferno.menu.HellForgeMenu;
import dev.zsskayr.merlins_inferno.multiblock.HellForgeStructure;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModRecipeTypes;

/**
 * The Hell Forge's block entity - a furnace/blast-furnace hybrid: it accepts either recipe type
 * for its input (checking {@link ModRecipeTypes#HELL_FORGE_SMELTING} first, then whichever of
 * {@link RecipeType#SMELTING} and {@link RecipeType#BLASTING} is quicker) and cooks 30% faster than the
 * recipe it picked ({@link #COOK_TIME_MULTIPLIER}). Fuel is restricted to lava buckets, Haunted logs and blaze powder
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

    // Kept as a WorldlyContainer (rather than a plain Container) so vanilla hoppers see no slots at
    // all on the Controller; a plain Container would let them reach every slot from any side.
    private static final int[] NO_SLOTS = new int[0];

    /** How much faster than a normal furnace/blast furnace this cooks - 30% faster. */
    private static final float COOK_TIME_MULTIPLIER = 0.7F;
    private static final int DEFAULT_COOK_TIME = 200;
    private static final int BURN_COOL_SPEED = 2;

    /** Tank size, in ticks of burn time - exactly one lava bucket's worth. */
    public static final int FUEL_CAPACITY = 20000;
    /** Burn time per millibucket of lava poured in as a fluid (a bucket is 1000 mB = 20000 ticks). */
    public static final int FUEL_PER_MB = 20;
    /** Haunted logs: not fuel for a vanilla furnace, but a strong one here. */
    public static final int HAUNTED_LOG_BURN = 18000;

    /** How often (ticks) the structure is re-validated while running - cheap, but not every tick. */
    private static final int STRUCTURE_CHECK_INTERVAL = 20;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    int storedFuel;
    int cookingProgress;
    int cookingTotalTime;

    // Structure state is derived, never saved: it is re-checked on the first tick after load.
    private boolean formed;
    private int coreTier;
    private Component structureError = Component.translatable("message.merlins_inferno.hell_forge.incomplete");
    private int structureCheckCountdown;
    /** Set when loaded from disk: the first check after a load must not replay the "formed" effect. */
    private boolean loadedFromDisk;

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

    /** Only lava buckets, Haunted logs and blaze powder count as fuel here - not vanilla's full fuel tag. */
    private static int getBurnDuration(ItemStack fuel) {
        if (fuel.is(Items.LAVA_BUCKET)) {
            return 20000; // same as a vanilla furnace - exactly fills an empty tank
        } else if (fuel.is(dev.zsskayr.merlins_inferno.registry.ModItems.HAUNTED_LOG_ITEM.get())) {
            return HAUNTED_LOG_BURN; // between a coal block (16000) and a lava bucket (20000)
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
            // Both vanilla types match most ores, and blasting is half the smelting time: take the quicker one, so the
            // forge is never slower than a Blast Furnace (smelting first would make it 140 ticks for iron against 100).
            Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> smelting = this.smeltingCheck.getRecipeFor(recipeInput, level);
            Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> blasting = this.blastingCheck.getRecipeFor(recipeInput, level);
            if (smelting.isPresent() && blasting.isPresent()) {
                found = blasting.get().value().getCookingTime() <= smelting.get().value().getCookingTime() ? blasting : smelting;
            } else {
                found = smelting.isPresent() ? smelting : blasting;
            }
        }
        return found.map(holder -> (RecipeHolder<AbstractCookingRecipe>) holder);
    }

    public boolean isFormed() {
        return this.formed;
    }

    /** The tier of the Core in the centre (1 = Demonic); 0 while not formed. */
    public int getCoreTier() {
        return this.coreTier;
    }

    /** Why the structure is not formed - only meaningful while {@link #isFormed()} is false. */
    public Component getStructureError() {
        return this.structureError;
    }

    /** Re-checks the 3x3x3 structure now (server side). */
    public void refreshStructure() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        HellForgeStructure.Result result = HellForgeStructure.validate(this.level, this.worldPosition,
                this.getBlockState().getValue(HellForgeControllerBlock.FACING));
        boolean wasFormed = this.formed;
        this.formed = result.formed();
        this.coreTier = result.coreTier();
        if (this.loadedFromDisk) {
            this.loadedFromDisk = false; // first check after a load: just learn the state, no effect
        } else if (this.formed != wasFormed && this.level instanceof ServerLevel serverLevel) {
            BlockPos centre = HellForgeStructure.corePos(this.worldPosition, this.getBlockState().getValue(HellForgeControllerBlock.FACING));
            if (this.formed) {
                emitFormedEffect(serverLevel, centre);
            } else {
                serverLevel.playSound(null, centre, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.8F, 0.8F);
            }
        }
        if (!result.formed()) {
            this.structureError = result.error();
        }
        this.structureCheckCountdown = STRUCTURE_CHECK_INTERVAL;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.formed && super.stillValid(player);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HellForgeBlockEntity blockEntity) {
        if (--blockEntity.structureCheckCountdown <= 0) {
            blockEntity.refreshStructure();
        }
        // Without the full structure the forge is inert: no fuel absorbed, nothing cooked.
        if (!blockEntity.formed) {
            setActive(level, pos, state, false);
            return;
        }

        boolean dirty = false;
        boolean working = false;

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
                if (fuel.isEmpty()) {
                    // The empty bucket goes back where the lava bucket came from, so the result slot
                    // stays clean for an output Vault; a Fuel Vault hands it back out (see HellForgeVaultHandler).
                    blockEntity.items.set(FUEL_SLOT, remainder);
                } else {
                    ejectRemainder(level, pos, blockEntity, remainder);
                }
            }
            dirty = true;
        }

        // No fuel and nothing half-cooked: no recipe can progress, so skip the lookup entirely.
        if (blockEntity.storedFuel <= 0 && blockEntity.cookingProgress <= 0) {
            setActive(level, pos, state, false);
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
            working = true;
            emitWorkingFeedback(level, pos, state);
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

        setActive(level, pos, state, working);
        if (dirty) {
            setChanged(level, pos, state);
        }
    }

    /**
     * Played once when the structure closes (like a Mekanism multiblock forming): a power-up sound,
     * flames bursting outwards along the 12 edges of the cube and a puff of smoke from each face.
     */
    private static void emitFormedEffect(ServerLevel level, BlockPos centre) {
        double cx = centre.getX() + 0.5;
        double cy = centre.getY() + 0.5;
        double cz = centre.getZ() + 0.5;
        level.playSound(null, centre, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7F, 1.4F);
        level.playSound(null, centre, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.7F);
        double[] steps = {-1.5, -0.75, 0.0, 0.75, 1.5};
        for (int axis = 0; axis < 3; axis++) {              // the axis the edge runs along
            for (int a = -1; a <= 1; a += 2) {
                for (int b = -1; b <= 1; b += 2) {          // the two fixed (outer) coordinates
                    for (double t : steps) {
                        double[] off = new double[3];
                        off[axis] = t;
                        off[(axis + 1) % 3] = a * 1.5;
                        off[(axis + 2) % 3] = b * 1.5;
                        double len = Math.sqrt(off[0] * off[0] + off[1] * off[1] + off[2] * off[2]);
                        // count 0 = the offsets are the particle's direction: fly outwards from the centre
                        level.sendParticles(ParticleTypes.FLAME, cx + off[0], cy + off[1], cz + off[2],
                                0, off[0] / len, off[1] / len, off[2] / len, 0.12);
                    }
                }
            }
        }
        for (Direction face : Direction.values()) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, cx + face.getStepX() * 1.6, cy + face.getStepY() * 1.6, cz + face.getStepZ() * 1.6,
                    0, face.getStepX(), face.getStepY(), face.getStepZ(), 0.05);
        }
    }

    /**
     * Drives the Controller's front-face animation and the Core's fireball animation: only changes
     * a blockstate when it actually flips.
     */
    private static void setActive(Level level, BlockPos pos, BlockState state, boolean active) {
        if (state.getValue(HellForgeControllerBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(HellForgeControllerBlock.ACTIVE, active), 3);
        }
        HellForgeControllerBlock.setCoreActive(level, pos, state, active);
    }

    /**
     * Furnace-style feedback while cooking, sent from the server: now and then a fire crackle from
     * the centre and smoke/flame puffs on the outside of the whole 3x3x3 structure (not just the Controller).
     */
    private static void emitWorkingFeedback(Level level, BlockPos pos, BlockState state) {
        RandomSource random = level.random;
        BlockPos centre = HellForgeStructure.corePos(pos, state.getValue(HellForgeControllerBlock.FACING));
        if (random.nextFloat() < 0.1F) {
            level.playSound(null, centre, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        if (random.nextFloat() < 0.3F && level instanceof ServerLevel serverLevel) {
            // A random point just outside one of the cube's six faces (the cube spans 1.5 blocks from its centre).
            Direction face = Direction.getRandom(random);
            double x = centre.getX() + 0.5 + (face.getAxis() == Direction.Axis.X ? face.getStepX() * 1.6 : random.nextDouble() * 3.0 - 1.5);
            double y = centre.getY() + 0.5 + (face.getAxis() == Direction.Axis.Y ? face.getStepY() * 1.6 : random.nextDouble() * 3.0 - 1.5);
            double z = centre.getZ() + 0.5 + (face.getAxis() == Direction.Axis.Z ? face.getStepZ() * 1.6 : random.nextDouble() * 3.0 - 1.5);
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            if (random.nextInt(3) == 0) {
                serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
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

    // --- WorldlyContainer ---
    // The Controller itself exposes no slots to hoppers: without Vaults the forge is fully manual.
    // Automation goes through HellForgeVaultBlock's item handler (see HellForgeVaultHandler) instead.

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
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
        this.loadedFromDisk = true;
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
