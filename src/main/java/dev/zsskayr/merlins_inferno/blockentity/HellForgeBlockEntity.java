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

import dev.zsskayr.merlins_inferno.recipe.HellForgeRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.block.HellForgeControllerBlock;
import dev.zsskayr.merlins_inferno.block.HellForgeCoreBlock;
import dev.zsskayr.merlins_inferno.menu.HellForgeMenu;
import dev.zsskayr.merlins_inferno.multiblock.HellForgeStructure;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModRecipeTypes;

import java.util.Arrays;
import dev.zsskayr.merlins_inferno.forge.ForgeThermalData;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock;

/** Independent processing lanes sharing one thermal chamber and a typed fuel reserve. */
public class HellForgeBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int LANES = 9;
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    public static final int DROSS_SLOT = 19;
    public static final int SCRAP_SLOT = 20;
    public static final int BUCKET_SLOT = 21;
    public static final int SLOT_COUNT = 22;
    public static final int BUCKET_FUEL = 20000;
    /** Capacity of the smallest forge, and the ceiling used when loading before the structure is known. */
    public static final int FUEL_CAPACITY = BUCKET_FUEL;
    public static final int MAX_FUEL_CAPACITY = BUCKET_FUEL * 10;
    public int fuelCapacity() { return structureCore == null ? FUEL_CAPACITY : structureCore.layout().fuelBuckets() * BUCKET_FUEL; }
    public static final int FUEL_PER_MB = 20;
    public static final int MAX_PORTS = 294;
    public static final int DATA_COUNT = 65 + MAX_PORTS * 3;
    private static final int[] NO_SLOTS = new int[0];

    public static int inputSlot(int lane) { return lane == 0 ? INPUT_SLOT : 3 + (lane - 1) * 2; }
    public static int outputSlot(int lane) { return lane == 0 ? RESULT_SLOT : 4 + (lane - 1) * 2; }
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final int[] heatedCounts = new int[LANES];
    private final double[] progress = new double[LANES];
    private final int[] overheat = new int[LANES];
    private final int[] critical = new int[LANES];
    private final int[] waiting = new int[LANES];
    private final int[] status = new int[LANES];
    private final int[] duration = new int[LANES];
    private final int[] vaultMasks = new int[MAX_PORTS];
    private final net.minecraft.resources.ResourceLocation[] recipes = new net.minecraft.resources.ResourceLocation[LANES];
    private double fuelAmount;
    private String fuelId = "";
    private double temperature = 473.15;
    private double actualTemperature = 473.15;
    private double targetTemperature = 473.15;
    private int selectedLane = -1;
    private boolean formed;
    private boolean autoSort;
    public void toggleAutoSort() { autoSort = !autoSort; setChanged(); }
    public boolean autoSortEnabled() { return autoSort; }
    private int coreTier;
    @Nullable private HellForgeStructure.Core structureCore;
    private int processingSlots;
    private int speedCatalysts;
    private int yieldCatalysts;
    private Component structureError = Component.translatable("message.merlins_inferno.hell_forge.incomplete");
    private int structureCheckCountdown;
    private boolean loadedFromDisk;
    // Client-facing fuel display: quantised fill (0-128), kind (0 none, 1 lava, 2 other) and shell radius (0 when unformed).
    private int renderFill;
    private int renderKind;
    private int renderRadius;
    public int renderFill() { return renderFill; }
    private float shownFill;
    private long lastAnimation;
    /** Eases the displayed level towards the target so fuel rises and drains smoothly instead of stepping. */
    public float animatedFill(float target) {
        long now = System.nanoTime();
        float dt = (now - lastAnimation) / 1.0E9F;
        if (lastAnimation == 0 || dt > 1F) shownFill = target;
        else shownFill += (target - shownFill) * (1F - (float) Math.exp(-dt * 2.5F));
        lastAnimation = now;
        return shownFill;
    }
    public int renderKind() { return renderKind; }
    public int renderRadius() { return renderRadius; }

    private void syncRender() {
        int fill = formed ? (int) Math.round(128.0 * Mth.clamp(fuelAmount / fuelCapacity(), 0, 1)) : 0;
        int kind = fill == 0 ? 0 : isLavaFuel() ? 1 : 2;
        int radius = formed && structureCore != null ? structureCore.layout().radius() : 0;
        if (fill == renderFill && kind == renderKind && radius == renderRadius) return;
        renderFill = fill;
        renderKind = kind;
        renderRadius = radius;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("RFill", renderFill);
        tag.putInt("RKind", renderKind);
        tag.putInt("RRadius", renderRadius);
        return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        renderFill = tag.getInt("RFill");
        renderKind = tag.getInt("RKind");
        renderRadius = tag.getInt("RRadius");
    }
    @Nullable @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    public HellForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.HELL_FORGE.get(), pos, state);
        Arrays.fill(vaultMasks, 511);
    }

    public boolean isFormed() { return formed; }
    boolean isFormedAround(HellForgeStructure.Core core) { return core.equals(structureCore); }
    public int getCoreTier() { return coreTier; }
    public Component getStructureError() { return structureError; }
    public int getProcessingSlots() { return processingSlots; }

    private ForgeThermalData.Core thermalCore() {
        return structureCore == null ? ForgeThermalData.core(level) : structureCore.block().thermalProfile(level);
    }

    private ForgeThermalData.Fuel fuel() {
        return level == null ? null : ForgeThermalData.fuels(level).stream()
                .filter(f -> f.id().equals(fuelId)).findFirst().orElse(null);
    }
    public boolean isLavaFuel() {
        var fuel = fuel();
        return fuel != null && fuel.item().equals("minecraft:lava_bucket");
    }
    public int lavaAmount() { return isLavaFuel() ? (int) fuelAmount / FUEL_PER_MB : 0; }
    public int fillLava(int amount, boolean simulate) {
        if (level == null || !formed) return 0;
        var lava = ForgeThermalData.fuel(level, new ItemStack(Items.LAVA_BUCKET));
        if (!thermalCore().accepts(lava) || (fuelAmount > 0 && !fuelId.equals(lava.id()))) return 0;
        int accepted = Math.min(amount, (int) (fuelCapacity() - fuelAmount) / FUEL_PER_MB);
        if (!simulate && accepted > 0) {
            fuelId = lava.id();
            fuelAmount += accepted * FUEL_PER_MB;
            setChanged();
        }
        return accepted;
    }
    public void dumpFuel() {
        fuelAmount = 0;
        fuelId = "";
        setChanged();
    }

    public BlockPos vaultPos(int face) {
        if (structureCore == null || face < 0) return null;
        int r = structureCore.layout().radius();
        int index = 0;
        for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
            int axes = (Math.abs(x) == r ? 1 : 0) + (Math.abs(y) == r ? 1 : 0) + (Math.abs(z) == r ? 1 : 0);
            if (axes == 1 && index++ == face) return structureCore.pos().offset(x, y, z);
        }
        return null;
    }

    public int vaultFace(BlockPos pos) {
        for (int face = 0; face < MAX_PORTS; face++) if (pos.equals(vaultPos(face))) return face;
        return -1;
    }
    public int vaultMask(BlockPos pos) {
        int face = vaultFace(pos);
        return face < 0 ? 0 : vaultMasks[face];
    }
    public void configureVault(int face, int action) {
        BlockPos pos = vaultPos(face);
        if (pos == null || level == null || !level.hasChunkAt(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof HellForgeVaultBlock)) return;
        if (action == -1) state = state.cycle(HellForgeVaultBlock.MODE);
        else if (action == -2) state = state.cycle(HellForgeVaultBlock.AUTO);
        else if (action >= 0 && action < LANES) vaultMasks[face] ^= 1 << action;
        level.setBlock(pos, state, 3);
        if (state.getValue(HellForgeVaultBlock.AUTO))
            level.scheduleTick(pos, state.getBlock(), HellForgeVaultBlock.AUTO_INTERVAL);
        setChanged();
    }

    public void refreshStructure() {
        if (level == null || level.isClientSide) return;
        var result = HellForgeStructure.validate(level, worldPosition, getBlockState().getValue(HellForgeControllerBlock.FACING));
        var previous = structureCore;
        boolean wasFormed = formed;
        structureCore = result.formed() ? result.core() : null;
        formed = result.formed();
        coreTier = result.coreTier();
        processingSlots = result.processingSlots();
        speedCatalysts = result.speedCatalysts();
        yieldCatalysts = result.yieldCatalysts();
        if (previous != null && !previous.equals(structureCore)) setCoreActive(level, previous, false);
        if (!formed) structureError = result.error();
        if (loadedFromDisk) {
            loadedFromDisk = false;
            if (!formed && getBlockState().getValue(HellForgeControllerBlock.ACTIVE)) setCoreActive(level, result.core(), false);
        } else if (formed && !wasFormed && level instanceof ServerLevel server) {
            emitFormedEffect(server, structureCore);
        }
        structureCheckCountdown = 20;
    }

    private void absorbFuel() {
        ItemStack stack = items.get(FUEL_SLOT);
        var next = ForgeThermalData.fuel(level, stack);
        if (!thermalCore().accepts(next) || (fuelAmount > 0 && !fuelId.equals(next.id()))
                || fuelAmount + next.amount() > fuelCapacity()) return;
        ItemStack remainder = stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
        if (!remainder.isEmpty() && !fits(BUCKET_SLOT, remainder)) return;
        stack.shrink(1);
        if (!remainder.isEmpty()) addResult(BUCKET_SLOT, remainder);
        fuelId = next.id();
        fuelAmount += next.amount();
    }

    private boolean fits(int slot, ItemStack result) {
        ItemStack existing = items.get(slot);
        return !result.isEmpty() && (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, result))
                && existing.getCount() + result.getCount() <= Math.min(getMaxStackSize(), result.getMaxStackSize());
    }
    private void addResult(int slot, ItemStack result) {
        if (items.get(slot).isEmpty()) items.set(slot, result.copy());
        else items.get(slot).grow(result.getCount());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HellForgeBlockEntity forge) {
        if (--forge.structureCheckCountdown <= 0) forge.refreshStructure();
        var core = forge.thermalCore();
        if (!forge.formed) {
            forge.temperature += (core.rest() - forge.temperature) * core.cooling();
            setActive(level, pos, state, null, false);
            forge.syncRender();
            forge.setChanged();
            return;
        }
        forge.absorbFuel();
        if (forge.autoSort) forge.balanceInputs();
        boolean running = forge.process(core);
        setActive(level, pos, state, forge.structureCore, running);
        if (running) emitWorkingFeedback(level, forge.structureCore);
        forge.syncRender();
        forge.setChanged();
    }

    private void balanceInputs() {
        // Only identical routing groups may exchange ingredients. Progress stays on its lane.
        for (int source = 0; source < processingSlots; source++) {
            ItemStack stack = items.get(inputSlot(source));
            if (stack.isEmpty()) continue;
            for (int target = 0; target < processingSlots; target++) {
                if (source == target) continue;
                boolean sameRoute = true;
                for (int mask : vaultMasks) if (((mask >> source) & 1) != ((mask >> target) & 1)) { sameRoute = false; break; }
                if (!sameRoute) continue;
                ItemStack other = items.get(inputSlot(target));
                if (!other.isEmpty() && !ItemStack.isSameItemSameComponents(stack, other)) continue;
                int move = (stack.getCount() - other.getCount()) / 2;
                if (move <= 0) continue;
                // Keep the item currently being processed in place, including its exposure timers.
                move = Math.min(move, stack.getCount() - 1);
                int hot = Math.min(move, heatedCounts[source]);
                if (other.isEmpty()) { resetLane(target); items.set(inputSlot(target), stack.copyWithCount(move)); }
                else other.grow(move);
                stack.shrink(move);
                heatedCounts[source] -= hot;
                heatedCounts[target] += hot;
            }
        }
    }

    private boolean process(ForgeThermalData.Core core) {
        var shell = ForgeThermalData.shell(level);
        var fuel = fuel();
        if (!core.accepts(fuel)) fuel = null;
        double maximum = Math.min(core.max(), shell.max());
        if (fuel != null) maximum = Math.min(maximum, fuel.max());
        double minimum = fuel == null ? core.min() : Math.max(core.min(), fuel.min());
        @SuppressWarnings("unchecked")
        RecipeHolder<AbstractCookingRecipe>[] current = new RecipeHolder[LANES];
        ForgeThermalData.Material[] materials = new ForgeThermalData.Material[LANES];
        boolean[] ready = new boolean[LANES];
        int oldest = -1;
        for (int lane = 0; lane < LANES; lane++) {
            status[lane] = lane < processingSlots ? 0 : 6;
            if (lane >= processingSlots) continue;
            ItemStack input = items.get(inputSlot(lane));
            current[lane] = ForgeThermalData.recipe(level, input).orElse(null);
            if (current[lane] == null) continue;
            var recipe = current[lane];
            if (recipes[lane] != null && !recipes[lane].equals(recipe.id())) resetLane(lane);
            recipes[lane] = recipe.id();
            materials[lane] = ForgeThermalData.material(level, input, recipe.id());
            var material = materials[lane];
            applyThermalLoad(lane, input, material, core, shell);
            double difficulty = Mth.clamp((material.min() - core.efficient()) / Math.max(1, maximum - core.efficient()), 0, 1);
            double speed = (1.50 - 0.48 * difficulty) * (1 + speedCatalysts * 0.15);
            int blastTime = recipe.value().getType() == RecipeType.BLASTING
                    ? recipe.value().getCookingTime() : Math.max(1, recipe.value().getCookingTime() / 2);
            duration[lane] = Math.max(1, (int) Math.floor(blastTime / speed));
            ItemStack output = recipe.value().assemble(new SingleRecipeInput(input), level.registryAccess());
            ready[lane] = fits(outputSlot(lane), output)
                    && (material.drossChance() + yieldCatalysts * 0.05 == 0
                    || fits(DROSS_SLOT, new ItemStack(ModItems.DUSK_DROSS.get())));
            if (!ready[lane]) { status[lane] = 5; continue; }
            waiting[lane] = Math.min(1000000, waiting[lane] + 1);
            if (material.min() <= maximum && material.max() >= minimum
                    && (oldest < 0 || waiting[lane] > waiting[oldest])) oldest = lane;
        }
        // Keep the selected recipe until it completes, then pick the longest-waiting lane.
        if (selectedLane < 0 || selectedLane >= processingSlots || !ready[selectedLane]
                || materials[selectedLane].min() > maximum || materials[selectedLane].max() < minimum)
            selectedLane = oldest;
        boolean heating = fuel != null && fuelAmount > 0 && selectedLane >= 0;
        targetTemperature = core.rest();
        if (heating) {
            var material = materials[selectedLane];
            targetTemperature = Mth.clamp((material.min() + material.max()) / 2, minimum, maximum);
            // The chamber and the fuel bath share heat: the bath adds thermal mass, so a big lava reserve
            // reacts slowly and steadily while a little coal heats quickly but flickers.
            double structureMass = core.capacity() + shell.capacity() * shellBlockCount();
            double mass = structureMass + fuelMass(fuel);
            double response = fuel.response() * mass / 318.0;
            double hot = Mth.clamp((targetTemperature - core.efficient()) / Math.max(1, maximum - core.efficient()), 0, 1);
            double rate = (1 - Math.pow(0.1, 1 / response)) / (1 + hot * 1.2);
            // Aim past the target so the climb stays brisk instead of crawling in on it asymptotically,
            // then hold exactly on the target once it is reached. Cooling down towards a lower target stays smooth.
            double drive = Math.min(maximum, targetTemperature + 0.35 * Math.max(0, targetTemperature - core.rest()));
            double before = temperature;
            temperature += (drive - temperature) * rate;
            if (before <= targetTemperature && temperature > targetTemperature) temperature = targetTemperature;
            fuelAmount = Math.max(0, fuelAmount - 1);
        } else temperature += (core.rest() - temperature) * core.cooling();
        double flicker = 0;
        if (heating) {
            double t = level.getGameTime();
            double structureMass = core.capacity() + shell.capacity() * shellBlockCount();
            double damping = structureMass / (structureMass + fuelMass(fuel));
            flicker = Math.sin(t * 0.07) * core.stability() * shell.stability()
                    + (Math.sin(t * 0.31) + Math.sin(t * 0.113 + 1.7)) * 0.5 * fuel.flicker() * damping;
        }
        double actual = temperature + flicker;
        actualTemperature = actual;
        // Every lane that is really working adds load: the others slow down and the fuel burns faster.
        int working = 0;
        for (int lane = 0; lane < processingSlots; lane++)
            if (current[lane] != null && ready[lane] && actual >= materials[lane].min() && actual <= materials[lane].max()) working++;
        double load = Math.max(0, working - 1);
        double laneSpeed = 1.0 / (1.0 + 0.08 * load);
        double laneFuel = 1.0 + 0.12 * load;
        for (int lane = 0; lane < processingSlots; lane++) {
            if (current[lane] == null) continue;
            var material = materials[lane];
            ItemStack input = items.get(inputSlot(lane));
            if (actual > material.max()) {
                overheat[lane] = Math.min(Integer.MAX_VALUE - 1, overheat[lane]) + 1;
                status[lane] = overheat[lane] <= material.grace() ? 3 : 4;
                if (overheat[lane] > material.grace()) progress[lane] = Math.max(0, progress[lane] - material.decay());
                critical[lane] = actual >= material.critical() ? Math.min(material.criticalTicks(), critical[lane] + 1) : 0;
                if (critical[lane] >= material.criticalTicks() && fits(SCRAP_SLOT, new ItemStack(ModItems.FORGED_SCRAP.get()))) {
                    input.shrink(1);
                    heatedCounts[lane] = input.getCount();
                    addResult(SCRAP_SLOT, new ItemStack(ModItems.FORGED_SCRAP.get()));
                    resetLane(lane);
                    if (selectedLane == lane) selectedLane = -1;
                }
                continue;
            }
            overheat[lane] = Math.max(0, overheat[lane] - 1);
            critical[lane] = 0;
            if (actual < material.min()) { status[lane] = 1; continue; }
            if (!ready[lane]) continue;
            // Earlier lanes may have filled the shared residue output during this same tick.
            if (material.drossChance() + yieldCatalysts * 0.05 > 0
                    && !fits(DROSS_SLOT, new ItemStack(ModItems.DUSK_DROSS.get()))) {
                status[lane] = 5;
                continue;
            }
            double processingCost = 200.0 / duration[lane] * laneFuel;
            if (fuelAmount < processingCost || fuel == null) { status[lane] = 7; continue; }
            progress[lane] += laneSpeed;
            fuelAmount = Math.max(0, fuelAmount - processingCost);
            status[lane] = 2;
            if (progress[lane] >= duration[lane]) {
                ItemStack output = current[lane].value().assemble(new SingleRecipeInput(input), level.registryAccess());
                if (!fits(outputSlot(lane), output)) continue;
                if (level.random.nextFloat() < yieldCatalysts * 0.10 && fits(outputSlot(lane), output.copyWithCount(output.getCount() * 2)))
                    output.setCount(output.getCount() * 2);
                addResult(outputSlot(lane), output);
                if (level.random.nextFloat() < Math.min(1, material.drossChance() + yieldCatalysts * 0.05))
                    addResult(DROSS_SLOT, new ItemStack(ModItems.DUSK_DROSS.get()));
                input.shrink(1);
                heatedCounts[lane] = input.getCount();
                resetLane(lane);
                if (selectedLane == lane) selectedLane = -1;
            }
        }
        if (fuelAmount <= 0) fuelId = "";
        return heating;
    }

    /** Thermal mass of the stored fuel, per bucket-equivalent, capped so a full tank cannot freeze the forge. */
    private double fuelMass(ForgeThermalData.Fuel fuel) {
        if (fuel == null) return 0;
        return Math.min(fuel.heatCapacity() * fuelAmount / BUCKET_FUEL, 318.0);
    }

    private int shellBlockCount() {
        if (structureCore == null) return 26;
        int size = structureCore.layout().radius() * 2 + 1;
        return size * size * size - (size - 2) * (size - 2) * (size - 2);
    }

    // Track newly inserted mass in the container, preserving ordinary item components/stacking.
    private void applyThermalLoad(int lane, ItemStack input, ForgeThermalData.Material material,
                                  ForgeThermalData.Core core, ForgeThermalData.Shell shell) {
        int added = Math.max(0, input.getCount() - heatedCounts[lane]);
        heatedCounts[lane] = input.getCount();
        if (added == 0) return;
        double structureMass = core.capacity() + shell.capacity() * shellBlockCount() + fuelMass(fuel());
        double itemMass = material.heatCapacity() * added;
        temperature = (temperature * structureMass + 293.15 * itemMass) / (structureMass + itemMass);
    }
    private void resetLane(int lane) {
        progress[lane] = 0;
        overheat[lane] = 0;
        critical[lane] = 0;
        waiting[lane] = 0;
        recipes[lane] = null;
    }
    /**
     * Played once when the structure closes (like a Mekanism multiblock forming): a power-up sound,
     * flames bursting outwards along the 12 edges of the cube and a puff of smoke from each face.
     */
    private static void emitFormedEffect(ServerLevel level, HellForgeStructure.Core core) {
        BlockPos centre = core.pos();
        double edge = core.layout().radius() + 0.5;
        double cx = centre.getX() + 0.5;
        double cy = centre.getY() + 0.5;
        double cz = centre.getZ() + 0.5;
        level.playSound(null, centre, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7F, 1.4F);
        level.playSound(null, centre, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.7F);
        double[] steps = {-edge, -edge / 2, 0.0, edge / 2, edge};
        for (int axis = 0; axis < 3; axis++) {              // the axis the edge runs along
            for (int a = -1; a <= 1; a += 2) {
                for (int b = -1; b <= 1; b += 2) {          // the two fixed (outer) coordinates
                    for (double t : steps) {
                        double[] off = new double[3];
                        off[axis] = t;
                        off[(axis + 1) % 3] = a * edge;
                        off[(axis + 2) % 3] = b * edge;
                        double len = Math.sqrt(off[0] * off[0] + off[1] * off[1] + off[2] * off[2]);
                        // count 0 = the offsets are the particle's direction: fly outwards from the centre
                        level.sendParticles(ParticleTypes.FLAME, cx + off[0], cy + off[1], cz + off[2],
                                0, off[0] / len, off[1] / len, off[2] / len, 0.12);
                    }
                }
            }
        }
        for (Direction face : Direction.values()) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, cx + face.getStepX() * (edge + 0.1), cy + face.getStepY() * (edge + 0.1), cz + face.getStepZ() * (edge + 0.1),
                    0, face.getStepX(), face.getStepY(), face.getStepZ(), 0.05);
        }
    }

    /**
     * Drives the Controller's front-face animation and the Core's fireball animation: only changes
     * a blockstate when it actually flips.
     */
    private static void setActive(Level level, BlockPos pos, BlockState state, @Nullable HellForgeStructure.Core core, boolean active) {
        if (state.getValue(HellForgeControllerBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(HellForgeControllerBlock.ACTIVE, active), 3);
        }
        setCoreActive(level, core, active);
    }

    public void clearCoreAnimation(BlockState controllerState) {
        if (this.level == null) {
            return;
        }
        HellForgeStructure.Core core = this.structureCore;
        if (core == null && controllerState.getValue(HellForgeControllerBlock.ACTIVE)) {
            core = HellForgeStructure.findCoreFromController(this.level, this.worldPosition,
                    controllerState.getValue(HellForgeControllerBlock.FACING));
        }
        setCoreActive(this.level, core, false);
    }

    private static void setCoreActive(Level level, @Nullable HellForgeStructure.Core core, boolean active) {
        if (core == null || !level.hasChunkAt(core.pos())) {
            return;
        }
        BlockState state = level.getBlockState(core.pos());
        if (state.is(core.block()) && state.getValue(HellForgeCoreBlock.ACTIVE) != active) {
            level.setBlock(core.pos(), state.setValue(HellForgeCoreBlock.ACTIVE, active), 3);
        }
    }

    /**
     * Furnace-style feedback while cooking, sent from the server: now and then a fire crackle from
     * the centre and smoke/flame puffs on the outside of the structure.
     */
    private static void emitWorkingFeedback(Level level, @Nullable HellForgeStructure.Core core) {
        if (core == null) {
            return;
        }
        RandomSource random = level.random;
        BlockPos centre = core.pos();
        if (random.nextFloat() < 0.1F) {
            level.playSound(null, centre, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        if (random.nextFloat() < 0.3F && level instanceof ServerLevel serverLevel) {
            double edge = core.layout().radius() + 0.5;
            Direction face = Direction.getRandom(random);
            double x = centre.getX() + 0.5 + (face.getAxis() == Direction.Axis.X ? face.getStepX() * (edge + 0.1) : random.nextDouble() * 2 * edge - edge);
            double y = centre.getY() + 0.5 + (face.getAxis() == Direction.Axis.Y ? face.getStepY() * (edge + 0.1) : random.nextDouble() * 2 * edge - edge);
            double z = centre.getZ() + 0.5 + (face.getAxis() == Direction.Axis.Z ? face.getStepZ() * (edge + 0.1) : random.nextDouble() * 2 * edge - edge);
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            if (random.nextInt(3) == 0) {
                serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }


    private int dataValue(int index) {
        if (index >= 20 && index < 65) {
            int lane = (index - 20) / 5;
            int field = (index - 20) % 5;
            return switch (field) {
                case 0 -> duration[lane] == 0 ? 0 : (int) (1000 * progress[lane] / duration[lane]);
                case 1 -> status[lane];
                case 2 -> Math.min(32767, overheat[lane]);
                case 3 -> Math.min(32767, critical[lane]);
                default -> Math.min(32767, duration[lane]);
            };
        }
        if (index >= 65) {
            int face = (index - 65) / 3;
            BlockPos pos = vaultPos(face);
            BlockState state = pos == null ? null : level.getBlockState(pos);
            boolean present = state != null && state.getBlock() instanceof HellForgeVaultBlock;
            return switch ((index - 65) % 3) {
                case 0 -> present ? state.getValue(HellForgeVaultBlock.MODE).ordinal() : -1;
                case 1 -> present && state.getValue(HellForgeVaultBlock.AUTO) ? 1 : 0;
                default -> vaultMasks[face];
            };
        }
        var core = thermalCore();
        var fuel = fuel();
        return switch (index) {
            case 0 -> (int) fuelAmount;
            case 1 -> fuelCapacity();
            case 2 -> (int) Math.round(actualTemperature);
            case 3 -> (int) Math.round(targetTemperature);
            case 4 -> processingSlots;
            case 5 -> speedCatalysts;
            case 6 -> yieldCatalysts;
            case 7 -> fuel == null ? -1 : ForgeThermalData.fuels(level).indexOf(fuel);
            case 8 -> (int) Math.round(fuel == null ? core.min() : Math.max(core.min(), fuel.min()));
            case 9 -> (int) Math.round(Math.min(ForgeThermalData.shell(level).max(), fuel == null ? core.max() : Math.min(core.max(), fuel.max())));
            case 12 -> autoSort ? 1 : 0;
            case 13 -> structureCore == null ? 0 : structureCore.pos().getX() & 65535;
            case 14 -> structureCore == null ? 0 : structureCore.pos().getX() >>> 16;
            case 15 -> structureCore == null ? 0 : structureCore.pos().getY() & 65535;
            case 16 -> structureCore == null ? 0 : structureCore.pos().getY() >>> 16;
            case 17 -> structureCore == null ? 0 : structureCore.pos().getZ() & 65535;
            case 18 -> structureCore == null ? 0 : structureCore.pos().getZ() >>> 16;
            case 10 -> structureCore == null ? 1 : structureCore.layout().radius();
            case 11 -> getBlockState().getValue(HellForgeControllerBlock.FACING).ordinal();
            default -> 0;
        };
    }
    private final ContainerData dataAccess = new ContainerData() {
        public int get(int index) { return dataValue(index); }
        public void set(int index, int value) {}
        public int getCount() { return DATA_COUNT; }
    };

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override public boolean stillValid(Player player) { return formed && super.stillValid(player); }
    @Override public void setItem(int slot, ItemStack stack) {
        ItemStack previous = items.get(slot);
        for (int lane = 0; lane < LANES; lane++) {
            if (slot == inputSlot(lane) && !ItemStack.isSameItemSameComponents(previous, stack)) {
                resetLane(lane);
                heatedCounts[lane] = 0;
            }
        }
        super.setItem(slot, stack);
        setChanged();
    }
    @Override public ItemStack removeItem(int slot, int count) {
        ItemStack removed = super.removeItem(slot, count);
        for (int lane = 0; lane < LANES; lane++) if (slot == inputSlot(lane)) {
            heatedCounts[lane] = Math.min(heatedCounts[lane], items.get(slot).getCount());
            if (items.get(slot).isEmpty()) resetLane(lane);
        }
        return removed;
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (level == null) return false;
        if (slot == FUEL_SLOT) return thermalCore().accepts(ForgeThermalData.fuel(level, stack));
        for (int lane = 0; lane < processingSlots; lane++)
            if (slot == inputSlot(lane)) return ForgeThermalData.recipe(level, stack).isPresent();
        return false;
    }
    @Override public int[] getSlotsForFace(Direction side) { return NO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override protected Component getDefaultName() { return Component.translatable("block.merlins_inferno.hell_forge"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new HellForgeMenu(id, inventory, this, dataAccess);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadedFromDisk = true;
        // Update packets reach loadAdditional, not handleUpdateTag, so the display values are read here too.
        if (tag.contains("RFill")) {
            renderFill = tag.getInt("RFill");
            renderKind = tag.getInt("RKind");
            renderRadius = tag.getInt("RRadius");
        }
        autoSort = tag.getBoolean("AutoSort");
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        fuelAmount = Mth.clamp(tag.getDouble("StoredFuel"), 0, MAX_FUEL_CAPACITY);
        if (!Double.isFinite(fuelAmount)) fuelAmount = 0;
        fuelId = tag.contains("FuelType") ? tag.getString("FuelType") : "merlins_inferno:fuel/lava";
        temperature = tag.contains("Temperature") ? tag.getDouble("Temperature") : 473.15;
        if (!Double.isFinite(temperature)) temperature = 473.15;
        actualTemperature = temperature;
        for (int lane = 0; lane < LANES; lane++) {
            CompoundTag saved = tag.getCompound("Lane" + lane);
            heatedCounts[lane] = Math.clamp(saved.getInt("HeatedCount"), 0, items.get(inputSlot(lane)).getCount());
            progress[lane] = Math.max(0, saved.getDouble("Progress"));
            if (!Double.isFinite(progress[lane])) progress[lane] = 0;
            overheat[lane] = Math.max(0, saved.getInt("Overheat"));
            critical[lane] = Math.max(0, saved.getInt("Critical"));
        }
        if (!tag.contains("Lane0")) progress[0] = Math.max(0, tag.getInt("CookTime"));
        int[] masks = tag.getIntArray("VaultMasks");
        for (int face = 0; face < MAX_PORTS; face++) vaultMasks[face] = face < masks.length ? masks[face] & 511 : 511;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putBoolean("AutoSort", autoSort);
        tag.putDouble("StoredFuel", fuelAmount);
        tag.putString("FuelType", fuelId);
        tag.putDouble("Temperature", temperature);
        tag.putIntArray("VaultMasks", vaultMasks);
        for (int lane = 0; lane < LANES; lane++) {
            CompoundTag saved = new CompoundTag();
            saved.putInt("HeatedCount", heatedCounts[lane]);
            saved.putDouble("Progress", progress[lane]);
            saved.putInt("Overheat", overheat[lane]);
            saved.putInt("Critical", critical[lane]);
            tag.put("Lane" + lane, saved);
        }
    }
}
