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
        event.registrar(PROTOCOL).playToServer(ForgeActionPayload.TYPE, ForgeActionPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var player = context.player();
                    if (player.containerMenu instanceof dev.zsskayr.merlins_inferno.menu.HellForgeMenu menu
                            && menu.containerId == payload.container() && menu.stillValid(player))
                        menu.clickMenuButton(player, payload.action());
                }));
        event.registrar(PROTOCOL).playToClient(ForgeThermalPayload.TYPE, ForgeThermalPayload.CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        dev.zsskayr.merlins_inferno.forge.ForgeThermalData.receive(payload.json())));
        event.registrar(PROTOCOL).playToServer(WingsBoostPayload.TYPE, WingsBoostPayload.CODEC,
                (payload, context) -> WingsItem.tryBoost(context.player()));
        event.registrar(PROTOCOL).playToServer(ZaeldrisBarragePayload.TYPE, ZaeldrisBarragePayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        dev.zsskayr.merlins_inferno.item.ZaeldrisItem.setBarrage(player, payload.held());
                    }
                }));
        event.registrar(PROTOCOL).playToClient(ZaeldrisPosePayload.TYPE, ZaeldrisPosePayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> dev.zsskayr.merlins_inferno.client.ZaeldrisPose.set(payload.entityId(), payload.barraging())));
        event.registrar(PROTOCOL).playToClient(CainStatePayload.TYPE, CainStatePayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> dev.zsskayr.merlins_inferno.client.CainHud.set(payload.bearing(), payload.ticksLeft(), payload.armed())));
        event.registrar(PROTOCOL).playToClient(GehennaStatePayload.TYPE, GehennaStatePayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> dev.zsskayr.merlins_inferno.client.GehennaHud.set(payload.fed(), payload.frenzyTicks())));
        event.registrar(PROTOCOL).playToClient(CirclePayload.TYPE, CirclePayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> ProgressionHelper.setClientCircle(payload.circle())));
        event.registrar(PROTOCOL).playToClient(ItemActivationPayload.TYPE, ItemActivationPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> dev.zsskayr.merlins_inferno.client.ItemActivationClient.show(payload.stack())));
    }
}
