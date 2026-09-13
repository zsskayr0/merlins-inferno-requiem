package dev.zsskayr.merlins_inferno.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import dev.zsskayr.merlins_inferno.compat.patchouli.PatchouliCompat;

/**
 * The Grimmorium - originally registered as a plain placeholder item (see the old comment in
 * {@code ModItems}) for the mod's future onboarding/tutorial flow. That flow turned out to be the
 * in-game guidebook: this is the same item, now actually opening it
 * ({@code data/merlins_inferno/patchouli_books/guide/}) on use, rather than a second, separate
 * "guide book" item existing alongside it.
 * <p>
 * Patchouli is an optional dependency - without it installed, using the item just does nothing
 * (server-side only, so a client with Patchouli but a server without it, or vice versa, never
 * desyncs trying to open a GUI the other side can't render).
 */
public class GrimmoriumItem extends Item {
    public GrimmoriumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && ModList.get().isLoaded("patchouli")) {
            PatchouliCompat.openGrimoire(serverPlayer);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        return InteractionResultHolder.pass(stack);
    }
}
