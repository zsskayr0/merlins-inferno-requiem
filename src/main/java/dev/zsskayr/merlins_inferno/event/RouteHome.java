package dev.zsskayr.merlins_inferno.event;

import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Home ground: the gear of each route wears slower and digs faster in the dimensions that belong to that route - whatever the Circle of
 * the set. One class holds the whole matrix, so a new set or a new dimension is a single line (or a tag entry) away:
 * <ul>
 *   <li>the <b>route</b> of an item comes from the {@code route_*} item tags (add the new set's pieces there, no code needed);</li>
 *   <li>the <b>dimensions</b> of a route are {@link Route#home}.</li>
 * </ul>
 * At home an item resists wear as if it had a {@link #SAVE_CHANCE} Unbreaking on every point of damage (tools and armor alike), and a
 * tool in hand mines {@link #SPEED_BONUS} faster. The wear itself is cut by {@code ItemStackMixin}.
 */
public final class RouteHome {
    /** Chance, per point of damage, that a piece of gear at home takes none. */
    public static final float SAVE_CHANCE = 0.30F;
    /** Mining speed a route tool gains at home, as a fraction. */
    public static final float SPEED_BONUS = 0.20F;

    private static ResourceKey<Level> dimension(String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, path));
    }

    /** The three routes, each with the item tag that lists its gear and the dimensions where that gear is at home. */
    public enum Route {
        /** The Druidic road: the Overworld (a mundane dimension of its own will join it here when it exists). */
        MUNDANE("mundane", ModTags.Items.ROUTE_MUNDANE, Set.of(Level.OVERWORLD)),
        /** The Infernal road: the Nether and Oblivion. */
        INFERNAL("infernal", ModTags.Items.ROUTE_INFERNAL, Set.of(Level.NETHER, dimension("oblivion"))),
        /** The Angelical road: El'ysium and the End. */
        ANGELICAL("angelical", ModTags.Items.ROUTE_ANGELICAL, Set.of(dimension("elysium"), Level.END));

        public final String id;
        public final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> gear;
        public final Set<ResourceKey<Level>> home;

        Route(String id, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> gear, Set<ResourceKey<Level>> home) {
            this.id = id;
            this.gear = gear;
            this.home = home;
        }
    }

    /** The route this item belongs to, or null if it belongs to none. */
    @Nullable
    public static Route routeOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (Route route : Route.values()) {
            if (stack.is(route.gear)) {
                return route;
            }
        }
        return null;
    }

    /** True when the item's route calls this dimension home. */
    public static boolean isHome(ItemStack stack, ResourceKey<Level> dimension) {
        Route route = routeOf(stack);
        return route != null && route.home.contains(dimension);
    }

    /** What a hit of {@code amount} wear really costs the item: each point may be saved when the wearer stands on home ground. */
    public static int adjustDamage(ItemStack stack, int amount, @Nullable Player player) {
        if (amount <= 0 || player == null || !isHome(stack, player.level().dimension())) {
            return amount;
        }
        int cost = 0;
        for (int i = 0; i < amount; i++) {
            if (player.getRandom().nextFloat() >= SAVE_CHANCE) {
                cost++;
            }
        }
        return cost;
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (isHome(player.getMainHandItem(), player.level().dimension())) {
            event.setNewSpeed(event.getNewSpeed() * (1.0F + SPEED_BONUS));
        }
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        Route route = routeOf(event.getItemStack());
        if (route != null) {
            List<Component> tooltip = event.getToolTip();
            // Only a digging tool gains anything from the mining bonus; weapons and armor just get the wear line.
            String key = "tooltip.merlins_inferno.home." + route.id
                    + (event.getItemStack().getItem() instanceof net.minecraft.world.item.DiggerItem ? "" : ".wear");
            tooltip.add(Component.translatable(key, Math.round(SAVE_CHANCE * 100), Math.round(SPEED_BONUS * 100)).withStyle(ChatFormatting.GRAY));
        }
    }
}
