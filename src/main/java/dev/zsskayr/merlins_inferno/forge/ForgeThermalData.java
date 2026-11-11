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
                           double decay, double critical, int criticalTicks, double drossChance) {}
    public record Core(double rest, double min, double max, double efficient,
                       double capacity, double stability, double cooling, String operation, List<String> allowedFuels) {
        public boolean accepts(Fuel fuel) { return fuel != null && allowedFuels.contains(fuel.item()); }
    }
    public record Fuel(String id, String item, int amount, double min, double max, double response,
                        double heatCapacity, double flicker) {}
    public record Shell(double max, double capacity, double stability) {}
    private record MaterialRule(JsonObject source, Material material) {}

    public static final double KELVIN_OFFSET = 273.15;
    public static final TagKey<net.minecraft.world.item.Item> EXTRA_INPUTS =
            TagKey.create(Registries.ITEM, id("hell_forge_inputs"));
    private static final Gson GSON = new Gson();
    private static JsonObject serverRules = new JsonObject();
    private static Rules server = parse(new JsonObject());
    private static Rules client = server;
    private record Rules(Material fallback, Map<String, Core> cores, Shell shell, List<Fuel> fuels, List<MaterialRule> materials) {}

    public ForgeThermalData() { super(GSON, "hell_forge"); }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("merlins_inferno", path); }
    private static double number(JsonObject j, String name, double fallback) {
        double value = j.has(name) ? j.get(name).getAsDouble() : fallback;
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid " + name);
        return value;
    }
    private static Material material(JsonObject j) {
        double min = number(j, "min_temperature", 1811.15);
        double max = number(j, "max_temperature", 1973.15);
        double critical = number(j, "critical_temperature", max + 150);
        double chance = number(j, "dross_chance", 0.10);
        if (max < min || critical <= max || chance > 1) throw new IllegalArgumentException("Invalid thermal range");
        return new Material(min, max, number(j, "heat_capacity", 12), (int) number(j, "overheat_grace_ticks", 100),
                number(j, "overheat_progress_decay", 0.5), critical,
                Math.max(1, (int) number(j, "critical_exposure_ticks", 400)), chance);
    }
    private static Rules parse(JsonObject documents) {
        Material fallback = material(new JsonObject());
        Core defaultCore = new Core(473.15, 873.15, 2173.15, 1373.15, 240, 5, 0.002,
                "heating", List.of("minecraft:coal", "minecraft:lava_bucket"));
        Map<String, Core> cores = new java.util.HashMap<>();
        cores.put("merlins_inferno:demonic_core", defaultCore);
        Shell shell = new Shell(2173.15, 3, 1);
        List<Fuel> fuels = new ArrayList<>();
        List<MaterialRule> materials = new ArrayList<>();
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
                            j.has("operation") ? j.get("operation").getAsString() : "heating", List.copyOf(allowedFuels));
                    if (core.min > core.max || core.rest > core.min || core.efficient > core.max
                            || core.efficient < core.min || core.cooling > 1 || !core.operation.equals("heating"))
                        throw new IllegalArgumentException("Invalid or unsupported Core thermal profile");
                    cores.put(j.has("block") ? j.get("block").getAsString() : "merlins_inferno:demonic_core", core);
                }
                case "shell" -> shell = new Shell(number(j, "max_temperature", shell.max),
                        number(j, "heat_capacity", shell.capacity), number(j, "stability", shell.stability));
                case "fuel" -> {
                    int amount = (int) number(j, "fuel_value", 1600);
                    double min = number(j, "min_temperature", 873.15);
                    double max = number(j, "max_temperature", 1923.15);
                    double response = number(j, "response_ticks", 200);
                    if (amount < 1 || amount > 20000 || max < min || response < 1)
                        throw new IllegalArgumentException("Invalid fuel " + entry.getKey());
                    fuels.add(new Fuel(entry.getKey(), j.get("item").getAsString(), amount, min, max, response,
                            Math.max(0, number(j, "heat_capacity", 20)), Math.max(0, number(j, "flicker", 5))));
                }
                default -> throw new IllegalArgumentException("Unknown Hell Forge rule " + entry.getKey());
            }
        }
        materials.sort(Comparator.comparingDouble((MaterialRule r) -> number(r.source, "priority", 0)).reversed());
        return new Rules(fallback, Map.copyOf(cores), shell, List.copyOf(fuels), List.copyOf(materials));
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
    public static Shell shell(Level level) { return rules(level).shell; }
    public static List<Fuel> fuels(Level level) { return rules(level).fuels; }
    public static Fuel fuel(Level level, ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return fuels(level).stream().filter(f -> f.item.equals(id)).findFirst().orElse(null);
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
