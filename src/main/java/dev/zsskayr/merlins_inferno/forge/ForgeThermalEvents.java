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
        if (!event.getItemStack().is(dev.zsskayr.merlins_inferno.registry.ModTags.Items.WRENCHES)
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
        if (!(state.getBlock() instanceof dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock)
                || !player.mayBuild() || !level.mayInteract(player, pos)) return;
        var held = player.getMainHandItem();
        boolean wrench = held.is(dev.zsskayr.merlins_inferno.registry.ModTags.Items.WRENCHES);
        if (!player.isShiftKeyDown()) {
            // Another mod's wrench cycles the Vault mode like ours does (ours handles itself in useOn).
            if (!wrench || held.getItem() instanceof dev.zsskayr.merlins_inferno.item.ForgeWrenchItem) return;
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide));
            if (level.isClientSide || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
            var next = state.getValue(dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock.MODE).next();
            level.setBlock(pos, state.setValue(dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock.MODE, next), 3);
            player.displayClientMessage(next.displayName(), true);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.2F);
            return;
        }
        // Only an empty hand or a wrench toggles; anything else (blocks, items) keeps its normal use.
        if (!held.isEmpty() && !wrench) return;
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
                state.getValue(dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock.MODE) == dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock.Mode.REDSTONE
                        ? "message.merlins_inferno.hell_forge.vault_readout." + (enabled ? "alarm" : "temperature")
                        : "message.merlins_inferno.hell_forge." + (output ? "vault_push." : "vault_auto.") + (enabled ? "on" : "off")), true);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.2F);
    }
    /** Any block of a Hell Forge opens its GUI; sneaking keeps the block's normal use (placing, vault toggle). */
    @SubscribeEvent public static void openFromAnyBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var level = event.getLevel();
        var pos = event.getPos();
        if (player.isShiftKeyDown() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof dev.zsskayr.merlins_inferno.block.HellForgeControllerBlock) return;
        boolean part = state.is(dev.zsskayr.merlins_inferno.registry.ModBlocks.COMPRESSED_NETHERRACK.get())
                || state.is(dev.zsskayr.merlins_inferno.registry.ModBlocks.HELLGLASS.get())
                || state.is(dev.zsskayr.merlins_inferno.registry.ModBlocks.CRYPT_GLASS.get())
                || state.getBlock() instanceof dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock
                || state.getBlock() instanceof dev.zsskayr.merlins_inferno.block.HellForgeCoreBlock
                || state.is(dev.zsskayr.merlins_inferno.registry.ModBlocks.BLIGHTSTEEL_CASING.get())
                || dev.zsskayr.merlins_inferno.block.HellForgeCoreBlock.catalystOf(state) >= 0;
        if (!part) return;
        var forge = dev.zsskayr.merlins_inferno.blockentity.HellForgeVaultHandler.findForgeAround(level, pos);
        if (forge == null) return;
        // Only a finished forge takes the click: while it is being built its blocks are plain blocks, so the item in
        // hand is placed or used as usual. The client knows it is formed by the synced shell radius.
        if (level.isClientSide) {
            if (forge.renderRadius() <= 0) return;
        } else {
            forge.refreshStructure();
            if (!forge.isFormed()) return;
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide));
        if (!level.isClientSide) player.openMenu(forge);
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
