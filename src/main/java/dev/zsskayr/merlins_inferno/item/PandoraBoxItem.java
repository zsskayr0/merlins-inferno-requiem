package dev.zsskayr.merlins_inferno.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import dev.zsskayr.merlins_inferno.menu.PandoraBoxMenu;

/**
 * Pandora's Box - the gate between Circle 1 and Circle 2. Found in the Sacred Church's chest; using it opens its
 * ritual screen ({@link PandoraBoxMenu}). It never leaves its owner: see {@code event.PandoraHandler}.
 */
public class PandoraBoxItem extends Item {
    public PandoraBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PandoraBoxMenu(id, inventory),
                    Component.translatable("item.merlins_inferno.pandora_box")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("item.merlins_inferno.pandora_box_tooltip.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.merlins_inferno.pandora_box_tooltip.2").withStyle(ChatFormatting.DARK_RED));
    }
}
