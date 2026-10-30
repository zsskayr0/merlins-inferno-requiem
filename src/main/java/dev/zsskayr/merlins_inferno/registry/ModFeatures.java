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

    // A taller, wider huge fungus for the Oblivion Forest (stem/hat/decor and height range are data).
    public static final DeferredHolder<Feature<?>, dev.zsskayr.merlins_inferno.worldgen.feature.TallFungusFeature> TALL_FUNGUS =
            FEATURES.register("tall_fungus", () -> new dev.zsskayr.merlins_inferno.worldgen.feature.TallFungusFeature(
                    dev.zsskayr.merlins_inferno.worldgen.feature.TallFungusFeature.Config.CODEC));

    // Tapering spire and half-ellipse arch for the Oblivion's own biomes (ebony spires, bone ribs, ruined gateways).
    public static final DeferredHolder<Feature<?>, dev.zsskayr.merlins_inferno.worldgen.feature.SpireFeature> SPIRE =
            FEATURES.register("spire", () -> new dev.zsskayr.merlins_inferno.worldgen.feature.SpireFeature(
                    dev.zsskayr.merlins_inferno.worldgen.feature.SpireFeature.Config.CODEC));
    public static final DeferredHolder<Feature<?>, dev.zsskayr.merlins_inferno.worldgen.feature.ArchFeature> ARCH =
            FEATURES.register("arch", () -> new dev.zsskayr.merlins_inferno.worldgen.feature.ArchFeature(
                    dev.zsskayr.merlins_inferno.worldgen.feature.ArchFeature.Config.CODEC));

    // Chars the flesh and stone around lava (Carnal Heap's burnt rims).
    public static final DeferredHolder<Feature<?>, dev.zsskayr.merlins_inferno.worldgen.feature.LavaCharFeature> LAVA_CHAR =
            FEATURES.register("lava_char", () -> new dev.zsskayr.merlins_inferno.worldgen.feature.LavaCharFeature(
                    dev.zsskayr.merlins_inferno.worldgen.feature.LavaCharFeature.Config.CODEC));

    // Giant block chains hanging from ceilings across the Oblivion (flesh, netherrack, rarely corrupted obsidian).
    public static final DeferredHolder<Feature<?>, dev.zsskayr.merlins_inferno.worldgen.feature.GiantChainFeature> GIANT_CHAIN =
            FEATURES.register("giant_chain", () -> new dev.zsskayr.merlins_inferno.worldgen.feature.GiantChainFeature(
                    dev.zsskayr.merlins_inferno.worldgen.feature.GiantChainFeature.Config.CODEC));

    // The Haunted Forest's twisting, weeping trees.
    public static final DeferredHolder<Feature<?>, dev.zsskayr.merlins_inferno.worldgen.feature.HauntedTreeFeature> HAUNTED_TREE =
            FEATURES.register("haunted_tree", () -> new dev.zsskayr.merlins_inferno.worldgen.feature.HauntedTreeFeature(
                    dev.zsskayr.merlins_inferno.worldgen.feature.HauntedTreeFeature.Config.CODEC));

    // Fills El'ysium's basins up to the water line (see the feature's own comment).
    public static final DeferredHolder<Feature<?>, dev.zsskayr.merlins_inferno.worldgen.feature.ElysiumWaterFeature> ELYSIUM_WATER =
            FEATURES.register("elysium_water", dev.zsskayr.merlins_inferno.worldgen.feature.ElysiumWaterFeature::new);

    // The bridges and giant chains that tie El'ysium's islands together.
    public static final DeferredHolder<Feature<?>, dev.zsskayr.merlins_inferno.worldgen.feature.ElysiumSpansFeature> ELYSIUM_SPANS =
            FEATURES.register("elysium_spans", dev.zsskayr.merlins_inferno.worldgen.feature.ElysiumSpansFeature::new);

    private ModFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
