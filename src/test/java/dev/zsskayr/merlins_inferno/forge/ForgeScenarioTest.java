package dev.zsskayr.merlins_inferno.forge;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

/**
 * Hypothetical Hell Forge runs, computed with the same formulas the block entity uses ({@link ForgeMath}) and the thermal
 * data of the mod. Every core, every size, a few catalyst setups and every fuel the core accepts are run against every
 * material, with each lane loaded with the same stack, and the result is written to build/reports/hellforge-scenarios.md.
 * The simulation follows the forge tick by tick (heating, the shimmer, lane load, fuel), but leaves out the parts that
 * depend on chance (doubling is reported as an expected value) and assumes the fuel never runs out.
 */
class ForgeScenarioTest {
    private static final Path DATA = Path.of("src/main/resources/data/merlins_inferno/hell_forge");
    private static final int ITEMS_PER_LANE = 16;
    private static final int COOKING_TIME = 100; // a typical blast recipe; Hell Forge recipes (200, halved) land on the same
    private static final int[] LANES = {0, 1, 5, 7, 9};
    private static final int[] FUEL_BUCKETS = {0, 1, 3, 6, 10};
    private static final double ROOM = 293.15;

    record Core(String id, int tier, double rest, double min, double max, double efficient, double capacity, double stability,
                List<String> fuels, double speedBonus, double yieldBonus, boolean dross, String shell) {}
    record Shell(double max, double capacity, double stability) {}
    record Fuel(String id, List<String> items, String fluid, double min, double max, double response, double heatCapacity, double flicker, double burnRate) {}
    record Material(String name, double min, double max, double heatCapacity, int minCoreTier) {}
    record Setup(String name, int speed, int yield, int stabilizers, int bellows, int economizers, int wards, boolean dross) {}
    record Size(int radius, int height, boolean roof) {
        int extra() { return Math.max(0, height - (2 * radius + 1)); }
        int lanes() { return radius == 1 ? 1 : Math.min(9, LANES[radius] + extra() / 2); }
        int fuelBuckets() { return FUEL_BUCKETS[radius] + extra() / 2; }
        String label() { return (2 * radius + 1) + "x" + (2 * radius + 1) + "x" + height + (roof ? "" : " open"); }
    }
    record Result(boolean compatible, String reason, double heatSeconds, double totalSeconds, double itemsPerMinute, double fuelPerItem, double yieldFactor) {}

