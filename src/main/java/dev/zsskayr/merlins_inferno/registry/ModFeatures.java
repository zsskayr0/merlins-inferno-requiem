package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.worldgen.feature.StructurePasteConfiguration;
import dev.zsskayr.merlins_inferno.worldgen.feature.StructurePasteFeature;

/**
 * Custom {@code Feature} types this mod adds (code-registered, unlike {@code ConfiguredFeature}s
 * which are datapack entries built by the datagen providers).
 */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Merlins_inferno.MODID);

    // Pastes a randomly-picked pre-made NBT structure (see StructurePasteConfiguration) instead of
    // growing a procedural tree - used for the Old Growth Pine Taiga fir trees ported from a
    // friend's structure-based tree pack, which ship as ready-made NBT shapes rather than a
    // trunk/foliage placer setup.
    public static final DeferredHolder<Feature<?>, StructurePasteFeature> STRUCTURE_PASTE = FEATURES.register("structure_paste",
            () -> new StructurePasteFeature(StructurePasteConfiguration.CODEC));

    private ModFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
