package dev.zsskayr.merlins_inferno.item;

import java.util.List;
import dev.zsskayr.merlins_inferno.event.LavaLensHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Activate in hand, then carry anywhere in inventory or equip in a Curios slot. */
public final class LavaLensItem extends Item {
    public static final int DURABILITY = 600;
    /** Written on the stack that is powering the effect: the tooltip and the glowing model read it. */
    public static final String ACTIVE_TAG = "MerlinsHellSightActive";
    public LavaLensItem(Properties properties) { super(properties.durability(DURABILITY).fireResistant()); }
    public static boolean isActive(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(ACTIVE_TAG);
    }
    public static void setActive(ItemStack stack, boolean active) {
        if (isActive(stack) == active) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (active) tag.putBoolean(ACTIVE_TAG, true); else tag.remove(ACTIVE_TAG);
        });
        if (!active && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
    }
    /** The glint itself is drawn red by client.HellSightGlint. */
    @Override public boolean isFoil(ItemStack stack) { return isActive(stack); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            boolean enabled = !player.getPersistentData().getBoolean(LavaLensHandler.ENABLED);
            player.getPersistentData().putBoolean(LavaLensHandler.ENABLED, enabled);
            player.displayClientMessage(Component.translatable(enabled ? "item.merlins_inferno.lava_lens.on" : "item.merlins_inferno.lava_lens.off"), true);
            player.getCooldowns().addCooldown(this, 6);
            LavaLensHandler.refresh(player, false);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.merlins_inferno.lava_lens.tooltip").withStyle(ChatFormatting.GRAY));
        if (isActive(stack)) tooltip.add(Component.translatable("item.merlins_inferno.lava_lens.active").withStyle(ChatFormatting.RED));
    }
}
