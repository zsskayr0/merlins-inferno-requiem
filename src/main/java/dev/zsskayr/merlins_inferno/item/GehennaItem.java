package dev.zsskayr.merlins_inferno.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import dev.zsskayr.merlins_inferno.event.GehennaHandler;

/**
 * Gehenna, the demonic route's mythic of Circle 1: a warhammer that held a demon's latent power through the ages,
 * tormenting all it touched. Its Unique, <b>Bloody</b> (event.GehennaHandler):
 * <ul>
 *   <li>a foe killed in one blow feeds it: the next {@value GehennaHandler#FED_HITS} blows hit harder;</li>
 *   <li>a blow that does not kill wakes its frenzy - harder still, for {@value GehennaHandler#FRENZY_SECONDS} seconds,
 *   kept alive by every further blow - but only on a bearer with at least {@value GehennaHandler#FRENZY_MIN_HEALTH}
 *   health;</li>
 *   <li>a frenzy that ends without a kill takes its due: the bearer suffers {@value GehennaHandler#RECOIL_PERCENT}% of
 *   the last damage they dealt.</li>
 * </ul>
 * One upgrade only, when the world reaches Circle 2 (still to be built).
 */
public class GehennaItem extends SwordItem {
    public GehennaItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        // Only the name of its Unique: the player finds out what it does by bleeding for it.
        tooltip.add(Component.translatable("item.merlins_inferno.gehenna.tooltip.unique").withStyle(ChatFormatting.DARK_RED));
    }
}
