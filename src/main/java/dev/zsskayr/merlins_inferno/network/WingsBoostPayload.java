package dev.zsskayr.merlins_inferno.network;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: the wings' boost key was pressed. Carries no data; the server decides from the player's own state. */
public record WingsBoostPayload() implements CustomPacketPayload {
    public static final Type<WingsBoostPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "wings_boost"));
    public static final StreamCodec<ByteBuf, WingsBoostPayload> CODEC = StreamCodec.unit(new WingsBoostPayload());
    @Override public Type<WingsBoostPayload> type() { return TYPE; }
}
