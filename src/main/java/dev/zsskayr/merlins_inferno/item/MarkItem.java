package dev.zsskayr.merlins_inferno.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import dev.zsskayr.merlins_inferno.mark.MarkHelper;

/**
 * A Mark: a powerful, cursed item that binds to its bearer and comes off only through very specific means (see
 * {@link MarkHelper}). Right-click takes it in; the 40% auto-equip on pickup is in {@code MarkPickupHandler}; with Curios it
 * also fits the {@code mark} slot ({@code compat.curios.CuriosMarks}). What a Mark does is up to its own handler.
 */
public class MarkItem extends Item {
    public MarkItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (MarkHelper.hasMark(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("item.merlins_inferno.mark.already_bound"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            MarkHelper.equip(player, this);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.merlins_inferno.mark.warning").withStyle(ChatFormatting.DARK_RED));
    }
}
