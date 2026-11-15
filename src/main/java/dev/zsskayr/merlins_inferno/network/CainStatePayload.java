package dev.zsskayr.merlins_inferno.network;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client, every second while the Mark of Cain is borne: ticks left to the next obligatory kill (negative once
 * overdue) and whether the Mark of the Murder is armed, and ticks until the Mark can refuse death again (0 ready, -1 blocked by an overdue kill). {@code bearing} false hides the clock.
 */
public record CainStatePayload(boolean bearing, int ticksLeft, boolean armed, int reviveLeft) implements CustomPacketPayload {
    public static final Type<CainStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "cain_state"));
    public static final StreamCodec<ByteBuf, CainStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, CainStatePayload::bearing, ByteBufCodecs.INT, CainStatePayload::ticksLeft,
            ByteBufCodecs.BOOL, CainStatePayload::armed, ByteBufCodecs.VAR_INT, CainStatePayload::reviveLeft, CainStatePayload::new);
    @Override public Type<CainStatePayload> type() { return TYPE; }
}
