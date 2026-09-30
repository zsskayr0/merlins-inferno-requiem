package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.blockentity.DryingRackBlockEntity;
import dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity;
import dev.zsskayr.merlins_inferno.blockentity.HellForgePartBlockEntity;
import dev.zsskayr.merlins_inferno.blockentity.SacredAltarBlockEntity;

/** Central registry for every {@code BlockEntityType} this mod adds. */
public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Merlins_inferno.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HellForgeBlockEntity>> HELL_FORGE = BLOCK_ENTITY_TYPES.register("hell_forge",
            () -> BlockEntityType.Builder.of(HellForgeBlockEntity::new, ModBlocks.HELL_FORGE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HellForgePartBlockEntity>> HELL_FORGE_PART = BLOCK_ENTITY_TYPES.register("hell_forge_part",
            () -> BlockEntityType.Builder.of(HellForgePartBlockEntity::new, ModBlocks.HELL_FORGE_PART.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SacredAltarBlockEntity>> SACRED_ALTAR = BLOCK_ENTITY_TYPES.register("sacred_altar",
            () -> BlockEntityType.Builder.of(SacredAltarBlockEntity::new, ModBlocks.SACRED_ALTAR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DryingRackBlockEntity>> DRYING_RACK = BLOCK_ENTITY_TYPES.register("drying_rack",
            () -> BlockEntityType.Builder.of(DryingRackBlockEntity::new, ModBlocks.DRYING_RACK.get()).build(null));

    private ModBlockEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
