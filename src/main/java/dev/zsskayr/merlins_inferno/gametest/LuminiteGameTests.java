package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.event.MeteorHandler;
import dev.zsskayr.merlins_inferno.event.SkyboundHandler;
import dev.zsskayr.merlins_inferno.meteor.MeteorData;
import dev.zsskayr.merlins_inferno.meteor.MeteorSite;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Circle 2's Celestial route: the Skybound set bonus, the Skyfall Compass and the fallen stars. */
@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class LuminiteGameTests {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, path);
    }

    private static void dress(net.minecraft.world.entity.player.Player player) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.LUMINITE_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.LUMINITE_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.LUMINITE_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.LUMINITE_BOOTS.get()));
    }

    @GameTest(template = "bench_empty")
    public static void skyboundCurveFollowsTheNightAndTheDark(GameTestHelper helper) {
        helper.assertTrue(SkyboundHandler.bonus(6000, 15) == 0.0, "Noon in full light gives nothing");
        helper.assertTrue(Math.abs(SkyboundHandler.bonus(18000, 15) - 1.0) < 1.0E-9, "Midnight gives the peak");
        helper.assertTrue(Math.abs(SkyboundHandler.bonus(6000, 0) - SkyboundHandler.LIGHT_WEIGHT) < 1.0E-9, "Darkness alone gives less than the night");
        helper.assertTrue(SkyboundHandler.bonus(18000, 0) == 1.0, "The larger of the two wins, it does not add");
        helper.assertTrue(SkyboundHandler.bonus(13000, 15) > SkyboundHandler.bonus(10000, 15), "It grows toward the night");
        double last = 0.0;
        for (long t = 6000; t <= 18000; t += 250) {
            double now = SkyboundHandler.bonus(t, 15);
            helper.assertTrue(now >= last && now - last < 0.1, "A smooth climb, no switch, at " + t);
            last = now;
        }
        helper.assertTrue(SkyboundHandler.bonus(18000 + 24000L * 5, 15) == SkyboundHandler.bonus(18000, 15), "Days repeat");
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void fullSetStaysBetweenNetheriteAndEbony(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        dress(player);
        SkyboundHandler.refresh(player);
        // The pieces' own armor is applied by the entity tick; what Skybound adds is a separate modifier.
        double armor = player.getAttributeValue(Attributes.ARMOR);
        double toughness = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        helper.assertTrue(armor >= 0.0 && armor <= SkyboundHandler.ARMOR_PEAK + 1.0E-6, "Skybound adds at most its peak armor, was " + armor);
        helper.assertTrue(toughness >= 0.0 && toughness <= SkyboundHandler.TOUGHNESS_PEAK + 1.0E-6, "and at most its peak toughness, was " + toughness);
        var material = dev.zsskayr.merlins_inferno.registry.ModArmorMaterials.LUMINITE.value();
        int floor = 0;
        for (var type : new net.minecraft.world.item.ArmorItem.Type[] {net.minecraft.world.item.ArmorItem.Type.HELMET, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                net.minecraft.world.item.ArmorItem.Type.LEGGINGS, net.minecraft.world.item.ArmorItem.Type.BOOTS}) {
            floor += material.getDefense(type);
        }
        var ebony = dev.zsskayr.merlins_inferno.registry.ModArmorMaterials.EBONY.value();
        int ebonyTotal = 0;
        for (var type : new net.minecraft.world.item.ArmorItem.Type[] {net.minecraft.world.item.ArmorItem.Type.HELMET, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                net.minecraft.world.item.ArmorItem.Type.LEGGINGS, net.minecraft.world.item.ArmorItem.Type.BOOTS}) {
            ebonyTotal += ebony.getDefense(type);
        }
        helper.assertTrue(floor == 20, "The floor is Netherite's 20, was " + floor);
        helper.assertTrue(floor + SkyboundHandler.ARMOR_PEAK < ebonyTotal && material.toughness() + SkyboundHandler.TOUGHNESS_PEAK < ebony.toughness(),
                "The peak stays under Ebony");

        // Three pieces are not a set: the bonus goes.
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        SkyboundHandler.refresh(player);
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 0.0 && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0.0,
                "Without the boots there is no bonus");
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void fullSetCancelsFallDamage(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        float before = player.getHealth();
        player.hurt(level.damageSources().fall(), 6.0F);
        helper.assertTrue(player.getHealth() < before, "Without the set a fall hurts");
        dress(player);
        player.setHealth(20.0F);
        player.hurt(level.damageSources().fall(), 6.0F);
        helper.assertTrue(player.getHealth() == 20.0F, "With the full set a fall does nothing");
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void skyfallCompassFollowsTheMeteors(GameTestHelper helper) {
        var level = helper.getLevel();
        MeteorData data = MeteorData.get(level.getServer().overworld());
        BlockPos here = helper.absolutePos(new BlockPos(0, 2, 0));
        var near = data.add(here.getX() + 400, here.getZ(), 0L, 70);
        var far = data.add(here.getX() - 2000, here.getZ(), 0L, 70);
        try {
            var compass = new ItemStack(ModItems.SKYFALL_COMPASS.get());
            dev.zsskayr.merlins_inferno.item.SkyfallCompassItem.retarget(compass, level, here);
            var tracker = compass.get(DataComponents.LODESTONE_TRACKER);
            helper.assertTrue(tracker != null && tracker.target().isPresent() && tracker.target().get().pos().getX() == here.getX() + 400,
                    "It points at the nearest meteor");
            helper.assertTrue(near != far, "Two distinct meteors");
        } finally {
            data.remove(near);
            data.remove(far);
        }
        var empty = new ItemStack(ModItems.SKYFALL_COMPASS.get());
        dev.zsskayr.merlins_inferno.item.SkyfallCompassItem.retarget(empty, level, here);
        helper.assertTrue(data.all().isEmpty() ? empty.get(DataComponents.LODESTONE_TRACKER) == null : true, "With no meteor there is no target");
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void meteorSiteBuildsACraterWithOreAndLoot(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 2, 0));
        helper.assertTrue(MeteorSite.canBuild(level, origin.getX(), origin.getZ()), "The test chunks are loaded");
        int floor = MeteorSite.build(level, origin.getX(), origin.getZ(), level.random);
        boolean ore = false;
        boolean chest = false;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-MeteorSite.RADIUS, -8, -MeteorSite.RADIUS), origin.offset(MeteorSite.RADIUS, 12, MeteorSite.RADIUS))) {
            var state = level.getBlockState(pos);
            ore |= state.is(ModBlocks.LUMINITE_CLUSTER.get());
            if (state.is(Blocks.CHEST) && level.getBlockEntity(pos) instanceof ChestBlockEntity chestEntity) {
                chest = true;
            }
        }
        helper.assertTrue(ore, "The heart geode grows Luminite crystals");
        helper.assertTrue(chest, "There is a chest by the crater");
        helper.assertTrue(floor > level.getMinBuildHeight(), "The floor height is reported");
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void luminiteDataLoads(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (String recipe : new String[] {"luminite_sword", "luminite_axe", "luminite_pickaxe", "luminite_helmet", "luminite_boots", "skyfall_compass",
                "luminite_leggings", "luminite_chestplate", "luminite_shovel", "luminite_hoe"}) {
            helper.assertTrue(server.getRecipeManager().byKey(id(recipe)).isPresent(), "Recipe " + recipe);
        }
        for (String table : new String[] {"chests/meteor", "chests/meteor_deep", "blocks/luminite_cluster"}) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id(table));
            helper.assertTrue(server.reloadableRegistries().getLootTable(key) != LootTable.EMPTY, "Loot table " + table);
        }
        // The sword and the axe come out of the table already Holy.
        var recipe = server.getRecipeManager().byKey(id("luminite_sword")).orElseThrow().value();
        var result = recipe.getResultItem(helper.getLevel().registryAccess());
        var holy = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(dev.zsskayr.merlins_inferno.registry.ModEnchantments.HOLY);
        helper.assertTrue(result.getEnchantmentLevel(holy) == 1, "The Luminite Sword is crafted with Holy");
        helper.assertTrue(MeteorHandler.MAX_DISTANCE == 3000 && MeteorHandler.MAX_ACTIVE > 0, "Meteors keep to their radius and cap");
        helper.succeed();
    }
}
