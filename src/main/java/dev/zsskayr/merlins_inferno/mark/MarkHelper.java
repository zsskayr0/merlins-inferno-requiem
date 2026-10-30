package dev.zsskayr.merlins_inferno.mark;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

import dev.zsskayr.merlins_inferno.compat.curios.CuriosMarks;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;

/**
 * The one place that knows which {@link dev.zsskayr.merlins_inferno.item.MarkItem Mark} a player bears.
 * <p>
 * The truth is the {@code EQUIPPED_MARK} attachment (saved, kept on death), never the Curios slot: without Curios the
 * Mark is simply taken in by right-click and the player is not told, they only feel its effects; with Curios the same
 * state is kept in step by {@link CuriosMarks}. A player bears at most one Mark.
 */
public final class MarkHelper {
    /** Chance that picking a Mark up off the ground binds it to a player who bears none (see MarkPickupHandler). */
    public static final float AUTO_EQUIP_CHANCE = 0.4F;

    private MarkHelper() {
    }

    /** The Mark's item, or {@code null} if the player bears none. */
    @Nullable
    public static Item getMark(Player player) {
        String id = player.getData(ModAttachments.EQUIPPED_MARK);
        if (id.isEmpty()) {
            return null;
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? null : BuiltInRegistries.ITEM.getOptional(key).orElse(null);
    }

    public static boolean hasMark(LivingEntity entity) {
        return entity instanceof Player player && getMark(player) != null;
    }

    public static boolean hasMark(LivingEntity entity, Item mark) {
        return entity instanceof Player player && getMark(player) == mark;
    }

    /** Binds {@code mark} to the player. Returns {@code false}, changing nothing, if they already bear one. */
    public static boolean equip(Player player, Item mark) {
        if (hasMark(player)) {
            return false;
        }
        player.setData(ModAttachments.EQUIPPED_MARK, BuiltInRegistries.ITEM.getKey(mark).toString());
        if (!player.level().isClientSide) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.4F, 0.5F);
        }
        return true;
    }

    /**
     * Frees the player of whatever Mark they bear: the state and, with Curios, the item in the slot. This is what a
     * Scroll of Salvation does - the Mark itself is gone, it does not come back as an item.
     */
    public static void clear(Player player) {
        player.setData(ModAttachments.EQUIPPED_MARK, "");
        if (ModList.get().isLoaded("curios")) {
            CuriosMarks.removeEquipped(player);
        }
    }
}
