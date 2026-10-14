package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.CainEntity;
import dev.zsskayr.merlins_inferno.entity.FleshGolemEntity;
import dev.zsskayr.merlins_inferno.item.DarkCovenItem;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Circle 2's Ebony line: the Dark Coven's boons and upgrade chain, Cain, the Flesh Golem and their data. */
@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class EbonyGameTests {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, path);
    }

    @GameTest(template = "bench_empty")
    public static void darkCovenBoonsScaleWithStage(GameTestHelper helper) {
        var level = helper.getLevel();
        var first = helper.makeMockPlayer(GameType.SURVIVAL);
        first.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DARK_COVEN_I.get()));
        ModItems.DARK_COVEN_I.get().use(level, first, InteractionHand.MAIN_HAND);
        helper.assertTrue(DarkCovenItem.isInvoked(first.getMainHandItem()), "Right-click invokes the coven");
        helper.assertTrue(first.getEffect(MobEffects.DAMAGE_BOOST) != null && first.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() == 2,
                "Stage I gives Strength III");
        helper.assertTrue(first.getEffect(MobEffects.WITHER) != null && first.getEffect(MobEffects.WITHER).getAmplifier() == 0,
                "Stage I costs Wither I");
        helper.assertTrue(first.getEffect(MobEffects.WITHER).getDuration() == 7 * 20, "Stage I lasts 7 seconds");
        helper.assertTrue(first.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() == 7 * 20, "The boons last as long as the Wither");
        helper.assertTrue(!first.getCooldowns().isOnCooldown(ModItems.DARK_COVEN_I.get()), "There is no cooldown");

        // The Wither fades: every other effect goes with it and the coven is dismissed.
        first.removeEffect(MobEffects.WITHER);
        first.getMainHandItem().inventoryTick(level, first, 0, true);
        helper.assertTrue(!DarkCovenItem.isInvoked(first.getMainHandItem()), "No Wither, no coven");
        helper.assertTrue(first.getEffect(MobEffects.DAMAGE_BOOST) == null && first.getEffect(MobEffects.MOVEMENT_SPEED) == null
                && first.getEffect(MobEffects.NIGHT_VISION) == null, "The other effects go with the Wither");

        // A second right-click dismisses a running coven; stowing the sword does too.
        ModItems.DARK_COVEN_I.get().use(level, first, InteractionHand.MAIN_HAND);
        ModItems.DARK_COVEN_I.get().use(level, first, InteractionHand.MAIN_HAND);
        helper.assertTrue(!DarkCovenItem.isInvoked(first.getMainHandItem()), "A second right-click dismisses it");
        ModItems.DARK_COVEN_I.get().use(level, first, InteractionHand.MAIN_HAND);
        first.getMainHandItem().inventoryTick(level, first, 5, false);
        helper.assertTrue(!DarkCovenItem.isInvoked(first.getMainHandItem()), "Stowing the sword dismisses the coven");

        var last = helper.makeMockPlayer(GameType.SURVIVAL);
        last.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DARK_COVEN_VII.get()));
        ModItems.DARK_COVEN_VII.get().use(level, last, InteractionHand.MAIN_HAND);
        helper.assertTrue(last.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() == 9, "The pact gives Strength X");
        helper.assertTrue(last.getEffect(MobEffects.DAMAGE_RESISTANCE) != null, "The pact gives Resistance");
        helper.assertTrue(last.getEffect(MobEffects.WITHER).getDuration() == 13 * 20, "Stage VII lasts 13 seconds");
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void upgradeChainAndDataLoad(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (int n = 2; n <= 7; n++) {
            helper.assertTrue(server.getRecipeManager().byKey(id("dark_coven_" + n + "_upgrade")).isPresent(), "Upgrade recipe to stage " + n);
        }
        for (String recipe : new String[] {"dark_coven", "ebony_bar_from_hell_forge", "ebony_pickaxe", "ebony_helmet"}) {
            helper.assertTrue(server.getRecipeManager().byKey(id(recipe)).isPresent(), "Recipe " + recipe);
        }
        for (String table : new String[] {"entities/cain", "entities/flesh_golem", "blocks/ebony_debris", "chests/boss/cain",
                "inject/ebony_scrap_from_ancient_debris"}) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id(table));
            helper.assertTrue(server.reloadableRegistries().getLootTable(key) != LootTable.EMPTY, "Loot table " + table);
        }
        helper.assertTrue(((DarkCovenItem) ModItems.DARK_COVEN_VII.get()).getStage() == DarkCovenItem.MAX_STAGE, "Last item is the last stage");
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void cainCarriesTheCovenAndShrugsOffWither(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        CainEntity cain = ModEntityTypes.CAIN.get().create(level);
        cain.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        cain.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.COMMAND, null);
        level.addFreshEntity(cain);
        helper.assertTrue(cain.getItemBySlot(EquipmentSlot.MAINHAND).is(ModItems.DARK_COVEN_VII.get()), "Cain wields the last Dark Coven");
        helper.assertTrue(cain.getMaxHealth() == CainEntity.MAX_HEALTH, "Cain has 800 health");
        helper.assertTrue(!cain.hurt(level.damageSources().wither(), 10.0F), "The Wither cannot hurt Cain");
        helper.assertTrue(!cain.canBeAffected(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WITHER, 100)),
                "The Wither effect cannot affect Cain");
        cain.discard();
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void fleshGolemGorges(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        FleshGolemEntity golem = ModEntityTypes.FLESH_GOLEM.get().create(level);
        golem.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(golem);
        golem.setHealth(50.0F);
        var victim = helper.makeMockPlayer(GameType.SURVIVAL);
        victim.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        golem.doHurtTarget(victim);
        helper.assertTrue(golem.getHealth() > 50.0F, "The golem heals when it lands a blow");
        golem.discard();
        helper.succeed();
    }
}
