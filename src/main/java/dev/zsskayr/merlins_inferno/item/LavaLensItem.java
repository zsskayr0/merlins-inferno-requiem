package dev.zsskayr.merlins_inferno.item;

import java.util.List;
import dev.zsskayr.merlins_inferno.event.LavaLensHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Activate in hand, then carry anywhere in inventory or equip in a Curios slot. */
public final class LavaLensItem extends Item {
    public static final int DURABILITY = 600;
    public LavaLensItem(Properties properties) { super(properties.durability(DURABILITY).fireResistant()); }
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
        tooltip.add(Component.translatable("item.merlins_inferno.lava_lens.wear").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.merlins_inferno.lava_lens.protection").withStyle(ChatFormatting.DARK_GRAY));
    }
}
