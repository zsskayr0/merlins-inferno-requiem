package dev.zsskayr.merlins_inferno.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

import dev.zsskayr.merlins_inferno.meteor.MeteorData;

/**
 * A compass tuned to fallen stars: its needle points at the exact spot of the nearest meteor of this world that nobody has
 * reached yet. The target is written to the stack's {@code lodestone_tracker} component by the server (so it syncs like any
 * component); the client only draws the needle, from {@code client.ModEntityRenderers}. With no meteor to find, it spins.
 */
public class SkyfallCompassItem extends Item {
    private static final int REFRESH_INTERVAL = 20;

    public SkyfallCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel server && (entity.tickCount + slot) % REFRESH_INTERVAL == 0) {
            retarget(stack, server, entity.blockPosition());
        }
    }

    /** Points the stack at the nearest unclaimed meteor of this level, or clears the target if there is none. */
    public static void retarget(ItemStack stack, ServerLevel level, BlockPos from) {
        BlockPos target = MeteorData.get(level.getServer().overworld()).nearest(level, from);
        LodestoneTracker current = stack.get(DataComponents.LODESTONE_TRACKER);
        if (target == null) {
            if (current != null) {
                stack.remove(DataComponents.LODESTONE_TRACKER);
            }
            return;
        }
        LodestoneTracker wanted = new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), target)), false);
        if (!wanted.equals(current)) {
            stack.set(DataComponents.LODESTONE_TRACKER, wanted);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(DataComponents.LODESTONE_TRACKER);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.merlins_inferno.skyfall_compass.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
