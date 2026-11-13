package dev.zsskayr.merlins_inferno.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;

import dev.zsskayr.merlins_inferno.compat.OptionalCurios;
import dev.zsskayr.merlins_inferno.registry.ModFluids;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/** Salamander Bracelet: lava and Soul Lava swim like water (pose, sprint-swim, diving). No fire protection. */
public final class SalamanderBracelet {
    /** Lava swim speed with a full Ebony set. */
    public static final float EBONY_SPEED = 1.6F;

    private SalamanderBracelet() {
    }

    /** With Curios the bracelet must be in a Curios slot; without it, anywhere in the inventory counts. */
    public static boolean wearing(Player player) {
        if (!ModList.get().isLoaded("curios")) return player.getInventory().contains(ModItems.SALAMANDER_BRACELET.get().getDefaultInstance());
        for (var stack : OptionalCurios.equipped(player)) if (stack.is(ModItems.SALAMANDER_BRACELET.get())) return true;
        return false;
    }

    public static boolean lava(FluidType type) {
        return type == NeoForgeMod.LAVA_TYPE.value() || type == ModFluids.SOUL_LAVA_TYPE.get();
    }

    /** Whether the entity is in lava and swims through it. */
    public static boolean swimsIn(Entity entity) {
        return entity instanceof Player player && entity.isInLava() && wearing(player);
    }
}
