package dev.zsskayr.merlins_inferno.registry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.portal.OblivionDimension;
import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;

/**
 * Placement modifiers of this mod. {@code merlins_inferno:in_oblivion} lets a feature generate only in the Oblivion
 * dimension, which shares the vanilla Nether biomes: a biome modifier alone cannot tell the two dimensions apart.
 */
public final class ModPlacementModifiers {
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, Merlins_inferno.MODID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<InOblivion>> IN_OBLIVION =
            PLACEMENT_MODIFIERS.register("in_oblivion", () -> () -> InOblivion.CODEC);
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<CircleAtLeast>> CIRCLE_AT_LEAST =
            PLACEMENT_MODIFIERS.register("circle_at_least", () -> () -> CircleAtLeast.CODEC);
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<CircleBelow>> CIRCLE_BELOW =
            PLACEMENT_MODIFIERS.register("circle_below", () -> () -> CircleBelow.CODEC);

    private ModPlacementModifiers() {
    }

    public static void register(IEventBus bus) {
        PLACEMENT_MODIFIERS.register(bus);
    }

    public static final class InOblivion extends PlacementFilter {
        public static final MapCodec<InOblivion> CODEC = MapCodec.unit(new InOblivion());

        @Override
        protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
            return context.getLevel().getLevel().dimension() == OblivionDimension.LEVEL;
        }

        @Override
        public PlacementModifierType<?> type() {
            return IN_OBLIVION.get();
        }
    }

    public static final class CircleAtLeast extends PlacementFilter {
        public static final MapCodec<CircleAtLeast> CODEC = net.minecraft.util.ExtraCodecs.POSITIVE_INT.fieldOf("circle").xmap(CircleAtLeast::new, value -> value.circle);
        private final int circle;
        public CircleAtLeast(int circle) { this.circle = circle; }
        @Override protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
            return ProgressionHelper.hasReached(context.getLevel().getLevel(), circle);
        }
        @Override public PlacementModifierType<?> type() { return CIRCLE_AT_LEAST.get(); }
    }

    public static final class CircleBelow extends PlacementFilter {
        public static final MapCodec<CircleBelow> CODEC = net.minecraft.util.ExtraCodecs.POSITIVE_INT.fieldOf("circle").xmap(CircleBelow::new, value -> value.circle);
        private final int circle;
        public CircleBelow(int circle) { this.circle = circle; }
        @Override protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
            return !ProgressionHelper.hasReached(context.getLevel().getLevel(), circle);
        }
        @Override public PlacementModifierType<?> type() { return CIRCLE_BELOW.get(); }
    }
}