    @Test
    void scenarios() throws IOException {
        Map<String, Shell> shells = new LinkedHashMap<>();
        List<Core> cores = new ArrayList<>();
        List<Fuel> fuels = new ArrayList<>();
        List<Material> materials = new ArrayList<>();
        for (Path file : jsons(DATA)) {
            JsonObject j = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            String name = file.getFileName().toString().replace(".json", "");
            switch (j.get("type").getAsString()) {
                case "shell" -> shells.put(str(j, "block", "merlins_inferno:compressed_netherrack"),
                        new Shell(num(j, "max_temperature", 2173.15), num(j, "heat_capacity", 3), num(j, "stability", 1)));
                case "core" -> {
                    String block = str(j, "block", "merlins_inferno:demonic_core");
                    boolean blighted = block.contains("blighted");
                    cores.add(new Core(block.replace("merlins_inferno:", ""), blighted ? 2 : 1, num(j, "rest_temperature", 473.15),
                            num(j, "min_temperature", 873.15), num(j, "max_temperature", 2173.15), num(j, "efficient_temperature", 1373.15),
                            num(j, "heat_capacity", 240), num(j, "stability", 5), list(j, "allowed_fuels"), num(j, "speed_bonus", 0),
                            num(j, "yield_bonus", 0), "consume".equals(str(j, "dross", "")),
                            blighted ? "merlins_inferno:blightsteel_casing" : "merlins_inferno:compressed_netherrack"));
                }
                case "fuel" -> {
                    List<String> items = list(j, "items");
                    if (j.has("item")) items.add(j.get("item").getAsString());
                    fuels.add(new Fuel(name, items, str(j, "fluid", ""), num(j, "min_temperature", 873.15), num(j, "max_temperature", 1923.15),
                            num(j, "response_ticks", 200), num(j, "heat_capacity", 20), num(j, "flicker", 5), num(j, "burn_rate", 1)));
                }
                case "material" -> materials.add(new Material(name, num(j, "min_temperature", 1811.15), num(j, "max_temperature", 1973.15),
                        num(j, "heat_capacity", 12), (int) num(j, "min_core_tier", 1)));
                case "default_material" -> materials.add(new Material("iron (default)", num(j, "min_temperature", 1811.15),
                        num(j, "max_temperature", 1973.15), num(j, "heat_capacity", 12), 1));
                default -> {
                }
            }
        }
        materials.sort((a, b) -> Double.compare(a.min, b.min));
        cores.sort((a, b) -> Integer.compare(a.tier, b.tier));

        StringBuilder out = new StringBuilder();
        out.append("# Hell Forge scenarios\n\n");
        out.append("Every lane loaded with ").append(ITEMS_PER_LANE).append(" items of the material; a ").append(COOKING_TIME)
                .append("-tick blast recipe. Heat-up = seconds from the core's rest temperature to the first item cooking. ")
                .append("Items/min counts every lane together. Fuel per item is in mB of lava (1 bucket = 1000 mB). ")
                .append("Yield is the expected output per item with doubling.\n\n");
        int rows = 0;
        for (Material material : materials) {
            out.append("## ").append(material.name).append(String.format(Locale.ROOT, " (%.0f-%.0f °C)\n\n", material.min - 273.15, material.max - 273.15));
            out.append("| Core | Size | Lanes | Setup | Fuel | Works | Heat-up (s) | Total (s) | Items/min | Fuel/item (mB) | Yield |\n");
            out.append("|---|---|---|---|---|---|---|---|---|---|---|\n");
            for (Core core : cores) {
                Shell shell = shells.getOrDefault(core.shell, shells.get("merlins_inferno:compressed_netherrack"));
                for (Size size : sizes()) {
                    int radius = size.radius;
                    for (Setup setup : setups(core, radius)) {
                        for (Fuel fuel : fuels) {
                            if (fuel.items.stream().noneMatch(core.fuels::contains) && !core.fuels.contains(fuel.fluid)) continue;
                            Result r = run(core, shell, fuel, material, size, setup);
                            rows++;
                            out.append(String.format(Locale.ROOT, "| %s | %s | %d | %s | %s | %s | %s | %s | %s | %s | %s |\n",
                                    core.id, size.label(), size.lanes(), setup.name, fuel.id,
                                    r.compatible ? "yes" : "no: " + r.reason,
                                    r.compatible ? String.format(Locale.ROOT, "%.1f", r.heatSeconds) : "-",
                                    r.compatible ? String.format(Locale.ROOT, "%.1f", r.totalSeconds) : "-",
                                    r.compatible ? String.format(Locale.ROOT, "%.1f", r.itemsPerMinute) : "-",
                                    r.compatible ? String.format(Locale.ROOT, "%.0f", r.fuelPerItem) : "-",
                                    r.compatible ? String.format(Locale.ROOT, "%.2f", r.yieldFactor) : "-"));
                            assertTrue(!r.compatible || (Double.isFinite(r.totalSeconds) && r.totalSeconds > 0), "finite run for " + material.name);
                        }
                    }
                }
            }
            out.append('\n');
        }
        Path report = Path.of("build/reports/hellforge-scenarios.md");
        Files.createDirectories(report.getParent());
        Files.writeString(report, out.toString(), StandardCharsets.UTF_8);
        assertTrue(rows > 0, "some scenarios ran");
    }

    /** The 3x3x3 cube, then every width as a cube, as an 11-tall tower, and that tower with an open top. */
    private static List<Size> sizes() {
        List<Size> list = new ArrayList<>();
        list.add(new Size(1, 3, true));
        for (int r = 2; r <= 4; r++) {
            list.add(new Size(r, 2 * r + 1, true));
            if (2 * r + 1 < 11) list.add(new Size(r, 11, true));
            list.add(new Size(r, 11, false));
        }
        return list;
    }

    /** The catalyst setups a core and width can hold: at most radius - 1 of each kind (the 3x3 has none). */
    private static List<Setup> setups(Core core, int radius) {
        int cap = Math.max(0, radius - 1);
        List<Setup> list = new ArrayList<>();
        list.add(new Setup("none", 0, 0, 0, 0, 0, 0, false));
        if (core.tier < 2) {
            if (cap > 0) list.add(new Setup(cap + " speed + " + cap + " yield", cap, cap, 0, 0, 0, 0, false));
        } else {
            list.add(new Setup("dross", 0, 0, 0, 0, 0, 0, true));
            if (cap > 0) list.add(new Setup("dross + " + cap + " of each", 0, 0, cap, cap, cap, cap, true));
        }
        return list;
    }

