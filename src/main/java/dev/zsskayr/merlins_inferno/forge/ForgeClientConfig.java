package dev.zsskayr.merlins_inferno.forge;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ForgeClientConfig {
    public enum Unit {
        CELSIUS, FAHRENHEIT, KELVIN;
        public String format(double kelvin) {
            return switch (this) {
                case CELSIUS -> String.format(java.util.Locale.ROOT, "%.0f \u00b0C", kelvin - 273.15);
                case FAHRENHEIT -> String.format(java.util.Locale.ROOT, "%.0f \u00b0F", (kelvin - 273.15) * 1.8 + 32);
                case KELVIN -> String.format(java.util.Locale.ROOT, "%.0f K", kelvin);
            };
        }
    }
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.EnumValue<Unit> UNIT = BUILDER.defineEnum("hellForge.temperatureUnit", Unit.CELSIUS);
    public static final ModConfigSpec SPEC = BUILDER.build();
    public static void cycle() {
        UNIT.set(Unit.values()[(UNIT.get().ordinal() + 1) % Unit.values().length]);
        SPEC.save();
    }
    public static String temperature(double kelvin) { return UNIT.get().format(kelvin); }
    public static String cyclingRange(ForgeThermalData.Material material, long milliseconds) {
        Unit unit = Unit.values()[(int) ((milliseconds / 2000) % 3)];
        return unit.format(material.min()) + " – " + unit.format(material.max());
    }
}
