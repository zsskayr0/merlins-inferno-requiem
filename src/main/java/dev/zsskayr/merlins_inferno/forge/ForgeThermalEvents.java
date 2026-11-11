package dev.zsskayr.merlins_inferno.forge;

import dev.zsskayr.merlins_inferno.network.ForgeThermalPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "merlins_inferno")
public final class ForgeThermalEvents {
    @SubscribeEvent public static void wrench(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof dev.zsskayr.merlins_inferno.item.ForgeWrenchItem)
                || !(event.getLevel().getBlockState(event.getPos()).getBlock()
                instanceof dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock)) return;
        event.setCanceled(true);
        if (!event.getLevel().isClientSide && event.getAction()
                == net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START) {
            var forge = dev.zsskayr.merlins_inferno.blockentity.HellForgeVaultHandler.findForge(event.getLevel(), event.getPos());
            if (forge != null) event.getEntity().openMenu(forge);
        }
    }
    @SubscribeEvent public static void toggleVaultAuto(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var level = event.getLevel();
        var pos = event.getPos();
        var state = level.getBlockState(pos);
        if (!player.isShiftKeyDown() || !(state.getBlock() instanceof dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock)
                || !player.mayBuild() || !level.mayInteract(player, pos)) return;
        // Only an empty hand or the Wrench toggles; anything else (blocks, items) keeps its normal use.
        var held = player.getMainHandItem();
        if (!held.isEmpty() && !(held.getItem() instanceof dev.zsskayr.merlins_inferno.item.ForgeWrenchItem)) return;
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide));
        // Cancel both hands, but mutate only once for the main-hand interaction.
        if (level.isClientSide || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        var property = dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock.AUTO;
        boolean enabled = !state.getValue(property);
        level.setBlock(pos, state.setValue(property, enabled), 3);
        if (enabled) level.scheduleTick(pos, state.getBlock(), dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock.AUTO_INTERVAL);
        boolean output = state.getValue(dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock.MODE)
                == dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock.Mode.OUTPUT;
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.merlins_inferno.hell_forge." + (output ? "vault_push." : "vault_auto.") + (enabled ? "on" : "off")), true);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.2F);
    }
    @SubscribeEvent public static void reload(AddReloadListenerEvent event) {
        event.addListener(new ForgeThermalData());
    }
    @SubscribeEvent public static void sync(OnDatapackSyncEvent event) {
        var payload = new ForgeThermalPayload(ForgeThermalData.encode());
        if (event.getPlayer() != null) PacketDistributor.sendToPlayer(event.getPlayer(), payload);
        else event.getPlayerList().getPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }
}
