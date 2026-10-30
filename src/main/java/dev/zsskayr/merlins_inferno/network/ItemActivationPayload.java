package dev.zsskayr.merlins_inferno.network;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Server to client: play the Totem of Undying's pop-up animation, with this item in the middle (a Scroll read, say). */
public record ItemActivationPayload(ItemStack stack) implements CustomPacketPayload {
    public static final Type<ItemActivationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "item_activation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemActivationPayload> CODEC =
            ItemStack.STREAM_CODEC.map(ItemActivationPayload::new, ItemActivationPayload::stack);
    @Override public Type<ItemActivationPayload> type() { return TYPE; }
}
