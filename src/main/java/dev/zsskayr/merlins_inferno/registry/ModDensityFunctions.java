package dev.zsskayr.merlins_inferno.registry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumField;

/** Density function types of this mod (El'ysium's islands and climate). */
public final class ModDensityFunctions {
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Merlins_inferno.MODID);

    static {
        DENSITY_FUNCTION_TYPES.register("elysium_field", () -> ElysiumField.MAP_CODEC);
    }

    private ModDensityFunctions() {
    }

    public static void register(IEventBus bus) {
        DENSITY_FUNCTION_TYPES.register(bus);
    }
}
