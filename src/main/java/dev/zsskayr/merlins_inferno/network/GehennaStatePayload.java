package dev.zsskayr.merlins_inferno.network;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: Gehenna's state for the HUD clock - blows left of the fed boon, and ticks left of the frenzy. */
public record GehennaStatePayload(int fed, int frenzyTicks) implements CustomPacketPayload {
    public static final Type<GehennaStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "gehenna_state"));
    public static final StreamCodec<ByteBuf, GehennaStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GehennaStatePayload::fed, ByteBufCodecs.VAR_INT, GehennaStatePayload::frenzyTicks, GehennaStatePayload::new);
    @Override public Type<GehennaStatePayload> type() { return TYPE; }
}
