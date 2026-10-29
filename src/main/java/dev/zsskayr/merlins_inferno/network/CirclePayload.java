package dev.zsskayr.merlins_inferno.network;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: the world's Circle, sent on login and whenever it changes, for the client-side readers (mining, item use). */
public record CirclePayload(int circle) implements CustomPacketPayload {
    public static final Type<CirclePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "circle"));
    public static final StreamCodec<ByteBuf, CirclePayload> CODEC = ByteBufCodecs.VAR_INT.map(CirclePayload::new, CirclePayload::circle);
    @Override public Type<CirclePayload> type() { return TYPE; }
}
