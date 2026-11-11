package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.loot.LootTableMatchesCondition;

public final class ModLootConditions {
    public static final DeferredRegister<LootItemConditionType> CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, Merlins_inferno.MODID);

    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> TABLE_MATCHES =
            CONDITIONS.register("loot_table_matches", () -> new LootItemConditionType(LootTableMatchesCondition.CODEC));

    private ModLootConditions() {
    }

    public static void register(IEventBus bus) {
        CONDITIONS.register(bus);
    }
}
