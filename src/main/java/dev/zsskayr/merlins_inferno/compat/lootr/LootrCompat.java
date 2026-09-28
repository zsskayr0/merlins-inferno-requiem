package dev.zsskayr.merlins_inferno.compat.lootr;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * Lootr (per-player loot instances) support without a dependency on it: Lootr's chest is an ordinary
 * {@code ChestBlock} / {@code ChestBlockEntity} subclass registered as {@code lootr:lootr_chest}, so it is looked up
 * by id at runtime and everything else (facing, loot table) goes through vanilla types. Nothing here imports a
 * Lootr class, so the mod loads and runs the same whether or not Lootr is installed.
 * <p>
 * Chests generated with a template (the Ancient Battlefield's) need nothing: Lootr converts any chest that
 * carries a loot table on its own. Only the chests this mod places at runtime go through here, and only when Lootr is there.
 */
public final class LootrCompat {
    private static final ResourceLocation LOOTR_CHEST = ResourceLocation.fromNamespaceAndPath("lootr", "lootr_chest");

    private LootrCompat() {
    }

    /** Lootr's chest, or empty if Lootr is not installed - then boss loot simply drops, as it always did. */
    public static Optional<Block> rewardChest() {
        return BuiltInRegistries.BLOCK.getOptional(LOOTR_CHEST);
    }
}
