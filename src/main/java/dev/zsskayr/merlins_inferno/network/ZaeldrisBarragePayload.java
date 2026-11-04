package dev.zsskayr.merlins_inferno.network;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: Zaeldris' barrage key went down ({@code held}) or up. The server keeps the firing rhythm itself. */
public record ZaeldrisBarragePayload(boolean held) implements CustomPacketPayload {
    public static final Type<ZaeldrisBarragePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "zaeldris_barrage"));
    public static final StreamCodec<ByteBuf, ZaeldrisBarragePayload> CODEC = ByteBufCodecs.BOOL.map(ZaeldrisBarragePayload::new, ZaeldrisBarragePayload::held);
    @Override public Type<ZaeldrisBarragePayload> type() { return TYPE; }
}
