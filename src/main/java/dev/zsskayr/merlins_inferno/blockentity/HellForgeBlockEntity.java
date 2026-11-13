package dev.zsskayr.merlins_inferno.blockentity;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
import dev.zsskayr.merlins_inferno.forge.ForgeMath;
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
    public static final int MAX_FUEL_CAPACITY = BUCKET_FUEL * 12;
    public int fuelCapacity() { return structureCore == null ? FUEL_CAPACITY : shape.fuelBuckets(structureCore.layout()) * BUCKET_FUEL; }
    /** A Magmatic Core forge breeds its own fuel instead of smelting (see processMagmatic). */
    private boolean magmatic;
    /** The shell actually built (a Magmatic Core takes either). */
    @Nullable private net.minecraft.world.level.block.Block shellBlock;
    public boolean isMagmatic() { return magmatic; }
    /** The forge's built shape (width, height, roof); a closed 3x3x3 until it is formed. */
    private HellForgeStructure.Shape shape = HellForgeStructure.Shape.NONE;
    public HellForgeStructure.Shape shape() { return shape; }
    public static final int FUEL_PER_MB = 20;
    /** Face positions of the largest forge (9 wide, 11 tall, roofed): 4 walls of 7x9, a floor of 48 and a roof of 49. */
    public static final int MAX_PORTS = 350;
    /** After the ports: the screen's fuel cap and the cap in force (both in tenths of a percent) and the Redstone Vault signal. */
    public static final int DATA_EXTRA = 65 + MAX_PORTS * 3;
    public static final int DATA_COUNT = DATA_EXTRA + 3;
    private static final int[] NO_SLOTS = new int[0];

    public static int inputSlot(int lane) { return lane == 0 ? INPUT_SLOT : 3 + (lane - 1) * 2; }
    public static int outputSlot(int lane) { return lane == 0 ? RESULT_SLOT : 4 + (lane - 1) * 2; }
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final int[] heatedCounts = new int[LANES];
    private final double[] progress = new double[LANES];
    private final int[] overheat = new int[LANES];
    /** Each lane's stack has its own temperature: it goes in cold and warms towards the chamber at its own pace. */
    private final double[] itemTemperature = new double[LANES];
    /** The player's cap on the fuel level (0-1, set in the screen); the fuel level sets the fire's heat. */
    private double fillLimit = 1.0;
    /** The strongest signal reaching a Redstone Vault (0-15), or -1 when the forge has none: it scales the cap down. */
    private int redstoneLevel = -1;
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
    private boolean formed;
    private boolean autoSort;
    public void toggleAutoSort() { autoSort = !autoSort; setChanged(); }
    public boolean autoSortEnabled() { return autoSort; }
    private int coreTier;
    @Nullable private HellForgeStructure.Core structureCore;
    private int processingSlots;
    private int speedCatalysts;
    private int yieldCatalysts;
    // Blighted Core catalysts: steadier heat, faster heat changes, cheaper extra lanes, overheat tolerance.
    private int stabilizers;
    private int bellows;
    private int economizers;
    private int wards;
    private Component structureError = Component.translatable("message.merlins_inferno.hell_forge.incomplete");
    private int structureCheckCountdown;
    private boolean loadedFromDisk;
    // Client-facing fuel display: quantised fill (0-128), kind (0 none, 1 lava, 2 other) and shell radius (0 when unformed).
    private int renderFill;
    private int renderKind;
    private int renderRadius;
    private String renderFuel = "";
    /** The fuel shown in the pool (its texture, tint and particle), or null when empty. */
    public ForgeThermalData.Fuel renderFuel() {
        // While the pool drains away after the fuel is dumped or burnt out, keep drawing the fuel it was.
        if (level != null && !renderFuel.isEmpty()) lastShownFuel = renderFuel;
        return level == null || lastShownFuel.isEmpty() ? null : ForgeThermalData.fuelById(level, lastShownFuel);
    }
    private String lastShownFuel = "";
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
    /** Half width of the formed forge, 0 while unformed. */
    public int renderRadius() { return renderRadius & 15; }
    public HellForgeStructure.Shape renderShape() { return HellForgeStructure.Shape.unpack(renderRadius); }

    private void syncRender() {
        int fill = formed ? (int) Math.round(128.0 * Mth.clamp(fuelAmount / fuelCapacity(), 0, 1)) : 0;
        int kind = fill == 0 ? 0 : isLavaFuel() ? 1 : 2;
        int radius = formed && structureCore != null ? shape.pack() : 0;
        String shown = fill == 0 ? "" : fuelId;
        if (fill == renderFill && kind == renderKind && radius == renderRadius && shown.equals(renderFuel)) return;
        renderFuel = shown;
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
        tag.putString("RFuel", renderFuel);
        return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        renderFill = tag.getInt("RFill");
        renderKind = tag.getInt("RKind");
        renderRadius = tag.getInt("RRadius");
        renderFuel = tag.getString("RFuel");
    }
    @Nullable @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    public HellForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.HELL_FORGE.get(), pos, state);
        Arrays.fill(vaultMasks, 511);
        Arrays.fill(itemTemperature, ROOM);
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
        return fuel != null && (fuel.items().contains("minecraft:lava_bucket") || fuel.fluid().equals("minecraft:lava"));
    }
    /** The fluid the stored fuel is made of, or null when the fuel is a solid. */
    public net.minecraft.world.level.material.Fluid fuelFluid() {
        var fuel = fuel();
        if (fuel == null || fuel.fluid().isEmpty()) return null;
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse(fuel.fluid()));
    }
    /** Millibuckets of fuel fluid held (the same energy a solid fuel would count for). */
    public int fluidAmount() {
        var fuel = fuel();
        return fuel == null || fuel.fluid().isEmpty() ? 0 : (int) (fuelAmount / fuel.perMb());
    }
    /** Millibucket capacity of the reserve for the current (or default lava) fuel fluid. */
    public int fluidCapacity() {
        var fuel = fuel();
        double perMb = fuel == null || fuel.fluid().isEmpty() ? FUEL_PER_MB : fuel.perMb();
        return (int) (fuelCapacity() / perMb);
    }
    public int lavaAmount() { return isLavaFuel() ? fluidAmount() : 0; }
    public int fillLava(int amount, boolean simulate) {
        return fillFluid(net.minecraft.world.level.material.Fluids.LAVA, amount, simulate);
    }
    public int fillFluid(net.minecraft.world.level.material.Fluid fluid, int amount, boolean simulate) {
        if (level == null || !formed) return 0;
        var fuel = ForgeThermalData.fuelForFluid(level, fluid);
        if (fuel == null && fluid.isSame(net.minecraft.world.level.material.Fluids.LAVA)) fuel = ForgeThermalData.fuel(level, new ItemStack(Items.LAVA_BUCKET));
        if (fuel == null || !thermalCore().accepts(fuel) || (fuelAmount > 0 && !fuelId.equals(fuel.id()))) return 0;
        int accepted = Math.min(amount, (int) ((fuelLimitAmount() - fuelAmount) / fuel.perMb()));
        if (!simulate && accepted > 0) {
            fuelId = fuel.id();
            fuelAmount += accepted * fuel.perMb();
            setChanged();
        }
        return Math.max(0, accepted);
    }
    public void dumpFuel() {
        fuelAmount = 0;
        fuelId = "";
        setChanged();
    }

    public BlockPos vaultPos(int face) {
        if (structureCore == null || face < 0) return null;
        var faces = shape.faces();
        return face < faces.size() ? structureCore.pos().offset(faces.get(face)) : null;
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
        magmatic = result.formed() && result.core() != null && result.core().block().magmatic();
        shellBlock = result.shell();
        shape = result.shape();
        speedCatalysts = result.speedCatalysts();
        yieldCatalysts = result.yieldCatalysts();
        stabilizers = result.catalysts()[2];
        bellows = result.catalysts()[3];
        economizers = result.catalysts()[4];
        wards = result.catalysts()[5];
        if (previous != null && !previous.equals(structureCore)) setCoreActive(level, previous, false, false);
        if (!formed) structureError = result.error();
        if (loadedFromDisk) {
            loadedFromDisk = false;
            if (!formed && getBlockState().getValue(HellForgeControllerBlock.ACTIVE)) setCoreActive(level, result.core(), false, false);
        } else if (formed && !wasFormed && level instanceof ServerLevel server) {
            emitFormedEffect(server, structureCore, shape, ForgeThermalData.particle(fuel()));
        }
        structureCheckCountdown = 20;
    }

    /**
     * Water on the grill: the chamber drops by the coolant's chill at once (never below the core's rest), with a hiss and
     * a cloud of steam; the fire climbs back to where the fuel level puts it.
     */
    public void quench(double chill, int count) {
        if (level == null || chill <= 0 || count <= 0) return;
        var core = thermalCore();
        temperature = Math.max(core.rest(), temperature - chill * count);
        actualTemperature = temperature;
        if (level instanceof ServerLevel server && structureCore != null) {
            BlockPos centre = structureCore.pos();
            double cy = centre.getY() + 1.0 + shape.height() * 0.6;
            server.playSound(null, centre.above(Math.max(1, shape.height() / 2)), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
            server.sendParticles(ParticleTypes.CLOUD, centre.getX() + 0.5, cy, centre.getZ() + 0.5, 12 + 6 * count,
                    shape.radius() * 0.4, 0.4, shape.radius() * 0.4, 0.04);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, centre.getX() + 0.5, cy, centre.getZ() + 0.5, 4 + 2 * count,
                    shape.radius() * 0.3, 0.3, shape.radius() * 0.3, 0.02);
        }
        setChanged();
    }

    /** A coolant left in the fuel slot is thrown in at once; a bucket comes back empty. */
    private boolean absorbCoolant() {
        ItemStack stack = items.get(FUEL_SLOT);
        double chill = ForgeThermalData.chill(level, stack);
        if (chill <= 0) return false;
        // one at a time, about once a second, like tossing ice onto a grill - not a whole stack in a blink
        if (level.getGameTime() % 20 != 0) return true;
        ItemStack remainder = stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
        if (!remainder.isEmpty() && !fits(BUCKET_SLOT, remainder)) return true;
        stack.shrink(1);
        if (!remainder.isEmpty()) addResult(BUCKET_SLOT, remainder);
        quench(chill, 1);
        return true;
    }

    /**
     * An open-topped forge takes what falls into it: fuel items join the tank (up to the cap), coolants and snowballs
     * thrown or shot in quench it. A dropper or dispenser above it is all the automation it needs.
     */
    private void catchFalling() {
        if (shape.roof() || structureCore == null) return;
        int r = shape.radius() - 1;
        BlockPos core = structureCore.pos();
        net.minecraft.world.phys.AABB pool = new net.minecraft.world.phys.AABB(core.getX() - r, core.getY() + 1, core.getZ() - r,
                core.getX() + r + 1, core.getY() + shape.height() + 2, core.getZ() + r + 1);
        for (var snowball : level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Snowball.class, pool, e -> e.isAlive())) {
            snowball.discard();
            quench(ForgeThermalData.chill(level, new ItemStack(Items.SNOWBALL)), 1);
        }
        for (var dropped : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, pool, e -> e.isAlive())) {
            ItemStack stack = dropped.getItem();
            double chill = ForgeThermalData.chill(level, stack);
            if (chill > 0) {
                quench(chill, stack.getCount());
                ItemStack remainder = stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
                if (!remainder.isEmpty()) { remainder.setCount(stack.getCount()); dropped.setItem(remainder); dropped.setPos(dropped.getX(), core.getY() + shape.height() + 0.5, dropped.getZ()); }
                else dropped.discard();
                continue;
            }
            var fuel = ForgeThermalData.fuel(level, stack);
            if (fuel == null || !thermalCore().accepts(fuel) || (fuelAmount > 0 && !fuelId.equals(fuel.id()))) continue;
            int each = fuel.amountFor(itemId(stack));
            int taken = 0;
            while (taken < stack.getCount() && fuelAmount + each <= fuelLimitAmount()) { fuelAmount += each; taken++; }
            if (taken > 0) {
                fuelId = fuel.id();
                stack.shrink(taken);
                if (stack.isEmpty()) dropped.discard(); else dropped.setItem(stack);
                level.playSound(null, core.above(1), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.8F, 1.0F);
                setChanged();
            }
        }
    }

    private void absorbFuel() {
        if (absorbCoolant()) return;
        ItemStack stack = items.get(FUEL_SLOT);
        var next = ForgeThermalData.fuel(level, stack);
        if (!thermalCore().accepts(next) || (fuelAmount > 0 && !fuelId.equals(next.id()))
                || fuelAmount + next.amountFor(itemId(stack)) > fuelLimitAmount()) return;
        ItemStack remainder = stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
        if (!remainder.isEmpty() && !fits(BUCKET_SLOT, remainder)) return;
        stack.shrink(1);
        if (!remainder.isEmpty()) addResult(BUCKET_SLOT, remainder);
        fuelId = next.id();
        fuelAmount += next.amountFor(itemId(stack));
    }

    private static String itemId(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
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
            setActive(level, pos, state, null, false, false);
            forge.syncRender();
            forge.setChanged();
            return;
        }
        if (level.getGameTime() % 10 == 0) forge.tendRedstoneVaults();
        if (level.getGameTime() % 4 == 0) forge.catchFalling();
        if (level.getGameTime() % 2 == 0) forge.scaldPool();
        forge.absorbFuel();
        if (forge.autoSort) forge.balanceInputs();
        boolean running = forge.process(core);
        var shownFuel = forge.fuel();
        setActive(level, pos, state, forge.structureCore, running, running && shownFuel != null && shownFuel.soulGlow());
        if (running) emitWorkingFeedback(level, forge.structureCore, forge.shape, ForgeThermalData.particle(forge.fuel()));
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

    /** The thermal rules of this forge's shell block. */
    private ForgeThermalData.Shell shellRule() {
        if (shellBlock != null) return ForgeThermalData.shell(level, shellBlock);
        return structureCore == null ? ForgeThermalData.shell(level) : ForgeThermalData.shell(level, structureCore.block().shellBlock());
    }

    /** Ticks an item may sit past its critical temperature before it turns to scrap (Wards stretch it). */
    private static final int SCRAP_TICKS = 60;
    private static final double ROOM = 293.15;

    /** The fuel level the forge may hold: the screen's cap, scaled by a Redstone Vault's signal when there is one. */
    public double effectiveFillLimit() {
        return redstoneLevel < 0 ? fillLimit : fillLimit * redstoneLevel / 15.0;
    }

    private double fuelLimitAmount() {
        // In a Magmatic forge the bar is the reserve kept for the fire, not a cap: it may always be filled up.
        return magmatic ? fuelCapacity() : fuelCapacity() * effectiveFillLimit();
    }

    /** Fuel a Magmatic forge may give out: whatever lies above its reserve. */
    public double extractableFuel() {
        return magmatic ? Math.max(0, fuelAmount - fuelCapacity() * effectiveFillLimit()) : 0;
    }

    /** Takes up to {@code mb} millibuckets of the stored fluid out of a Magmatic forge (above its reserve); returns what was taken. */
    public int drainFluid(int mb, boolean simulate) {
        var fuel = fuel();
        if (!magmatic || fuel == null || fuel.fluid().isEmpty()) return 0;
        int taken = (int) Math.min(mb, extractableFuel() / fuel.perMb());
        if (!simulate && taken > 0) {
            fuelAmount -= taken * fuel.perMb();
            setChanged();
        }
        return Math.max(0, taken);
    }

    /** The chamber's temperature as a comparator signal: 0 at the core's rest, 15 at the hottest this forge can reach. */
    public int temperatureSignal() {
        var core = thermalCore();
        double top = Math.min(core.max(), shellRule().max());
        return (int) Math.round(15 * Mth.clamp((actualTemperature - core.rest()) / Math.max(1, top - core.rest()), 0, 1));
    }

    /** True while any lane holds a stack that is too hot or burning. */
    public boolean overheating() {
        for (int lane = 0; lane < processingSlots; lane++) if (status[lane] == 3 || status[lane] == 4) return true;
        return false;
    }

    /** Reads every Redstone Vault's input and pokes the comparators reading them. */
    private void tendRedstoneVaults() {
        int strongest = -1;
        var faces = shape.faces();
        for (int face = 0; face < faces.size(); face++) {
            BlockPos pos = vaultPos(face);
            if (pos == null || !level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof HellForgeVaultBlock) || state.getValue(HellForgeVaultBlock.MODE) != HellForgeVaultBlock.Mode.REDSTONE) continue;
            strongest = Math.max(strongest, level.getBestNeighborSignal(pos));
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        if (strongest != redstoneLevel) {
            redstoneLevel = strongest;
            setChanged();
        }
    }

    /**
     * The liquid's volume in the world for a given fill (0-1): the hollow above the floor, up to the fuel's surface.
     * Null for the 3x3x3 (no hollow) or an unformed forge. Built from the synced shape so client and server agree.
     */
    @Nullable
    public net.minecraft.world.phys.AABB poolBox(float fill) {
        var poolShape = renderShape();
        int r = renderRadius();
        if (r < 2 || fill <= 0.004F) return null;
        Direction facing = getBlockState().getValue(HellForgeControllerBlock.FACING);
        BlockPos core = worldPosition.relative(facing.getOpposite(), r);
        int depth = poolShape.height() - (poolShape.roof() ? 2 : 1);
        double top = core.getY() + 1 + depth * fill;
        return new net.minecraft.world.phys.AABB(core.getX() - (r - 1), core.getY() + 1, core.getZ() - (r - 1),
                core.getX() + r, top, core.getZ() + r);
    }

    /** Whatever is in the pool burns like in lava, and mobs wade through it slowly; players move themselves (client side). */
    private void scaldPool() {
        var pool = poolBox((float) Mth.clamp(fuelAmount / Math.max(1, fuelCapacity()), 0, 1));
        if (pool == null) return;
        for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, pool, e -> e.isAlive() && !e.isSpectator())) {
            // fuel and coolants thrown in are taken by catchFalling, not burnt
            if (entity instanceof net.minecraft.world.entity.item.ItemEntity dropped
                    && (ForgeThermalData.fuel(level, dropped.getItem()) != null || ForgeThermalData.chill(level, dropped.getItem()) > 0)) continue;
            entity.resetFallDistance();
            if (!entity.fireImmune()) entity.lavaHurt();
            if (!(entity instanceof net.minecraft.world.entity.player.Player)) {
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5, 0.8, 0.5));
            }
        }
    }

    /** Client side: lets the local player swim in the pool and tints the view while under it. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, HellForgeBlockEntity forge) {
        dev.zsskayr.merlins_inferno.client.HellForgePoolClient.tick(forge);
    }

    public void setFillLimit(double limit) {
        fillLimit = Mth.clamp(limit, 0, 1);
        setChanged();
    }

    /**
     * One tick of the forge, a grill rather than a pot: the fire's heat follows the fuel level (an empty tank is the fuel's
     * coolest, a full one its hottest), the chamber climbs towards it, and every lane's stack warms towards the chamber at
     * its own pace. A stack cooks while it is inside its range; past its maximum it keeps cooking but is held until it
     * cools back into range; past its critical temperature it turns to scrap within a few seconds.
     */
    /**
     * The Magmatic Core: no lanes, one input and one bucket. The pool keeps itself hot with its own fuel (its level sets the
     * fire, as usual); once the chamber is hot enough, the stone in the input melts into more of that same fuel. Output
     * only takes what lies above the reserve the bar sets. Soul Lava recipes need a Blightsteel shell.
     */
    private boolean processMagmatic(ForgeThermalData.Core core) {
        var shell = shellRule();
        var fuel = fuel();
        if (!core.accepts(fuel)) fuel = null;
        double maximum = Math.min(core.max(), shell.max());
        if (fuel != null) maximum = Math.min(maximum, fuel.max());
        double minimum = fuel == null ? core.min() : Math.max(core.min(), fuel.min());
        boolean burning = fuel != null && fuelAmount > 0;
        double level01 = Mth.clamp(fuelAmount / Math.max(1, fuelCapacity()), 0, 1);
        targetTemperature = burning ? Mth.clamp(fuel.min() + (fuel.max() - fuel.min()) * level01, minimum, maximum) : core.rest();
        double structureMass = core.capacity() + shell.capacity() * shellBlockCount();
        if (burning) {
            double response = ForgeMath.response(fuel.response(), structureMass + fuelMass(fuel), false, 0);
            temperature = ForgeMath.heat(temperature, targetTemperature, core.rest(), core.efficient(), maximum, response);
            fuelAmount = Math.max(0, fuelAmount - fuel.burnRate());
            double damping = structureMass / (structureMass + fuelMass(fuel));
            actualTemperature = temperature + ForgeMath.flicker(level.getGameTime(), core.stability(), shell.stability(), fuel.flicker(), damping, 0)
                    * (shape.roof() ? 1 : 1.5);
        } else {
            temperature += (core.rest() - temperature) * core.cooling() * (shape.roof() ? 1 : 1.5);
            actualTemperature = temperature;
        }
        for (int lane = 0; lane < LANES; lane++) status[lane] = lane == 0 ? 0 : 6;
        ItemStack input = items.get(inputSlot(0));
        var melting = ForgeThermalData.melting(level, input);
        if (melting == null || fuel == null || !melting.fluid().equals(fuel.fluid()) || melting.minCoreTier() > coreTier) {
            progress[0] = 0;
            duration[0] = 0;
        } else {
            duration[0] = melting.time();
            if (actualTemperature < melting.min()) status[0] = 1;
            else if (fuelAmount + melting.amount() * fuel.perMb() > fuelCapacity()) status[0] = 5;
            else {
                status[0] = 2;
                progress[0] += 1;
                if (progress[0] >= melting.time()) {
                    progress[0] = 0;
                    input.shrink(1);
                    fuelAmount += melting.amount() * fuel.perMb();
                    level.playSound(null, worldPosition, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.6F, 0.9F);
                }
            }
        }
        // An empty bucket in the output is filled from what lies above the reserve.
        ItemStack out = items.get(outputSlot(0));
        if (out.is(Items.BUCKET) && out.getCount() == 1 && fuel != null && !fuel.fluid().isEmpty() && extractableFuel() >= 1000 * fuel.perMb()) {
            var filled = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fuel.fluid())).getBucket();
            if (filled != Items.AIR) {
                fuelAmount -= 1000 * fuel.perMb();
                items.set(outputSlot(0), new ItemStack(filled));
            }
        }
        if (fuelAmount <= 0) fuelId = "";
        return burning;
    }

    private boolean process(ForgeThermalData.Core core) {
        if (magmatic) return processMagmatic(core);
        var shell = shellRule();
        // A dross-eating core (Blighted) burns the dross left in its slot: while there is some, it heats faster and yields more.
        boolean consumesDross = core.consumesDross();
        boolean drossActive = consumesDross && items.get(DROSS_SLOT).is(ModItems.DUSK_DROSS.get());
        var fuel = fuel();
        if (!core.accepts(fuel)) fuel = null;
        double maximum = Math.min(core.max(), shell.max());
        if (fuel != null) maximum = Math.min(maximum, fuel.max());
        double minimum = fuel == null ? core.min() : Math.max(core.min(), fuel.min());
        boolean burning = fuel != null && fuelAmount > 0;
        double level01 = Mth.clamp(fuelAmount / Math.max(1, fuelCapacity()), 0, 1);
        targetTemperature = burning ? Mth.clamp(fuel.min() + (fuel.max() - fuel.min()) * level01, minimum, maximum) : core.rest();
        double structureMass = core.capacity() + shell.capacity() * shellBlockCount();
        if (burning) {
            double response = ForgeMath.response(fuel.response(), structureMass + fuelMass(fuel), drossActive, bellows);
            temperature = ForgeMath.heat(temperature, targetTemperature, core.rest(), core.efficient(), maximum, response);
            fuelAmount = Math.max(0, fuelAmount - fuel.burnRate());
        } else temperature += (core.rest() - temperature) * core.cooling() * (shape.roof() ? 1 : 1.5);
        double flicker = 0;
        if (burning) {
            double damping = structureMass / (structureMass + fuelMass(fuel));
            flicker = ForgeMath.flicker(level.getGameTime(), core.stability(), shell.stability(), fuel.flicker(), damping, stabilizers)
                    * (shape.roof() ? 1 : 1.5);
        }
        double actual = temperature + flicker;
        actualTemperature = actual;

        @SuppressWarnings("unchecked")
        RecipeHolder<AbstractCookingRecipe>[] current = new RecipeHolder[LANES];
        ForgeThermalData.Material[] materials = new ForgeThermalData.Material[LANES];
        boolean[] ready = new boolean[LANES];
        boolean[] gated = new boolean[LANES];
        for (int lane = 0; lane < LANES; lane++) {
            status[lane] = lane < processingSlots ? 0 : 6;
            if (lane >= processingSlots) continue;
            ItemStack input = items.get(inputSlot(lane));
            double laneChill = ForgeThermalData.chill(level, input);
            if (laneChill > 0) {
                // ice among the raw items: thrown onto the fire one at a time, about once a second
                if (level.getGameTime() % 20 == lane % 20) {
                    ItemStack remainder = input.hasCraftingRemainingItem() ? input.getCraftingRemainingItem() : ItemStack.EMPTY;
                    if (remainder.isEmpty() || fits(BUCKET_SLOT, remainder)) {
                        input.shrink(1);
                        if (!remainder.isEmpty()) addResult(BUCKET_SLOT, remainder);
                        quench(laneChill, 1);
                    }
                }
                resetLane(lane);
                continue;
            }
            current[lane] = ForgeThermalData.recipe(level, input).orElse(null);
            if (current[lane] == null) { itemTemperature[lane] = ROOM; heatedCounts[lane] = 0; continue; }
            var recipe = current[lane];
            if (recipes[lane] != null && !recipes[lane].equals(recipe.id())) { resetLane(lane); itemTemperature[lane] = ROOM; heatedCounts[lane] = 0; }
            recipes[lane] = recipe.id();
            materials[lane] = ForgeThermalData.material(level, input, recipe.id());
            var material = materials[lane];
            if (material.minCoreTier() > coreTier) { gated[lane] = true; status[lane] = 8; continue; }
            applyThermalLoad(lane, input, material, core, shell);
            itemTemperature[lane] = ForgeMath.itemHeat(itemTemperature[lane], actual, material.heatCapacity());
            double speed = ForgeMath.speed(material.min(), core.efficient(), maximum, speedCatalysts, core.speedBonus(), drossActive);
            duration[lane] = ForgeMath.duration(recipe.value().getCookingTime(), recipe.value().getType() == RecipeType.BLASTING, speed);
            ItemStack output = recipe.value().assemble(new SingleRecipeInput(input), level.registryAccess());
            ready[lane] = fits(outputSlot(lane), output)
                    && (consumesDross || material.drossChance() + yieldCatalysts * 0.05 == 0
                    || fits(DROSS_SLOT, new ItemStack(ModItems.DUSK_DROSS.get())));
            if (!ready[lane]) status[lane] = 5;
        }
        // Every lane that is really cooking adds load: the others slow down and the fuel burns faster.
        int working = 0;
        for (int lane = 0; lane < processingSlots; lane++)
            if (current[lane] != null && !gated[lane] && ready[lane] && itemTemperature[lane] >= materials[lane].min()) working++;
        double laneSpeed = ForgeMath.laneSpeed(working);
        double laneFuel = ForgeMath.laneFuel(working, economizers);
        for (int lane = 0; lane < processingSlots; lane++) {
            if (current[lane] == null || gated[lane]) continue;
            var material = materials[lane];
            ItemStack input = items.get(inputSlot(lane));
            double heat = itemTemperature[lane];
            int scrapTicks = (int) (SCRAP_TICKS * (1 + 0.25 * wards));
            double criticalTemperature = material.critical() + 25 * wards;
            if (heat >= criticalTemperature) {
                critical[lane] = Math.min(scrapTicks, critical[lane] + 1);
                status[lane] = 4;
                if (critical[lane] >= scrapTicks && fits(SCRAP_SLOT, new ItemStack(ModItems.FORGED_SCRAP.get()))) {
                    input.shrink(1);
                    heatedCounts[lane] = input.getCount();
                    addResult(SCRAP_SLOT, new ItemStack(ModItems.FORGED_SCRAP.get()));
                    progress[lane] = 0;
                    critical[lane] = 0;
                }
                continue;
            }
            critical[lane] = Math.max(0, critical[lane] - 2);
            if (heat < material.min()) { status[lane] = 1; continue; }
            if (!ready[lane]) continue;
            // Earlier lanes may have filled the shared residue output during this same tick.
            if (!consumesDross && material.drossChance() + yieldCatalysts * 0.05 > 0
                    && !fits(DROSS_SLOT, new ItemStack(ModItems.DUSK_DROSS.get()))) {
                status[lane] = 5;
                continue;
            }
            boolean tooHot = heat > material.max();
            if (progress[lane] < duration[lane]) {
                // The fuel can be gone (emptied or data reloaded) while the forge still holds heat.
                if (fuel == null) { status[lane] = 7; continue; }
                double processingCost = ForgeMath.processingCost(duration[lane], laneFuel) * fuel.burnRate();
                if (fuelAmount < processingCost) { status[lane] = 7; continue; }
                progress[lane] = Math.min(duration[lane], progress[lane] + laneSpeed);
                fuelAmount = Math.max(0, fuelAmount - processingCost);
            }
            status[lane] = tooHot ? 3 : 2;
            // Done, but a stack that is too hot is held until it settles back into its range.
            if (progress[lane] >= duration[lane] && !tooHot) {
                ItemStack output = current[lane].value().assemble(new SingleRecipeInput(input), level.registryAccess());
                if (!fits(outputSlot(lane), output)) continue;
                double doubleChance = ForgeMath.doubleChance(yieldCatalysts, core.yieldBonus(), drossActive);
                if (level.random.nextFloat() < doubleChance && fits(outputSlot(lane), output.copyWithCount(output.getCount() * 2)))
                    output.setCount(output.getCount() * 2);
                addResult(outputSlot(lane), output);
                if (consumesDross) {
                    if (drossActive && level.random.nextFloat() < 0.3F) items.get(DROSS_SLOT).shrink(1);
                } else if (level.random.nextFloat() < Math.min(1, material.drossChance() + yieldCatalysts * 0.05))
                    addResult(DROSS_SLOT, new ItemStack(ModItems.DUSK_DROSS.get()));
                input.shrink(1);
                heatedCounts[lane] = input.getCount();
                resetLane(lane);
                recipes[lane] = current[lane].id();
            }
        }
        if (fuelAmount <= 0) fuelId = "";
        return burning;
    }

    /** Thermal mass of the stored fuel, per bucket-equivalent, capped so a full tank cannot freeze the forge. */
    private double fuelMass(ForgeThermalData.Fuel fuel) {
        if (fuel == null) return 0;
        return ForgeMath.fuelMass(fuel.heatCapacity(), fuelAmount);
    }

    private int shellBlockCount() {
        return shape.shellBlocks();
    }

    // Track newly inserted mass in the container, preserving ordinary item components/stacking.
    private void applyThermalLoad(int lane, ItemStack input, ForgeThermalData.Material material,
                                  ForgeThermalData.Core core, ForgeThermalData.Shell shell) {
        int added = Math.max(0, input.getCount() - heatedCounts[lane]);
        heatedCounts[lane] = input.getCount();
        if (added == 0) return;
        int before = Math.max(0, input.getCount() - added);
        itemTemperature[lane] = before == 0 ? ROOM : (itemTemperature[lane] * before + ROOM * added) / (before + added);
        // Like meat on a grill: the fire barely notices a cold piece going in.
        double structureMass = core.capacity() + shell.capacity() * shellBlockCount() + fuelMass(fuel());
        double itemMass = material.heatCapacity() * added * 0.1;
        temperature = (temperature * structureMass + ROOM * itemMass) / (structureMass + itemMass);
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
    private static void emitFormedEffect(ServerLevel level, HellForgeStructure.Core core, HellForgeStructure.Shape shape, net.minecraft.core.particles.SimpleParticleType flame) {
        BlockPos centre = core.pos();
        double edge = core.layout().radius() + 0.5;
        double cx = centre.getX() + 0.5;
        double cy = centre.getY() + 0.5 + shape.centreY();
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
                        level.sendParticles(flame, cx + off[0], cy + off[1], cz + off[2],
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
    private static void setActive(Level level, BlockPos pos, BlockState state, @Nullable HellForgeStructure.Core core, boolean active, boolean soul) {
        if (state.getValue(HellForgeControllerBlock.ACTIVE) != active || state.getValue(HellForgeControllerBlock.SOUL) != soul) {
            level.setBlock(pos, state.setValue(HellForgeControllerBlock.ACTIVE, active).setValue(HellForgeControllerBlock.SOUL, soul), 3);
        }
        setCoreActive(level, core, active, soul);
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
        setCoreActive(this.level, core, false, false);
    }

    private static void setCoreActive(Level level, @Nullable HellForgeStructure.Core core, boolean active, boolean soul) {
        if (core == null || !level.hasChunkAt(core.pos())) {
            return;
        }
        BlockState state = level.getBlockState(core.pos());
        if (state.is(core.block()) && (state.getValue(HellForgeCoreBlock.ACTIVE) != active || state.getValue(HellForgeCoreBlock.SOUL) != soul)) {
            level.setBlock(core.pos(), state.setValue(HellForgeCoreBlock.ACTIVE, active).setValue(HellForgeCoreBlock.SOUL, soul), 3);
        }
    }

    /**
     * Furnace-style feedback while cooking, sent from the server: now and then a fire crackle from
     * the centre and smoke/flame puffs on the outside of the structure.
     */
    private static void emitWorkingFeedback(Level level, @Nullable HellForgeStructure.Core core, HellForgeStructure.Shape shape, net.minecraft.core.particles.SimpleParticleType flame) {
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
            double half = shape.height() / 2.0;
            double y = centre.getY() + 0.5 + shape.centreY() + (face.getAxis() == Direction.Axis.Y ? face.getStepY() * (half + 0.1) : random.nextDouble() * 2 * half - half);
            double z = centre.getZ() + 0.5 + (face.getAxis() == Direction.Axis.Z ? face.getStepZ() * (edge + 0.1) : random.nextDouble() * 2 * edge - edge);
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            if (random.nextInt(3) == 0) {
                serverLevel.sendParticles(flame, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
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
                case 2 -> (int) Math.min(32767, Math.round(itemTemperature[lane]));
                case 3 -> Math.min(32767, critical[lane]);
                default -> Math.min(32767, duration[lane]);
            };
        }
        if (index >= DATA_EXTRA) {
            return switch (index - DATA_EXTRA) {
                case 0 -> (int) Math.round(fillLimit * 1000);
                case 1 -> (int) Math.round(effectiveFillLimit() * 1000);
                default -> redstoneLevel;
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
            case 9 -> (int) Math.round(Math.min(shellRule().max(), fuel == null ? core.max() : Math.min(core.max(), fuel.max())));
            case 12 -> autoSort ? 1 : 0;
            case 13 -> structureCore == null ? 0 : structureCore.pos().getX() & 65535;
            case 14 -> structureCore == null ? 0 : structureCore.pos().getX() >>> 16;
            case 15 -> structureCore == null ? 0 : structureCore.pos().getY() & 65535;
            case 16 -> structureCore == null ? 0 : structureCore.pos().getY() >>> 16;
            case 17 -> structureCore == null ? 0 : structureCore.pos().getZ() & 65535;
            case 18 -> structureCore == null ? 0 : structureCore.pos().getZ() >>> 16;
            case 19 -> coreTier | (magmatic ? 16 : 0);
            case 10 -> structureCore == null ? HellForgeStructure.Shape.NONE.pack() : shape.pack();
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
        if (slot == FUEL_SLOT) return thermalCore().accepts(ForgeThermalData.fuel(level, stack)) || ForgeThermalData.chill(level, stack) > 0;
        if (slot == DROSS_SLOT) return !magmatic && thermalCore().consumesDross() && stack.is(ModItems.DUSK_DROSS.get());
        if (magmatic && slot == inputSlot(0)) return ForgeThermalData.melting(level, stack) != null;
        if (magmatic && slot == outputSlot(0)) return stack.is(Items.BUCKET);
        for (int lane = 0; lane < processingSlots; lane++)
            if (slot == inputSlot(lane)) return ForgeThermalData.recipe(level, stack).isPresent() || ForgeThermalData.chill(level, stack) > 0;
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
            renderFuel = tag.getString("RFuel");
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
            itemTemperature[lane] = saved.contains("ItemTemperature") ? saved.getDouble("ItemTemperature") : ROOM;
        }
        if (!tag.contains("Lane0")) progress[0] = Math.max(0, tag.getInt("CookTime"));
        fillLimit = tag.contains("FillLimit") ? Mth.clamp(tag.getDouble("FillLimit"), 0, 1) : 1.0;
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
        tag.putDouble("FillLimit", fillLimit);
        tag.putIntArray("VaultMasks", vaultMasks);
        for (int lane = 0; lane < LANES; lane++) {
            CompoundTag saved = new CompoundTag();
            saved.putInt("HeatedCount", heatedCounts[lane]);
            saved.putDouble("Progress", progress[lane]);
            saved.putInt("Overheat", overheat[lane]);
            saved.putInt("Critical", critical[lane]);
            saved.putDouble("ItemTemperature", itemTemperature[lane]);
            tag.put("Lane" + lane, saved);
        }
    }
}
