package dev.zsskayr.merlins_inferno.registry;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.loot.DoubleEssenceModifier;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Merlins_inferno.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<DoubleEssenceModifier>> DOUBLE_ESSENCE =
            SERIALIZERS.register("double_essence", () -> DoubleEssenceModifier.CODEC);

    private ModLootModifiers() {
    }

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
