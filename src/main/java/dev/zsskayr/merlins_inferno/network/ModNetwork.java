package dev.zsskayr.merlins_inferno.network;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.item.WingsItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Every network payload this mod sends. */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class ModNetwork {
    private static final String PROTOCOL = "1";
    private ModNetwork() {}
    @SubscribeEvent static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL).playToServer(WingsBoostPayload.TYPE, WingsBoostPayload.CODEC,
                (payload, context) -> WingsItem.tryBoost(context.player()));
        event.registrar(PROTOCOL).playToClient(CirclePayload.TYPE, CirclePayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> ProgressionHelper.setClientCircle(payload.circle())));
    }
}
