package dev.zsskayr.merlins_inferno.client.elysium;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.block.CloudSeaBlock;
import dev.zsskayr.merlins_inferno.client.IrisFogCompat;

/**
 * Client hooks for El'ysium: registers {@link ElysiumSkyEffects}, and tells {@link CloudSeaBlock} whether to glow, which it
 * only does without a shader pack (a pack would tint the glow like torchlight, and shades the clouds well on its own).
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class ElysiumClientEvents {
    private ElysiumClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ElysiumSkyEffects.ID, new ElysiumSkyEffects());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level != null && mc.level.effects() instanceof ElysiumSkyEffects)) {
            return;
        }
        boolean bright = !IrisFogCompat.irisReady();
        if (bright != CloudSeaBlock.isBright()) {
            CloudSeaBlock.setBright(bright);
            mc.levelRenderer.allChanged(); // re-mesh: emissive rendering is baked into the chunk meshes
        }
    }
}
