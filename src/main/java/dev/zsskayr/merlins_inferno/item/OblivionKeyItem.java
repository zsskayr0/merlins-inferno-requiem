package dev.zsskayr.merlins_inferno.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import dev.zsskayr.merlins_inferno.portal.OblivionPortalShape;

/**
 * The Key of Oblivion, forged in the opened Pandora Box. Used on an obsidian frame it lights an Oblivion portal, the
 * way flint and steel lights a Nether portal; it has three uses.
 */
public class OblivionKeyItem extends Item {
    public OblivionKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Optional<OblivionPortalShape> shape = OblivionPortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X);
        if (shape.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            shape.get().createPortalBlocks();
            level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6F, 1.6F);
            Player player = context.getPlayer();
            ItemStack stack = context.getItemInHand();
            if (player != null) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.merlins_inferno.oblivion_key_tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.merlins_inferno.oblivion_key_uses", stack.getMaxDamage() - stack.getDamageValue())
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
