package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Grace Fire: infinite over Arganite, light 10, hurts the unholy through fire immunity, spares dropped items. */
@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class GraceFireGameTests {
    private static final BlockPos FLOOR = new BlockPos(1, 1, 1);
    private static final BlockPos FIRE = FLOOR.above();

    private static BlockState lit(GameTestHelper helper) {
        helper.setBlock(FLOOR, ModBlocks.ARGANITE.get());
        helper.setBlock(FIRE, ModBlocks.GRACE_FIRE.get());
        return helper.getBlockState(FIRE);
    }

    private static void stand(GameTestHelper helper, net.minecraft.world.entity.Entity entity) {
        lit(helper).entityInside(helper.getLevel(), helper.absolutePos(FIRE), entity);
    }

    @GameTest(template = "bench_empty")
    public static void burnsForeverOnArganiteWithLight(GameTestHelper helper) {
        BlockState fire = lit(helper);
        helper.assertTrue(fire.getLightEmission() == 10, "Grace Fire gives light 10");
        helper.assertTrue(helper.getBlockState(FLOOR).is(helper.getLevel().dimensionType().infiniburn()),
                "Arganite is in the Overworld's infiniburn tag");
        helper.assertTrue(!helper.getBlockState(FLOOR.east()).is(helper.getLevel().dimensionType().infiniburn()),
                "Plain air is not infiniburn");
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void unholyBurnThroughFireImmunity(GameTestHelper helper) {
        var skeleton = helper.spawn(EntityType.WITHER_SKELETON, FIRE);
        skeleton.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600));
        helper.assertTrue(skeleton.fireImmune(), "A wither skeleton is fire immune");
        float before = skeleton.getHealth();
        stand(helper, skeleton);
        helper.assertTrue(Math.abs(before - skeleton.getHealth() - 4.0F) < 0.01F,
                "The unholy take 4 (twice Soul Fire) despite fire immunity and Fire Resistance, took " + (before - skeleton.getHealth()));
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void ordinaryCreaturesTakePlainFireDamage(GameTestHelper helper) {
        LivingEntity pig = helper.spawn(EntityType.PIG, FIRE);
        float before = pig.getHealth();
        stand(helper, pig);
        helper.assertTrue(Math.abs(before - pig.getHealth() - 1.0F) < 0.01F, "A pig takes a plain fire's 1, took " + (before - pig.getHealth()));
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void droppedItemsDoNotBurn(GameTestHelper helper) {
        ItemEntity item = helper.getLevel().getServer() == null ? null : new ItemEntity(helper.getLevel(),
                helper.absolutePos(FIRE).getX() + 0.5, helper.absolutePos(FIRE).getY(), helper.absolutePos(FIRE).getZ() + 0.5, new ItemStack(Items.DIAMOND));
        helper.getLevel().addFreshEntity(item);
        stand(helper, item);
        helper.assertTrue(item.getRemainingFireTicks() <= 0 && !item.isOnFire(), "The item is not set alight");
        helper.assertTrue(!item.isRemoved(), "The item survives");
        helper.succeed();
    }
}
