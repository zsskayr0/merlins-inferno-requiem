package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.block.SoulLavaBlock;

/** Soul Lava: a lava-like fluid that fills the Oblivion's basalt deltas. */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Merlins_inferno.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Merlins_inferno.MODID);

    public static final DeferredHolder<FluidType, FluidType> SOUL_LAVA_TYPE = FLUID_TYPES.register("soul_lava", () -> new FluidType(
            FluidType.Properties.create()
                    .descriptionId("block.merlins_inferno.soul_lava")
                    .canSwim(false).canDrown(false)
                    .pathType(PathType.LAVA).adjacentPathType(null)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                    .lightLevel(15).density(3000).viscosity(6000).temperature(1300)
                    .motionScale(0.0023333333333333335D)) {
        /** Like lava, but fire-proof items (Netherite, the Haunted Boat) bob up to the surface instead of drifting. */
        @Override
        public void setItemMovement(net.minecraft.world.entity.item.ItemEntity entity) {
            var motion = entity.getDeltaMovement();
            double lift = entity.fireImmune() ? Math.min(motion.y + 0.01, 0.06) : motion.y + (motion.y < 0.06 ? 5.0E-4 : 0.0);
            entity.setDeltaMovement(motion.x * 0.95, lift, motion.z * 0.95);
        }
    });

    public static final DeferredHolder<Fluid, FlowingFluid> SOUL_LAVA = FLUIDS.register("soul_lava",
            () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_SOUL_LAVA = FLUIDS.register("flowing_soul_lava",
            () -> new BaseFlowingFluid.Flowing(properties()));

    public static final DeferredBlock<LiquidBlock> SOUL_LAVA_BLOCK = ModBlocks.BLOCKS.register("soul_lava",
            () -> new SoulLavaBlock(SOUL_LAVA.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA)));
    public static final DeferredItem<BucketItem> SOUL_LAVA_BUCKET = ModItems.ITEMS.register("soul_lava_bucket",
            () -> new BucketItem(SOUL_LAVA.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(SOUL_LAVA_TYPE, SOUL_LAVA, FLOWING_SOUL_LAVA)
                .block(SOUL_LAVA_BLOCK).bucket(SOUL_LAVA_BUCKET)
                .slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(30).explosionResistance(100F);
    }

    private ModFluids() {
    }

    /** Soul Lava meeting water hardens like lava: obsidian from a source, blackstone from a flow. */
    public static void registerInteractions() {
        FluidInteractionRegistry.addInteraction(SOUL_LAVA_TYPE.get(), new FluidInteractionRegistry.InteractionInformation(
                NeoForgeMod.WATER_TYPE.value(),
                state -> state.isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState()));
    }

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
    }
}
