package dev.zsskayr.merlins_inferno.network;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: player {@code entityId} started or stopped Zaeldris' barrage, so others draw them holding it like a trident. */
public record ZaeldrisPosePayload(int entityId, boolean barraging) implements CustomPacketPayload {
    public static final Type<ZaeldrisPosePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "zaeldris_pose"));
    public static final StreamCodec<ByteBuf, ZaeldrisPosePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ZaeldrisPosePayload::entityId, ByteBufCodecs.BOOL, ZaeldrisPosePayload::barraging, ZaeldrisPosePayload::new);
    @Override public Type<ZaeldrisPosePayload> type() { return TYPE; }
}
