package dev.zsskayr.merlins_inferno.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dev.zsskayr.merlins_inferno.forge.ForgeThermalData;

public record ForgeThermalPayload(String json) implements CustomPacketPayload {
    public static final Type<ForgeThermalPayload> TYPE = new Type<>(ForgeThermalData.id("forge_thermal"));
    public static final StreamCodec<ByteBuf, ForgeThermalPayload> CODEC =
            ByteBufCodecs.stringUtf8(1048576).map(ForgeThermalPayload::new, ForgeThermalPayload::json);
    @Override public Type<ForgeThermalPayload> type() { return TYPE; }
}
