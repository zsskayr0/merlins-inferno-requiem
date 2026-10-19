package dev.zsskayr.merlins_inferno.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.block.HellForgeVaultBlock;

/**
 * Right-click a {@link HellForgeVaultBlock} to cycle its mode (input, output, fuel); sneak and
 * right-click to toggle its auto transfer with an adjacent inventory. The mode shows on the
 * block's texture; both changes are also confirmed on the action bar.
 */
public class ForgeWrenchItem extends Item {
    public ForgeWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof HellForgeVaultBlock)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            Player player = context.getPlayer();
            Component message;
            if (player != null && player.isShiftKeyDown()) {
                boolean auto = !state.getValue(HellForgeVaultBlock.AUTO);
                level.setBlock(pos, state.setValue(HellForgeVaultBlock.AUTO, auto), 3);
                if (auto) {
                    level.scheduleTick(pos, state.getBlock(), HellForgeVaultBlock.AUTO_INTERVAL);
                }
                message = Component.translatable("message.merlins_inferno.hell_forge.vault_auto." + (auto ? "on" : "off"));
            } else {
                HellForgeVaultBlock.Mode next = state.getValue(HellForgeVaultBlock.MODE).next();
                level.setBlock(pos, state.setValue(HellForgeVaultBlock.MODE, next), 3);
                message = next.displayName();
            }
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.6F, 1.2F);
            if (player != null) {
                player.displayClientMessage(message, true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
