package dev.zsskayr.merlins_inferno.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.blockentity.HellForgeVaultFluidHandler;
import dev.zsskayr.merlins_inferno.blockentity.HellForgeVaultHandler;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/** Hooks the Hell Forge Vault up to the item handler capability, so hoppers and pipes can use it. */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class HellForgeCapabilities {
    private HellForgeCapabilities() {
    }

    @SubscribeEvent
    static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> HellForgeVaultHandler.of(level, pos),
                ModBlocks.HELL_FORGE_VAULT.get());
        event.registerBlock(Capabilities.FluidHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> HellForgeVaultFluidHandler.of(level, pos),
                ModBlocks.HELL_FORGE_VAULT.get());
    }
}
