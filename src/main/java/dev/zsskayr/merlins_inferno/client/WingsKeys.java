package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.WingsItem;
import dev.zsskayr.merlins_inferno.network.WingsBoostPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** The wings' boost key: asks the server for a boost only when the wings are worn, gliding and off cooldown. */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class WingsKeys {
    public static final KeyMapping BOOST = new KeyMapping("key.merlins_inferno.wings_boost", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.merlins_inferno");
    private WingsKeys() {}

    @SubscribeEvent static void register(RegisterKeyMappingsEvent event) { event.register(BOOST); }

    @SubscribeEvent static void tick(ClientTickEvent.Post event) {
        LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        // Always drained, so presses made outside a world or while not gliding never pile up.
        while (BOOST.consumeClick()) {
            if (player == null || !player.isFallFlying()) continue;
            ItemStack wings = WingsItem.find(player);
            if (!wings.isEmpty() && !player.getCooldowns().isOnCooldown(wings.getItem())) {
                PacketDistributor.sendToServer(new WingsBoostPayload());
            }
        }
    }
}