    private static Result run(Core core, Shell shell, Fuel fuel, Material material, Size size, Setup s) {
        int radius = size.radius;
        double maximum = Math.min(Math.min(core.max, shell.max), fuel.max);
        double minimum = Math.max(core.min, fuel.min);
        if (material.minCoreTier > core.tier) return new Result(false, "needs a stronger core", 0, 0, 0, 0, 0);
        if (material.min > maximum) return new Result(false, "too cold", 0, 0, 0, 0, 0);
        if (material.max < minimum) return new Result(false, "too hot", 0, 0, 0, 0, 0);
        int lanes = size.lanes();
        double fuelAmount = size.fuelBuckets() * ForgeMath.BUCKET_FUEL;
        double structure = ForgeMath.structureMass(core.capacity, shell.capacity, radius, size.height, size.roof);
        double mass = structure + ForgeMath.fuelMass(fuel.heatCapacity, fuelAmount);
        double temperature = core.rest;
        // every lane's stack goes in at once and drags the chamber towards room temperature
        // cold stacks barely cool the fire (one tenth of their mass), but each starts at room temperature
        double items = material.heatCapacity * ITEMS_PER_LANE * lanes * 0.1;
        temperature = (temperature * mass + ROOM * items) / (mass + items);
        double itemTemp = ROOM;
        // the player sets the fuel cap so the fire sits in the middle of the material's range
        double mid = (material.min + material.max) / 2;
        double target = ForgeMath.clamp(mid, minimum, maximum);
        double speed = ForgeMath.speed(material.min, core.efficient, maximum, s.speed, core.speedBonus, s.dross);
        int duration = ForgeMath.duration(COOKING_TIME, true, speed);
        double response = ForgeMath.response(fuel.response, mass, s.dross, s.bellows);
        double damping = structure / mass;
        double[] progress = new double[lanes];
        int[] left = new int[lanes];
        java.util.Arrays.fill(left, ITEMS_PER_LANE);
        double fuelUsed = 0;
        int firstCook = -1;
        int done = 0;
        int tick = 0;
        while (done < lanes * ITEMS_PER_LANE && tick < 20 * 60 * 60) {
            temperature = ForgeMath.heat(temperature, target, core.rest, core.efficient, maximum, response);
            fuelUsed += fuel.burnRate;
            double actual = temperature + ForgeMath.flicker(tick, core.stability, shell.stability, fuel.flicker, damping, s.stabilizers) * (size.roof ? 1 : 1.5);
            itemTemp = ForgeMath.itemHeat(itemTemp, actual, material.heatCapacity);
            boolean inRange = itemTemp >= material.min && itemTemp <= material.max;
            int working = 0;
            for (int lane = 0; lane < lanes; lane++) if (left[lane] > 0 && inRange) working++;
            if (working > 0 && firstCook < 0) firstCook = tick;
            double laneSpeed = ForgeMath.laneSpeed(working);
            double laneFuel = ForgeMath.laneFuel(working, s.economizers);
            for (int lane = 0; lane < lanes; lane++) {
                if (left[lane] <= 0 || !inRange) continue;
                progress[lane] += laneSpeed;
                fuelUsed += ForgeMath.processingCost(duration, laneFuel) * fuel.burnRate;
                if (progress[lane] >= duration) {
                    progress[lane] = 0;
                    left[lane]--;
                    done++;
                }
            }
            tick++;
        }
        double total = tick / 20.0;
        int cooked = lanes * ITEMS_PER_LANE;
        double yield = 1 + Math.min(1, ForgeMath.doubleChance(s.yield, core.yieldBonus, s.dross));
        return new Result(true, "", Math.max(0, firstCook) / 20.0, total, cooked / (total / 60.0), fuelUsed / cooked / 20.0, yield);
    }

    private static List<Path> jsons(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static double num(JsonObject j, String key, double fallback) {
        return j.has(key) ? j.get(key).getAsDouble() : fallback;
    }

    private static String str(JsonObject j, String key, String fallback) {
        return j.has(key) ? j.get(key).getAsString() : fallback;
    }

    private static List<String> list(JsonObject j, String key) {
        List<String> out = new ArrayList<>();
        if (j.has(key)) for (JsonElement e : j.getAsJsonArray(key)) out.add(e.getAsString());
        return out;
    }
}
