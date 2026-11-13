package dev.zsskayr.merlins_inferno.client;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModFluids;

/** Entities set alight by Soul Lava burn with blue flames, on screen and in the world, until the fire goes out. */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class SoulFire {
    private static final Set<Entity> SOUL = Collections.newSetFromMap(new WeakHashMap<>());

    private SoulFire() {
    }

    public static boolean burning(Entity entity) {
        return entity != null && entity.isOnFire() && SOUL.contains(entity);
    }

    public static TextureAtlasSprite sprite(int frame) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.withDefaultNamespace("block/soul_fire_" + frame));
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            SOUL.clear();
            return;
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (!entity.isOnFire()) SOUL.remove(entity);
            else if (entity.isInFluidType(ModFluids.SOUL_LAVA_TYPE.get())) SOUL.add(entity);
            // Plain lava sets it back to ordinary flames.
            else if (entity.isInFluidType(net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value())) SOUL.remove(entity);
        }
    }
}
