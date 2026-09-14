package dev.zsskayr.merlins_inferno.compat.patchouli;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import vazkii.patchouli.api.PatchouliAPI;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * Every reference to a Patchouli class lives here, isolated from {@code item.GrimmoriumItem} -
 * that class only ever calls {@link #openGrimoire}, and only from behind a
 * {@code ModList.get().isLoaded("patchouli")} check. Java resolves a class's own symbolic
 * references lazily (only when a method that actually touches them runs), so as long as nothing
 * outside this package imports {@code vazkii.patchouli.*} directly, none of this ever gets loaded
 * - and never throws {@code NoClassDefFoundError} - when Patchouli isn't installed.
 */
public final class PatchouliCompat {
    private static final ResourceLocation BOOK_ID = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "guide");

    private PatchouliCompat() {
    }

    public static void openGrimoire(ServerPlayer player) {
        PatchouliAPI.get().openBookGUI(player, BOOK_ID);
    }
}
