package dev.zsskayr.merlins_inferno.forge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import dev.zsskayr.merlins_inferno.registry.ModRecipeTypes;

/** Server datapack rules, mirrored to clients for recipe displays. All temperatures are Kelvin. */
public final class ForgeThermalData extends SimpleJsonResourceReloadListener {
    public record Material(double min, double max, double heatCapacity, int grace,
                           double decay, double critical, int criticalTicks, double drossChance, int minCoreTier) {}
    public record Core(double rest, double min, double max, double efficient,
                       double capacity, double stability, double cooling, String operation, List<String> allowedFuels,
                       double speedBonus, double yieldBonus, boolean consumesDross) {
        public boolean accepts(Fuel fuel) { return fuel != null && (fuel.items().stream().anyMatch(allowedFuels::contains) || (!fuel.fluid().isEmpty() && allowedFuels.contains(fuel.fluid()))); }
    }
    public record Fuel(String id, List<String> items, int amount, double min, double max, double response,
                        double heatCapacity, double flicker, String fluid,
                        String liquidTexture, int tint, String particle, boolean soulGlow, int uiColor,
                        Map<String, Integer> itemValues, double burnRate, boolean sinkable) {
        /** The energy one of this item adds: its own value when listed, the fuel_value otherwise. */
        public int amountFor(String item) { return itemValues.getOrDefault(item, amount); }
        public String item() { return items.isEmpty() ? "" : items.getFirst(); }
        /** Burn energy per millibucket of the fluid (fuel_value is the energy of 1000 mB). */
        public double perMb() { return amount / 1000.0; }
    }
    public record Shell(double max, double capacity, double stability) {}
    private record MaterialRule(JsonObject source, Material material) {}

