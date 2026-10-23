package dev.zsskayr.merlins_inferno.compat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import dev.zsskayr.merlins_inferno.Merlins_inferno;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;

/** Optional 1.21.1 Curios API bridge; the standalone inventory item needs no additional dependency. */
public final class OptionalCurios {
    private static Method inventory;
    private static Method equipped;
    private static boolean resolved;
    private OptionalCurios() {}
    /** Like {@link #equipped}, but skips the stacks whose Curios visibility (eye) toggle is switched off. */
    public static List<ItemStack> visible(LivingEntity entity) {
        if (!ModList.get().isLoaded("curios")) return List.of();
        try {
            Optional<?> capability = (Optional<?>)Class.forName("top.theillusivec4.curios.api.CuriosApi")
                    .getMethod("getCuriosInventory", LivingEntity.class).invoke(null, entity);
            if (capability.isEmpty()) return List.of();
            Object handlers = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler")
                    .getMethod("getCurios").invoke(capability.get());
            Class<?> stacksClass = Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler");
            Method getStacks = stacksClass.getMethod("getStacks");
            Method getRenders = stacksClass.getMethod("getRenders");
            List<ItemStack> result = new ArrayList<>();
            for (Object stacks : ((java.util.Map<?, ?>)handlers).values()) {
                IItemHandler handler = (IItemHandler)getStacks.invoke(stacks);
                List<?> renders = (List<?>)getRenders.invoke(stacks);
                for (int i = 0; i < handler.getSlots(); i++) {
                    if (i < renders.size() && Boolean.FALSE.equals(renders.get(i))) continue;
                    result.add(handler.getStackInSlot(i));
                }
            }
            return result;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException exception) {
            org.slf4j.LoggerFactory.getLogger(OptionalCurios.class).warn("Curios visibility bridge unavailable", exception);
            return List.of();
        }
    }

    public static List<ItemStack> equipped(LivingEntity entity) {
        if (!ModList.get().isLoaded("curios")) return List.of();
        try {
            if (!resolved) {
                resolved = true;
                inventory = Class.forName("top.theillusivec4.curios.api.CuriosApi").getMethod("getCuriosInventory", LivingEntity.class);
                equipped = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler").getMethod("getEquippedCurios");
            }
            if (inventory == null) return List.of();
            Optional<?> capability = (Optional<?>)inventory.invoke(null, entity);
            if (capability.isEmpty()) return List.of();
            IItemHandler handler = (IItemHandler)equipped.invoke(capability.get());
            List<ItemStack> result = new ArrayList<>();
            for (int i=0;i<handler.getSlots();i++) result.add(handler.getStackInSlot(i));
            return result;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException exception) {
            if (inventory != null) org.slf4j.LoggerFactory.getLogger(OptionalCurios.class).warn("Curios lava-lens bridge unavailable; inventory mode remains available", exception);
            inventory = null;
            return List.of();
        }
    }
}
