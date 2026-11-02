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

/** Circle 2's Celestial route: the Skybound set bonus, the Star Seeker and the fallen stars. */
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

    private static double swingDamage(ItemStack stack) {
        double total = 1.0; // the hand
        for (var entry : stack.getAttributeModifiers().modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
                total += entry.modifier().amount();
            }
        }
        return total;
    }

    @GameTest(template = "bench_empty")
    public static void circleTwoGearClimbsFromDragonsteelToTheCoven(GameTestHelper helper) {
        var types = new net.minecraft.world.item.ArmorItem.Type[] {net.minecraft.world.item.ArmorItem.Type.HELMET, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                net.minecraft.world.item.ArmorItem.Type.LEGGINGS, net.minecraft.world.item.ArmorItem.Type.BOOTS};
        var luminite = dev.zsskayr.merlins_inferno.registry.ModArmorMaterials.LUMINITE.value();
        var ebony = dev.zsskayr.merlins_inferno.registry.ModArmorMaterials.EBONY.value();
        int luminiteTotal = 0;
        int ebonyTotal = 0;
        for (var type : types) {
            luminiteTotal += luminite.getDefense(type);
            ebonyTotal += ebony.getDefense(type);
        }
        // Ice and Fire's dragonsteel: 34 defense, toughness 6, enchantability 30, 8000 uses and a sword that hits for 28.
        helper.assertTrue(luminiteTotal >= 34 && luminite.toughness() >= 6.0F && luminite.enchantmentValue() >= 30, "Luminite armor is at least dragonsteel's");
        helper.assertTrue(ebonyTotal > luminiteTotal && ebony.toughness() >= luminite.toughness() && ebony.knockbackResistance() > luminite.knockbackResistance(),
                "Ebony armor is above Luminite's");
        helper.assertTrue(ModItems.LUMINITE_CHESTPLATE.get().getMaxDamage(new ItemStack(ModItems.LUMINITE_CHESTPLATE.get())) >= 2560, "Luminite lasts as long as dragonsteel");
        helper.assertTrue(ModItems.EBONY_CHESTPLATE.get().getMaxDamage(new ItemStack(ModItems.EBONY_CHESTPLATE.get()))
                > ModItems.LUMINITE_CHESTPLATE.get().getMaxDamage(new ItemStack(ModItems.LUMINITE_CHESTPLATE.get())), "and Ebony lasts longer");
        helper.assertTrue(dev.zsskayr.merlins_inferno.item.ModTiers.LUMINITE.getUses() >= 8000 && dev.zsskayr.merlins_inferno.item.ModTiers.EBONY.getUses() > dev.zsskayr.merlins_inferno.item.ModTiers.LUMINITE.getUses(),
                "The tools too");

        double luminiteSword = swingDamage(new ItemStack(ModItems.LUMINITE_SWORD.get()));
        double ebonySword = swingDamage(new ItemStack(ModItems.EBONY_SWORD.get()));
        helper.assertTrue(luminiteSword == 28.0 && ebonySword > luminiteSword, "Luminite hits for 28, Ebony harder: " + luminiteSword + " / " + ebonySword);
        var coven = new net.minecraft.world.item.Item[] {ModItems.DARK_COVEN_I.get(), ModItems.DARK_COVEN_II.get(), ModItems.DARK_COVEN_III.get(), ModItems.DARK_COVEN_IV.get(),
                ModItems.DARK_COVEN_V.get(), ModItems.DARK_COVEN_VI.get(), ModItems.DARK_COVEN_VII.get()};
        double last = ebonySword;
        for (var stage : coven) {
            double now = swingDamage(new ItemStack(stage));
            helper.assertTrue(now > last, "Every stage of the Dark Coven hits harder than the one before it and than the Ebony Sword, " + now);
            last = now;
        }
        helper.assertTrue(last == 48.0, "and the last one hits for 48, was " + last);
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void skyboundIsLightnessNotArmor(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        dress(player);
        double before = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        SkyboundHandler.refresh(player);
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(speed >= before && speed <= before * (1.0 + SkyboundHandler.SPEED_PEAK) + 1.0E-6, "Skybound adds at most its peak speed, was " + speed + " from " + before);
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0.0 && player.getAttributeValue(Attributes.ARMOR) == 0.0, "and no armor of its own");

        // Three pieces are not a set: the bonus goes.
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        SkyboundHandler.refresh(player);
        helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == before, "Without the boots there is no bonus");
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void routeGearCarriesItsEvilAndHoly(GameTestHelper helper) {
        helper.assertTrue(dev.zsskayr.merlins_inferno.event.InnateEnchantments.holyLevel(new ItemStack(ModItems.LUMINITE_SWORD.get())) == 2, "Luminite carries Holy II");
        helper.assertTrue(dev.zsskayr.merlins_inferno.event.InnateEnchantments.holyLevel(new ItemStack(ModItems.SERAPHIUM_SWORD.get())) == 1, "Seraphium carries Holy I");
        helper.assertTrue(dev.zsskayr.merlins_inferno.event.InnateEnchantments.evilLevel(new ItemStack(ModItems.DEMONBLOOD_SWORD.get())) == 1, "Demonblood carries Evil I");
        helper.assertTrue(dev.zsskayr.merlins_inferno.event.InnateEnchantments.evilLevel(new ItemStack(ModItems.EBONY_SWORD.get())) == 2, "Ebony carries Evil II");
        helper.assertTrue(dev.zsskayr.merlins_inferno.event.InnateEnchantments.evilLevel(new ItemStack(ModItems.DARK_COVEN_I.get())) == 3
                && dev.zsskayr.merlins_inferno.event.InnateEnchantments.evilLevel(new ItemStack(ModItems.DARK_COVEN_VII.get())) == 5, "The Dark Coven climbs from Evil III to V");
        helper.assertTrue(dev.zsskayr.merlins_inferno.event.InnateEnchantments.holyLevel(new ItemStack(ModItems.EBONY_SWORD.get())) == 0
                && dev.zsskayr.merlins_inferno.event.InnateEnchantments.evilLevel(new ItemStack(ModItems.LUMINITE_SWORD.get())) == 0, "and the routes do not cross");
        helper.assertTrue(new ItemStack(ModItems.LUMINITE_SWORD.get()).is(dev.zsskayr.merlins_inferno.registry.ModTags.Items.CELESTIAL_ITEMS), "Luminite is celestial");
        helper.assertTrue(new ItemStack(ModItems.EBONY_SWORD.get()).is(dev.zsskayr.merlins_inferno.registry.ModTags.Items.DEMONIC_ITEMS), "and Ebony is demonic");
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
    public static void starSeekerFollowsTheMeteors(GameTestHelper helper) {
        var level = helper.getLevel();
        MeteorData data = MeteorData.get(level.getServer().overworld());
        BlockPos here = helper.absolutePos(new BlockPos(0, 2, 0));
        var near = data.add(here.getX() + 400, here.getZ(), 0L, 70);
        var far = data.add(here.getX() - 2000, here.getZ(), 0L, 70);
        try {
            var compass = new ItemStack(ModItems.STAR_SEEKER.get());
            dev.zsskayr.merlins_inferno.item.StarSeekerItem.retarget(compass, level, here);
            var tracker = compass.get(DataComponents.LODESTONE_TRACKER);
            helper.assertTrue(tracker != null && tracker.target().isPresent() && tracker.target().get().pos().getX() == here.getX() + 400,
                    "It points at the nearest meteor");
            helper.assertTrue(near != far, "Two distinct meteors");
        } finally {
            data.remove(near);
            data.remove(far);
        }
        var empty = new ItemStack(ModItems.STAR_SEEKER.get());
        dev.zsskayr.merlins_inferno.item.StarSeekerItem.retarget(empty, level, here);
        helper.assertTrue(data.all().isEmpty() ? empty.get(DataComponents.LODESTONE_TRACKER) == null : true, "With no meteor there is no target");
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void seekersAreWornAsCharms(GameTestHelper helper) throws ReflectiveOperationException {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) {
            helper.succeed();
            return;
        }
        var api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
        var slotsOf = api.getMethod("getItemStackSlots", ItemStack.class, boolean.class);
        helper.assertTrue(((java.util.Map<?, ?>) slotsOf.invoke(null, new ItemStack(ModItems.STAR_SEEKER.get()), false)).keySet().equals(java.util.Set.of("charm")), "The Star Seeker fits only a charm slot");
        helper.assertTrue(((java.util.Map<?, ?>) slotsOf.invoke(null, new ItemStack(ModItems.CHAMPION_SEEKER.get()), false)).keySet().equals(java.util.Set.of("charm")), "The Champion Seeker fits only a charm slot");
        helper.assertTrue(((java.util.Map<?, ?>) slotsOf.invoke(null, new ItemStack(ModItems.LAVA_LENS.get()), false)).keySet().equals(java.util.Set.of("head")), "Hell Sight fits only a head slot");

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        Object handler = ((java.util.Optional<?>) api.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class).invoke(null, player)).orElseThrow();
        var seeker = new ItemStack(ModItems.STAR_SEEKER.get());
        Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler")
                .getMethod("setEquippedCurio", String.class, int.class, ItemStack.class).invoke(handler, "charm", 0, seeker);
        helper.assertTrue(dev.zsskayr.merlins_inferno.compat.OptionalCurios.visible(player).stream().anyMatch(stack -> stack == seeker), "The HUD finds the worn seeker");

        MeteorData data = MeteorData.get(helper.getLevel().getServer().overworld());
        var meteor = data.add(player.getBlockX() + 400, player.getBlockZ(), 0L, 70);
        try {
            // Curios ticks a worn stack with slot -1; the seeker has to keep its target from there.
            player.tickCount = 1;
            seeker.inventoryTick(helper.getLevel(), player, -1, false);
            helper.assertTrue(seeker.get(DataComponents.LODESTONE_TRACKER) != null, "A worn seeker still finds the meteor");
        } finally {
            data.remove(meteor);
        }
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void meteorSiteBuildsACraterWithOreAndLoot(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 2, 0));
        for (int cx = -4; cx <= 4; cx++) {
            for (int cz = -4; cz <= 4; cz++) {
                level.getChunk(origin.offset(cx * 16, 0, cz * 16));
            }
        }
        helper.assertTrue(MeteorSite.canBuild(level, origin.getX(), origin.getZ()), "The test chunks are loaded");
        int floor = MeteorSite.build(level, origin.getX(), origin.getZ(), level.random).y();
        boolean ore = false;
        boolean chest = false;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-10, -24, -10), origin.offset(10, 12, 10))) {
            var state = level.getBlockState(pos);
            ore |= state.is(ModBlocks.LUMINITE_CLUSTER.get());
            if (state.is(Blocks.CHEST) && level.getBlockEntity(pos) instanceof ChestBlockEntity chestEntity) {
                chest = true;
            }
        }
        helper.assertTrue(ore, "The heart geode grows Luminite crystals");
        helper.assertTrue(chest, "The sky-drop chest sits in the geode's hollow");
        helper.assertTrue(floor > level.getMinBuildHeight(), "The floor height is reported");
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void luminiteDataLoads(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (String recipe : new String[] {"luminite_sword", "luminite_axe", "luminite_pickaxe", "luminite_helmet", "luminite_boots", "star_seeker",
                "luminite_leggings", "luminite_chestplate", "luminite_shovel", "luminite_hoe"}) {
            helper.assertTrue(server.getRecipeManager().byKey(id(recipe)).isPresent(), "Recipe " + recipe);
        }
        for (String table : new String[] {"chests/meteor", "chests/meteor_deep", "blocks/luminite_cluster"}) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id(table));
            helper.assertTrue(server.reloadableRegistries().getLootTable(key) != LootTable.EMPTY, "Loot table " + table);
        }
        helper.assertTrue(MeteorHandler.MAX_DISTANCE == 3000 && MeteorHandler.MAX_ACTIVE > 0, "Meteors keep to their radius and cap");
        helper.succeed();
    }
}