    public static final double KELVIN_OFFSET = 273.15;
    public static final TagKey<net.minecraft.world.item.Item> EXTRA_INPUTS =
            TagKey.create(Registries.ITEM, id("hell_forge_inputs"));
    private static final Gson GSON = new Gson();
    private static JsonObject serverRules = new JsonObject();
    private static Rules server = parse(new JsonObject());
    private static Rules client = server;
    private record Rules(Material fallback, Map<String, Core> cores, Map<String, Shell> shells, List<Fuel> fuels, List<MaterialRule> materials,
                         Map<String, Double> coolants, List<Melting> meltings) {}
    /** A Magmatic Core recipe: these items melt into this much of a fuel fluid, once the chamber is hot enough. */
    public record Melting(List<String> items, String tag, String fluid, int amount, int time, double min, int minCoreTier) {
        public boolean matches(ItemStack stack) {
            if (items.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) return true;
            return !tag.isEmpty() && stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag)));
        }
    }
    private static final String DEFAULT_SHELL = "merlins_inferno:compressed_netherrack";

    public ForgeThermalData() { super(GSON, "hell_forge"); }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("merlins_inferno", path); }
    private static double number(JsonObject j, String name, double fallback) {
        double value = j.has(name) ? j.get(name).getAsDouble() : fallback;
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid " + name);
        return value;
    }
    private static Map<String, Integer> itemValues(JsonObject j) {
        Map<String, Integer> values = new java.util.HashMap<>();
        if (j.has("item_values")) {
            for (var e : j.getAsJsonObject("item_values").entrySet()) values.put(e.getKey(), e.getValue().getAsInt());
        }
        return Map.copyOf(values);
    }
    private static Material material(JsonObject j) {
        double min = number(j, "min_temperature", 1811.15);
        double max = number(j, "max_temperature", 1973.15);
        double critical = number(j, "critical_temperature", max + 150);
        double chance = number(j, "dross_chance", 0.10);
        if (max < min || critical <= max || chance > 1) throw new IllegalArgumentException("Invalid thermal range");
        return new Material(min, max, number(j, "heat_capacity", 12), (int) number(j, "overheat_grace_ticks", 100),
                number(j, "overheat_progress_decay", 0.5), critical,
                Math.max(1, (int) number(j, "critical_exposure_ticks", 400)), chance, (int) number(j, "min_core_tier", 1));
    }
    private static Rules parse(JsonObject documents) {
        Material fallback = material(new JsonObject());
        Core defaultCore = new Core(473.15, 873.15, 2173.15, 1373.15, 240, 5, 0.002,
                "heating", List.of("minecraft:coal", "minecraft:lava_bucket"), 0, 0, false);
        Map<String, Core> cores = new java.util.HashMap<>();
        cores.put("merlins_inferno:demonic_core", defaultCore);
        Shell shell = new Shell(2173.15, 3, 1);
        Map<String, Shell> shells = new java.util.HashMap<>();
        List<Fuel> fuels = new ArrayList<>();
        List<MaterialRule> materials = new ArrayList<>();
        Map<String, Double> coolants = new java.util.HashMap<>();
        List<Melting> meltings = new ArrayList<>();
        for (var entry : documents.entrySet()) {
            JsonObject j = entry.getValue().getAsJsonObject();
            switch (j.get("type").getAsString()) {
                case "default_material" -> fallback = material(j);
                case "material" -> materials.add(new MaterialRule(j, material(j)));
                case "core" -> {
                    List<String> allowedFuels = new ArrayList<>();
                    if (j.has("allowed_fuels")) j.getAsJsonArray("allowed_fuels").forEach(f -> allowedFuels.add(f.getAsString()));
                    else allowedFuels.addAll(defaultCore.allowedFuels());
                    Core core = new Core(number(j, "rest_temperature", defaultCore.rest),
                            number(j, "min_temperature", defaultCore.min), number(j, "max_temperature", defaultCore.max),
                            number(j, "efficient_temperature", defaultCore.efficient), Math.max(1, number(j, "heat_capacity", defaultCore.capacity)),
                            number(j, "stability", defaultCore.stability), number(j, "cooling_rate", defaultCore.cooling),
                            j.has("operation") ? j.get("operation").getAsString() : "heating", List.copyOf(allowedFuels),
                            number(j, "speed_bonus", 0), number(j, "yield_bonus", 0),
                            j.has("dross") && j.get("dross").getAsString().equals("consume"));
                    if (core.min > core.max || core.rest > core.min || core.efficient > core.max
                            || core.efficient < core.min || core.cooling > 1 || !core.operation.equals("heating"))
                        throw new IllegalArgumentException("Invalid or unsupported Core thermal profile");
                    cores.put(j.has("block") ? j.get("block").getAsString() : "merlins_inferno:demonic_core", core);
                }
                case "shell" -> shells.put(j.has("block") ? j.get("block").getAsString() : DEFAULT_SHELL,
                        new Shell(number(j, "max_temperature", shell.max),
                        number(j, "heat_capacity", shell.capacity), number(j, "stability", shell.stability)));
                case "fuel" -> {
                    int amount = (int) number(j, "fuel_value", 1600);
                    double min = number(j, "min_temperature", 873.15);
                    double max = number(j, "max_temperature", 1923.15);
                    double response = number(j, "response_ticks", 200);
                    if (amount < 1 || amount > 20000 || max < min || response < 1)
                        throw new IllegalArgumentException("Invalid fuel " + entry.getKey());
                    String fluid = j.has("fluid") ? j.get("fluid").getAsString() : "";
                    List<String> items = new ArrayList<>();
                    if (j.has("item")) items.add(j.get("item").getAsString());
                    if (j.has("items")) j.getAsJsonArray("items").forEach(e -> items.add(e.getAsString()));
                    if (j.has("item_values")) j.getAsJsonObject("item_values").keySet().forEach(k -> { if (!items.contains(k)) items.add(k); });
                    if (items.isEmpty() && fluid.isEmpty()) throw new IllegalArgumentException("Fuel needs an item or a fluid " + entry.getKey());
                    fuels.add(new Fuel(entry.getKey(), List.copyOf(items), amount, min, max, response,
                            Math.max(0, number(j, "heat_capacity", 20)), Math.max(0, number(j, "flicker", 5)), fluid,
                            j.has("liquid_texture") ? j.get("liquid_texture").getAsString() : "minecraft:block/lava_still",
                            j.has("tint") ? Integer.parseInt(j.get("tint").getAsString(), 16) : 0xFFFFFF,
                            j.has("particle") ? j.get("particle").getAsString() : "minecraft:flame",
                            j.has("glow") && j.get("glow").getAsString().equals("soul"),
                            j.has("ui_color") ? Integer.parseInt(j.get("ui_color").getAsString(), 16) : 0xF48C24,
                            itemValues(j), Math.max(0.05, number(j, "burn_rate", 1)),
                            !j.has("sinkable") || j.get("sinkable").getAsBoolean()));
                }
                // Water on the grill: an item that drops the chamber's temperature at once; the fire then climbs back.
                case "coolant" -> coolants.put(j.get("item").getAsString(), number(j, "chill", 100));
                case "melting" -> {
                    List<String> items = new ArrayList<>();
                    if (j.has("item")) items.add(j.get("item").getAsString());
                    if (j.has("items")) j.getAsJsonArray("items").forEach(e -> items.add(e.getAsString()));
                    meltings.add(new Melting(List.copyOf(items), j.has("tag") ? j.get("tag").getAsString() : "",
                            j.get("fluid").getAsString(), (int) number(j, "amount", 100), Math.max(1, (int) number(j, "time", 100)),
                            number(j, "min_temperature", 1273.15), (int) number(j, "min_core_tier", 1)));
                }
                default -> throw new IllegalArgumentException("Unknown Hell Forge rule " + entry.getKey());
            }
        }
        materials.sort(Comparator.comparingDouble((MaterialRule r) -> number(r.source, "priority", 0)).reversed());
        shells.putIfAbsent(DEFAULT_SHELL, shell);
        return new Rules(fallback, Map.copyOf(cores), Map.copyOf(shells), List.copyOf(fuels), List.copyOf(materials), Map.copyOf(coolants), List.copyOf(meltings));
    }
    @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        JsonObject next = new JsonObject();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> next.add(e.getKey().toString(), e.getValue()));
        Rules parsed = parse(next);
        serverRules = next;
        server = parsed;
    }
    public static String encode() { return GSON.toJson(serverRules); }
    public static void receive(String json) {
        JsonObject next = GSON.fromJson(json, JsonObject.class);
        Rules parsed = parse(next);
        client = parsed;
    }
    private static Rules rules(Level level) { return level.isClientSide ? client : server; }
    public static Core core(Level level) { return rules(level).cores.get("merlins_inferno:demonic_core"); }
    public static Core core(Level level, net.minecraft.world.level.block.Block block) {
        return rules(level).cores.getOrDefault(BuiltInRegistries.BLOCK.getKey(block).toString(), core(level));
    }
    public static Shell shell(Level level) { return rules(level).shells.get(DEFAULT_SHELL); }
    public static Shell shell(Level level, net.minecraft.world.level.block.Block block) {
        return rules(level).shells.getOrDefault(BuiltInRegistries.BLOCK.getKey(block).toString(), shell(level));
    }
    public static List<Fuel> fuels(Level level) { return rules(level).fuels; }
    public static Fuel fuelById(Level level, String id) {
        return fuels(level).stream().filter(f -> f.id.equals(id)).findFirst().orElse(null);
    }
    /** The particle a fuel burns with, falling back to a plain flame. */
    public static net.minecraft.core.particles.SimpleParticleType particle(Fuel fuel) {
        if (fuel != null && BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse(fuel.particle)) instanceof net.minecraft.core.particles.SimpleParticleType type)
            return type;
        return net.minecraft.core.particles.ParticleTypes.FLAME;
    }
    public static Fuel fuelForFluid(Level level, net.minecraft.world.level.material.Fluid fluid) {
        String id = BuiltInRegistries.FLUID.getKey(fluid).toString();
        return fuels(level).stream().filter(f -> f.fluid.equals(id)).findFirst().orElse(null);
    }
    /** The Magmatic Core recipe for this item, or null. */
    public static Melting melting(Level level, ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Melting melting : rules(level).meltings) if (melting.matches(stack)) return melting;
        return null;
    }
    public static List<Melting> meltings(Level level) { return rules(level).meltings; }

    /** How many degrees one of this item takes off the chamber, or 0 when it is no coolant. */
    public static double chill(Level level, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return rules(level).coolants.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), 0.0);
    }
    public static Fuel fuel(Level level, ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return fuels(level).stream().filter(f -> f.items.contains(id)).findFirst().orElse(null);
    }
    public static Material material(Level level, ItemStack input, ResourceLocation recipe) {
        for (MaterialRule rule : rules(level).materials) {
            JsonObject j = rule.source;
            if (j.has("recipe") && !j.get("recipe").getAsString().equals(recipe == null ? "" : recipe.toString())) continue;
            if (j.has("tag") && !input.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(j.get("tag").getAsString())))) continue;
            if (j.has("items")) {
                String item = BuiltInRegistries.ITEM.getKey(input.getItem()).toString();
                boolean matches = false;
                for (JsonElement element : j.getAsJsonArray("items")) matches |= element.getAsString().equals(item);
                if (!matches) continue;
            }
            return rule.material;
        }
        return rules(level).fallback;
    }
    /** One row of the materials table: the items a rule covers and its thermal profile. */
    public record MaterialEntry(List<ItemStack> items, Material material) {}

    /** What one core can reach: its tier and the lowest/highest temperature its shell and accepted fuels allow. */
    public record CoreReach(String block, int tier, double low, double high) {
        public boolean fits(Material material) {
            return material.minCoreTier() <= tier && material.min() <= high && material.max() >= low;
        }
    }

    /** Every material the forge knows, coolest first, the default (iron and any other blast recipe) included. */
    public static List<MaterialEntry> materialTable(Level level) {
        List<MaterialEntry> table = new ArrayList<>();
        for (MaterialRule rule : rules(level).materials) {
            List<ItemStack> items = new ArrayList<>();
            JsonObject j = rule.source;
            if (j.has("items")) {
                for (JsonElement element : j.getAsJsonArray("items")) {
                    BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(element.getAsString())).ifPresent(item -> items.add(new ItemStack(item)));
                }
            }
            if (j.has("tag")) {
                TagKey<net.minecraft.world.item.Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(j.get("tag").getAsString()));
                BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> items.add(new ItemStack(holder.value())));
            }
            if (!items.isEmpty()) table.add(new MaterialEntry(List.copyOf(items), rule.material));
        }
        table.add(new MaterialEntry(List.of(new ItemStack(net.minecraft.world.item.Items.RAW_IRON), new ItemStack(net.minecraft.world.item.Items.IRON_ORE)),
                rules(level).fallback));
        table.sort(Comparator.comparingDouble(e -> e.material().min()));
        return table;
    }

    /** Every core's reach, lowest tier first. */
    public static List<CoreReach> coreReaches(Level level) {
        Rules rules = rules(level);
        List<CoreReach> reaches = new ArrayList<>();
        for (var entry : rules.cores.entrySet()) {
            var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.getKey()));
            if (!(block instanceof dev.zsskayr.merlins_inferno.block.HellForgeCoreBlock coreBlock)) continue;
            Core core = entry.getValue();
            Shell shell = rules.shells.getOrDefault(BuiltInRegistries.BLOCK.getKey(coreBlock.shellBlock()).toString(), rules.shells.get(DEFAULT_SHELL));
            double high = 0, low = Double.MAX_VALUE;
            for (Fuel fuel : rules.fuels) {
                if (!core.accepts(fuel)) continue;
                high = Math.max(high, Math.min(Math.min(core.max, shell.max), fuel.max));
                low = Math.min(low, Math.max(core.min, fuel.min));
            }
            if (high > 0) reaches.add(new CoreReach(entry.getKey(), coreBlock.getTier(), low, high));
        }
        reaches.sort(Comparator.comparingInt(CoreReach::tier));
        return reaches;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional<RecipeHolder<AbstractCookingRecipe>> recipe(Level level, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        var input = new SingleRecipeInput(stack);
        var manager = level.getRecipeManager();
        Optional found = manager.getRecipeFor(ModRecipeTypes.HELL_FORGE_SMELTING.get(), input, level);
        if (found.isEmpty()) found = manager.getRecipeFor(RecipeType.BLASTING, input, level);
        if (found.isEmpty() && stack.is(EXTRA_INPUTS)) found = manager.getRecipeFor(RecipeType.SMELTING, input, level);
        return found;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<RecipeHolder<AbstractCookingRecipe>> allRecipes(Level level) {
        List<RecipeHolder<AbstractCookingRecipe>> recipes = new ArrayList<>();
        recipes.addAll((List) level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.HELL_FORGE_SMELTING.get()));
        recipes.addAll((List) level.getRecipeManager().getAllRecipesFor(RecipeType.BLASTING));
        for (var holder : level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING)) {
            if (java.util.Arrays.stream(holder.value().getIngredients().getFirst().getItems()).anyMatch(s -> s.is(EXTRA_INPUTS)))
                recipes.add((RecipeHolder) holder);
        }
        return recipes;
    }
}
