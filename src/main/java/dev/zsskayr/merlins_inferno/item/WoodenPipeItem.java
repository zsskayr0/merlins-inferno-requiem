package dev.zsskayr.merlins_inferno.item;

import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/** A wooden pipe: smoking a Dried Edenweed from the inventory grants the Druidic Trance for ninety seconds. */
public class WoodenPipeItem extends Item {
    public static final int TRANCE_TICKS = 1800;
    private static final int SMOKE_TICKS = 32;

    public WoodenPipeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pipe = player.getItemInHand(hand);
        if (findDried(player).isEmpty() && !player.getAbilities().instabuild) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.merlins_inferno.pipe.empty"), true);
            }
            return InteractionResultHolder.fail(pipe);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(pipe);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack pipe, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return pipe;
        }
        ItemStack dried = findDried(player);
        if (dried.isEmpty() && !player.getAbilities().instabuild) {
            return pipe;
        }
        if (!level.isClientSide) {
            if (!player.getAbilities().instabuild) {
                dried.shrink(1);
                pipe.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            }
            player.addEffect(new MobEffectInstance(ModEffects.DRUIDIC_TRANCE, TRANCE_TICKS, 0));
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getEyeY() - 0.1, player.getZ(), 6, 0.2, 0.1, 0.2, 0.01);
            }
        }
        return pipe;
    }

    private static ItemStack findDried(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.DRIED_EDENWEED.get())) {
                return stack;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.is(ModItems.DRIED_EDENWEED.get()) ? offhand : ItemStack.EMPTY;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return SMOKE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}
