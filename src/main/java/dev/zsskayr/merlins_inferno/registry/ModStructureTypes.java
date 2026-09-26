package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.worldgen.structure.DruidSanctuaryStructure;
import dev.zsskayr.merlins_inferno.worldgen.structure.RowanwoodTreeStructure;

/**
 * Custom {@code StructureType}s this mod adds (code-registered, unlike {@code Structure}s
 * themselves which are datapack entries built by {@code ModStructureProvider}).
 * <p>
 * This exists so Rowanwood is a real vanilla {@code Structure} (with its own {@code StructureSet}
 * for spacing) instead of a decoration {@code Feature} - the only way to make it findable with
 * {@code /locate}, which only knows about the structure registry.
 */
public final class ModStructureTypes {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Merlins_inferno.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<RowanwoodTreeStructure>> ROWANWOOD_TREE =
            STRUCTURE_TYPES.register("rowanwood_tree", () -> () -> RowanwoodTreeStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<DruidSanctuaryStructure>> DRUID_SANCTUARY =
            STRUCTURE_TYPES.register("druid_sanctuary", () -> () -> DruidSanctuaryStructure.CODEC);

    private ModStructureTypes() {
    }

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
    }
}
