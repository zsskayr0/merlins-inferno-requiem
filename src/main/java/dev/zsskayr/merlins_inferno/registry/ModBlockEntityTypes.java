package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.blockentity.DryingRackBlockEntity;
import dev.zsskayr.merlins_inferno.blockentity.GreatDoorBlockEntity;
import dev.zsskayr.merlins_inferno.blockentity.PandoraBoxBlockEntity;
import dev.zsskayr.merlins_inferno.blockentity.HellForgeBlockEntity;
import dev.zsskayr.merlins_inferno.blockentity.MouthBlockEntity;
import dev.zsskayr.merlins_inferno.blockentity.SacredAltarBlockEntity;
import dev.zsskayr.merlins_inferno.blockentity.SacredAltarPartBlockEntity;

/** Central registry for every {@code BlockEntityType} this mod adds. */
public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Merlins_inferno.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HellForgeBlockEntity>> HELL_FORGE = BLOCK_ENTITY_TYPES.register("hell_forge_controller",
            () -> BlockEntityType.Builder.of(HellForgeBlockEntity::new, ModBlocks.HELL_FORGE_CONTROLLER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SacredAltarBlockEntity>> SACRED_ALTAR = BLOCK_ENTITY_TYPES.register("sacred_altar",
            () -> BlockEntityType.Builder.of(SacredAltarBlockEntity::new, ModBlocks.SACRED_ALTAR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SacredAltarPartBlockEntity>> SACRED_ALTAR_PART = BLOCK_ENTITY_TYPES.register("sacred_altar_part",
            () -> BlockEntityType.Builder.of(SacredAltarPartBlockEntity::new, ModBlocks.SACRED_ALTAR_PART.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DryingRackBlockEntity>> DRYING_RACK = BLOCK_ENTITY_TYPES.register("drying_rack",
            () -> BlockEntityType.Builder.of(DryingRackBlockEntity::new, ModBlocks.DRYING_RACK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PandoraBoxBlockEntity>> PANDORA_BOX = BLOCK_ENTITY_TYPES.register("pandora_box",
            () -> BlockEntityType.Builder.of(PandoraBoxBlockEntity::new, ModBlocks.PANDORA_BOX.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GreatDoorBlockEntity>> GREAT_DOOR = BLOCK_ENTITY_TYPES.register("great_door",
            () -> BlockEntityType.Builder.of(GreatDoorBlockEntity::new,
                    ModBlocks.GREAT_DOORS.stream().map(door -> (net.minecraft.world.level.block.Block) door.get()).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MouthBlockEntity>> MOUTH_BLOCK = BLOCK_ENTITY_TYPES.register("mouth_block",
            () -> BlockEntityType.Builder.of(MouthBlockEntity::new, ModBlocks.MOUTH_BLOCK.get()).build(null));

    private ModBlockEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
