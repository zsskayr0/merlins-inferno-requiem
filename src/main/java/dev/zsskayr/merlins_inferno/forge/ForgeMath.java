package dev.zsskayr.merlins_inferno.forge;

/**
 * The Hell Forge's numbers, free of any game object so the block entity and the scenario tests share one formula.
 * Temperatures are Kelvin.
 */
public final class ForgeMath {
    /** The thermal mass that a forge's response time is quoted against. */
    public static final double REFERENCE_MASS = 318.0;
    /** One bucket of lava's worth of fuel energy. */
    public static final double BUCKET_FUEL = 20000;

    private ForgeMath() {
    }

    /** How fast a lane cooks (higher is faster): hard materials near the ceiling are slower, catalysts and the core speed it up. */
    public static double speed(double materialMin, double efficient, double maximum, int speedCatalysts, double coreSpeedBonus, boolean drossActive) {
        double difficulty = clamp((materialMin - efficient) / Math.max(1, maximum - efficient), 0, 1);
        return (1.50 - 0.48 * difficulty) * (1 + speedCatalysts * 0.15) * (1 + coreSpeedBonus) * (drossActive ? 1.25 : 1);
    }

    /** Ticks for one item: a blasting recipe's time as is, any other cooking recipe's time halved, divided by the speed. */
    public static int duration(int cookingTime, boolean blasting, double speed) {
        int blastTime = blasting ? cookingTime : Math.max(1, cookingTime / 2);
        return Math.max(1, (int) Math.floor(blastTime / speed));
    }

    /** Every lane working past the first slows the others down. */
    public static double laneSpeed(int working) {
        return 1.0 / (1.0 + 0.08 * Math.max(0, working - 1));
    }

    /** Every lane working past the first burns extra fuel; Economizers trim that extra. */
    public static double laneFuel(int working, int economizers) {
        return 1.0 + 0.12 * Math.max(0, working - 1) * Math.max(0.2, 1 - 0.2 * economizers);
    }

    /** Fuel burnt per tick by a lane that is cooking. */
    public static double processingCost(int duration, double laneFuel) {
        return 200.0 / duration * laneFuel;
    }

    public static int shellBlocks(int radius) {
        int size = radius * 2 + 1;
        return size * size * size - (size - 2) * (size - 2) * (size - 2);
    }

    /** Shell blocks of a square forge of this half width and height; an open top leaves its inside out. */
    public static int shellBlocks(int radius, int height, boolean roof) {
        int side = 2 * radius + 1, inner = side - 2;
        int blocks = side * side * height - inner * inner * Math.max(0, height - 2);
        return roof ? blocks : blocks - inner * inner;
    }

    public static double structureMass(double coreCapacity, double shellCapacity, int radius) {
        return coreCapacity + shellCapacity * shellBlocks(radius);
    }

    public static double structureMass(double coreCapacity, double shellCapacity, int radius, int height, boolean roof) {
        return coreCapacity + shellCapacity * shellBlocks(radius, height, roof);
    }

    /** The stored fuel adds thermal mass, capped so a full tank cannot freeze the forge. */
    public static double fuelMass(double fuelHeatCapacity, double fuelAmount) {
        return Math.min(fuelHeatCapacity * fuelAmount / BUCKET_FUEL, REFERENCE_MASS);
    }

    /** The forge's response time in ticks; dross and Bellows make it react faster. */
    public static double response(double fuelResponse, double mass, boolean drossActive, int bellows) {
        return fuelResponse * mass / REFERENCE_MASS * (drossActive ? 0.75 : 1) / (1 + 0.2 * bellows);
    }

    /**
     * One tick of heating towards {@code target}: aim past it so the climb stays brisk, then hold on it once reached.
     * Returns the new temperature.
     */
    public static double heat(double temperature, double target, double rest, double efficient, double maximum, double response) {
        double hot = clamp((target - efficient) / Math.max(1, maximum - efficient), 0, 1);
        double rate = (1 - Math.pow(0.1, 1 / response)) / (1 + hot * 1.2);
        double drive = Math.min(maximum, target + 0.35 * Math.max(0, target - rest));
        double next = temperature + (drive - temperature) * rate;
        return temperature <= target && next > target ? target : next;
    }

    /** The shimmer around the held temperature; Stabilizers damp it. */
    public static double flicker(double time, double coreStability, double shellStability, double fuelFlicker, double damping, int stabilizers) {
        double wave = Math.sin(time * 0.07) * coreStability * shellStability
                + (Math.sin(time * 0.31) + Math.sin(time * 0.113 + 1.7)) * 0.5 * fuelFlicker * damping;
        return wave * Math.max(0.2, 1 - 0.2 * stabilizers);
    }

    /** One tick of a stack warming (or cooling) towards the chamber; heavy materials take longer. */
    public static double itemHeat(double item, double chamber, double heatCapacity) {
        double tau = 40 + 25 * heatCapacity;
        return item + (chamber - item) * (1 - Math.exp(-1 / tau));
    }

    /** The chance an item comes out doubled. */
    public static double doubleChance(int yieldCatalysts, double coreYieldBonus, boolean drossActive) {
        return yieldCatalysts * 0.10 + coreYieldBonus + (drossActive ? 0.15 : 0);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
