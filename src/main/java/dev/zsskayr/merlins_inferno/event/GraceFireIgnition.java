package dev.zsskayr.merlins_inferno.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/** A plain flint and steel used on top of Arganite lights Grace Fire instead of ordinary fire. */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class GraceFireIgnition {
    private GraceFireIgnition() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(Items.FLINT_AND_STEEL) || event.getFace() != Direction.UP) {
            return;
        }
        Level level = event.getLevel();
        Player player = event.getEntity();
        BlockPos target = event.getPos().above();
        if (!level.getBlockState(event.getPos()).is(ModBlocks.ARGANITE.get()) || !level.getBlockState(target).isAir()
                || !player.mayUseItemAt(target, Direction.UP, event.getItemStack())) {
            return;
        }
        if (!level.isClientSide) {
            level.playSound(null, target, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 0.8F);
            level.setBlock(target, ModBlocks.GRACE_FIRE.get().defaultBlockState(), 11);
            level.gameEvent(player, GameEvent.BLOCK_PLACE, target);
            event.getItemStack().hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
    }
}
