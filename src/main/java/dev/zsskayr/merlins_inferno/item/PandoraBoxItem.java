package dev.zsskayr.merlins_inferno.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import dev.zsskayr.merlins_inferno.menu.PandoraBoxMenu;

/**
 * Pandora's Box - the gate between Circle 1 and Circle 2. Found in the Sacred Church's chest. Place it and
 * right-click the block, or sneak + use it from the hand, to open its ritual screen ({@link PandoraBoxMenu}).
 * It never leaves its owner: see {@code event.PandoraHandler}.
 */
public class PandoraBoxItem extends BlockItem implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.pandora_box.idle");

    /** Client: set every frame by the screen hook while the cursor is over a Box in a slot. */
    public static boolean hovered;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PandoraBoxItem(Block block, Properties properties) {
        super(block, properties);
    }

    private static void openMenu(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PandoraBoxMenu(id, inventory),
                    Component.translatable("container.merlins_inferno.pandora_box")));
        }
    }

    /** Sneaking never places it; it opens the screen instead. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            if (!context.getLevel().isClientSide) {
                openMenu(player);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide) {
                openMenu(player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("item.merlins_inferno.pandora_box_tooltip.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.merlins_inferno.pandora_box_tooltip.2").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.merlins_inferno.pandora_box_tooltip.3").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "box", 0, state -> hovered ? state.setAndContinue(IDLE) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
