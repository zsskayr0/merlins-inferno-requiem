package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.compat.OptionalCurios;
import dev.zsskayr.merlins_inferno.event.LavaLensHandler;
import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class LavaLensGameTests {
    @GameTest(template="bench_empty")
    public static void optionalCuriosEquippedMode(GameTestHelper h) throws ReflectiveOperationException {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) { h.succeed(); return; }
        // A plain mock avoids a fake network login with no Curios payload handshake.
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var api=Class.forName("top.theillusivec4.curios.api.CuriosApi");
        var type=Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
        var inventory=(java.util.Optional<?>)api.getMethod("getCuriosInventory",net.minecraft.world.entity.LivingEntity.class).invoke(null,player);
        h.assertTrue(inventory.isPresent(),"Curios player inventory capability is present");
        Object handler=inventory.orElseThrow();
        ItemStack lens=new ItemStack(ModItems.LAVA_LENS.get());
        type.getMethod("setEquippedCurio",String.class,int.class,ItemStack.class).invoke(handler,"charm",0,lens);
        h.assertTrue(OptionalCurios.equipped(player).stream().anyMatch(stack -> stack==lens),"Optional bridge sees the actual equipped stack");
        player.getPersistentData().putBoolean(LavaLensHandler.ENABLED,true);
        for(int i=0;i<20;i++)LavaLensHandler.refresh(player,true);
        h.assertTrue(player.hasEffect(ModEffects.LAVA_SIGHT) && lens.getDamageValue()==1,"Curios-only lens powers the effect and loses durability");
        type.getMethod("setEquippedCurio",String.class,int.class,ItemStack.class).invoke(handler,"charm",0,ItemStack.EMPTY);
        LavaLensHandler.refresh(player,true);
        h.assertTrue(!player.hasEffect(ModEffects.LAVA_SIGHT),"Unequipping disables the effect");h.succeed();
    }
    @GameTest(template="bench_empty")
    public static void toggleCarryWearAndRemoval(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack lens=new ItemStack(ModItems.LAVA_LENS.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,lens);
        h.assertTrue(!player.hasEffect(ModEffects.LAVA_SIGHT),"Off by default");
        ModItems.LAVA_LENS.get().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertTrue(player.hasEffect(ModEffects.LAVA_SIGHT),"Right click enables lava sight");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        player.getInventory().setItem(12,lens);
        for(int i=0;i<20;i++)LavaLensHandler.refresh(player,true);
        h.assertTrue(lens.getDamageValue()==1,"One durability per active second in inventory");
        h.assertTrue(player.hasEffect(ModEffects.LAVA_SIGHT),"Moving to inventory preserves activation");
        h.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION) && !player.hasEffect(MobEffects.FIRE_RESISTANCE),"Lens does not grant unrelated vision or fire protection");
        player.getInventory().setItem(12,ItemStack.EMPTY);
        LavaLensHandler.refresh(player,true);
        h.assertTrue(!player.hasEffect(ModEffects.LAVA_SIGHT),"Removing the lens immediately disables the effect");h.succeed();
    }
    @GameTest(template="bench_empty")
    public static void breakStopsEffectAndCopiesDoNotDoubleCharge(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack first=new ItemStack(ModItems.LAVA_LENS.get()),second=new ItemStack(ModItems.LAVA_LENS.get());
        player.getInventory().setItem(0,first);player.getInventory().setItem(1,second);
        ModItems.LAVA_LENS.get().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        for(int i=0;i<20;i++)LavaLensHandler.refresh(player,true);
        h.assertTrue(first.getDamageValue()==1 && second.getDamageValue()==0,"Only one carried copy pays durability");
        player.getInventory().setItem(1,ItemStack.EMPTY);first.setDamageValue(first.getMaxDamage()-1);
        for(int i=0;i<20;i++)LavaLensHandler.refresh(player,true);
        h.assertTrue(first.isEmpty() && !player.hasEffect(ModEffects.LAVA_SIGHT),"Breaking ends the effect");h.succeed();
    }
}
