package dev.zsskayr.merlins_inferno.network;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import dev.zsskayr.merlins_inferno.forge.ForgeThermalData;
public record ForgeActionPayload(int container, int action) implements CustomPacketPayload {
    public static final Type<ForgeActionPayload> TYPE = new Type<>(ForgeThermalData.id("forge_action"));
    public static final StreamCodec<ByteBuf, ForgeActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ForgeActionPayload::container,
            ByteBufCodecs.VAR_INT, ForgeActionPayload::action, ForgeActionPayload::new);
    @Override public Type<ForgeActionPayload> type() { return TYPE; }
}
