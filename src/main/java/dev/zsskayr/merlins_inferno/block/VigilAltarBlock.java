package dev.zsskayr.merlins_inferno.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import dev.zsskayr.merlins_inferno.blockentity.VigilAltarBlockEntity;

/**
 * The altar of a Vigil Shrine. A diamond laid on it - a tithe - keeps the vigil again: it rehangs the
 * Great Bell and wakes the Penitent, once the day-long cooldown after the last one's death has
 * passed and no Penitent is already awake. Unbreakable in survival (it holds the shrine's state).
 */
public class VigilAltarBlock extends Block implements EntityBlock {
    public VigilAltarBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VigilAltarBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.DIAMOND)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(level.getBlockEntity(pos) instanceof VigilAltarBlockEntity altar)) {
            return ItemInteractionResult.CONSUME;
        }
        if (altar.hasLivingPenitent(serverLevel)) {
            player.displayClientMessage(Component.translatable("message.merlins_inferno.vigil_altar.awake"), true);
        } else if (!altar.isReady(level)) {
            player.displayClientMessage(Component.translatable("message.merlins_inferno.vigil_altar.cooldown", altar.remainingMinutes(level)), true);
        } else if (altar.invoke(serverLevel)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.displayClientMessage(Component.translatable("message.merlins_inferno.vigil_altar.invoked"), true);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.merlins_inferno.vigil_altar.hint"), true);
        }
        return InteractionResult.SUCCESS;
    }
}
