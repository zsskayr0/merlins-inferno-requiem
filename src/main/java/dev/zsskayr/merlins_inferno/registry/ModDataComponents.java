package dev.zsskayr.merlins_inferno.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/** Item data components this mod adds. */
public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Merlins_inferno.MODID);

    /** What a Dark Coven has killed while invoked, for the stage it is at. A sword that has moved up a stage starts over (see item.DarkCovenItem). */
    public record CovenTraining(int stage, int kills) {
        public static final Codec<CovenTraining> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("stage").forGetter(CovenTraining::stage),
                Codec.INT.fieldOf("kills").forGetter(CovenTraining::kills)).apply(i, CovenTraining::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, CovenTraining> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, CovenTraining::stage, ByteBufCodecs.VAR_INT, CovenTraining::kills, CovenTraining::new);
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CovenTraining>> COVEN_TRAINING = DATA_COMPONENTS.register("coven_training",
            () -> DataComponentType.<CovenTraining>builder().persistent(CovenTraining.CODEC).networkSynchronized(CovenTraining.STREAM_CODEC).build());

    /**
     * The stage a Dark Coven has finished training for: set once its kills reach what that stage asks, and what the
     * smithing recipes look for in the base sword (a stage-III sword with 2 here is ready to become stage IV...).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COVEN_READY = DATA_COMPONENTS.register("coven_ready",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    private ModDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
