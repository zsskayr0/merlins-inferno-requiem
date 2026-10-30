package dev.zsskayr.merlins_inferno.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import dev.zsskayr.merlins_inferno.mark.MarkHelper;

/** The one cure for a Mark (besides creative): read it and the bearer is free; the Mark is gone for good. */
public class ScrollOfSalvationItem extends Item {
    public ScrollOfSalvationItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!MarkHelper.hasMark(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("item.merlins_inferno.scroll_of_salvation.no_mark"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            MarkHelper.clear(player);
            level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 0.7F);
            player.displayClientMessage(Component.translatable("item.merlins_inferno.scroll_of_salvation.freed"), true);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.merlins_inferno.scroll_of_salvation.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
