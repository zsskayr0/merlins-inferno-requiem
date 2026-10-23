package dev.zsskayr.merlins_inferno.item;

import java.util.function.Supplier;
import dev.zsskayr.merlins_inferno.portal.OblivionDimension;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Everything that sets one pair of wings apart from another: cooldown, look, how fast it wears and how hard it pulls in
 * each world, and the route it belongs to.
 *
 * @param recharge multiplier on the cooldown in each world (below 1 = the wings recharge faster there)
 * @param route    the armour the wings are at home with; wings that have one are slowed when worn in a Curios slot or
 *                 with any other armour stronger than iron
 * @param repair   what mends the wings in an anvil, or null when they cannot be mended at all
 */
public record WingsProfile(int cooldownTicks, ParticleOptions trail, ResourceLocation texture, PerWorld wear, PerWorld boost,
                           PerWorld recharge, TagKey<Item> route, Supplier<Item> repair) {

    public WingsProfile(int cooldownTicks, ParticleOptions trail, ResourceLocation texture, PerWorld wear, PerWorld boost,
                        TagKey<Item> route, Supplier<Item> repair) {
        this(cooldownTicks, trail, texture, wear, boost, PerWorld.FLAT, route, repair);
    }

    /** The cooldown, in ticks, after a boost in this world. */
    public int cooldownIn(Level level) {
        return Math.max(1, (int) Math.round(cooldownTicks * recharge.in(level)));
    }

    /** A multiplier for each of the three worlds; everything that is neither the Nether nor Oblivion counts as the Overworld. */
    public record PerWorld(double overworld, double nether, double oblivion) {
        public static final PerWorld FLAT = new PerWorld(1, 1, 1);

        public double in(Level level) {
            ResourceKey<Level> dimension = level.dimension();
            if (dimension == OblivionDimension.LEVEL) return oblivion;
            return dimension == Level.NETHER ? nether : overworld;
        }
    }
}
