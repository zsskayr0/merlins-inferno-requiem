package dev.zsskayr.merlins_inferno.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A key forged in the opened Pandora Box (Oblivion or Purgatory). The portals they open are Circle 2 content that
 * does not exist yet, so for now this is only a marker item with a tooltip saying so.
 */
public class PortalKeyItem extends Item {
    private final String id;

    public PortalKeyItem(Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.merlins_inferno." + this.id + "_key_tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.merlins_inferno.portal_key_dormant").withStyle(ChatFormatting.DARK_GRAY));
    }
}
