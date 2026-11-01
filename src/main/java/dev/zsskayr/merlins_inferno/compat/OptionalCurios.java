package dev.zsskayr.merlins_inferno.compat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
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
    private static Method visibleInventory, visibleCurios, visibleStacks, visibleRenders;
    private static boolean visibleResolved;
    /** Client render asks per player per frame; the answer only has to be as fresh as the tick. */
    private static final Map<LivingEntity, VisibleMemo> VISIBLE_MEMO = Collections.synchronizedMap(new WeakHashMap<>());
    private record VisibleMemo(long tick, List<ItemStack> stacks) {}
    private OptionalCurios() {}
    /** Like {@link #equipped}, but skips the stacks whose Curios visibility (eye) toggle is switched off. */
    public static List<ItemStack> visible(LivingEntity entity) {
        if (!ModList.get().isLoaded("curios")) return List.of();
        boolean client = entity.level().isClientSide;
        long tick = entity.level().getGameTime();
        if (client) {
            VisibleMemo memo = VISIBLE_MEMO.get(entity);
            if (memo != null && memo.tick == tick) return memo.stacks;
        }
        try {
            if (!visibleResolved) {
                visibleResolved = true;
                visibleInventory = Class.forName("top.theillusivec4.curios.api.CuriosApi").getMethod("getCuriosInventory", LivingEntity.class);
                visibleCurios = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler").getMethod("getCurios");
                Class<?> stacksClass = Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler");
                visibleStacks = stacksClass.getMethod("getStacks");
                visibleRenders = stacksClass.getMethod("getRenders");
            }
            Optional<?> capability = (Optional<?>)visibleInventory.invoke(null, entity);
            if (capability.isEmpty()) return List.of();
            Object handlers = visibleCurios.invoke(capability.get());
            List<ItemStack> result = new ArrayList<>();
            for (Object stacks : ((Map<?, ?>)handlers).values()) {
                IItemHandler handler = (IItemHandler)visibleStacks.invoke(stacks);
                List<?> renders = (List<?>)visibleRenders.invoke(stacks);
                for (int i = 0; i < handler.getSlots(); i++) {
                    if (i < renders.size() && Boolean.FALSE.equals(renders.get(i))) continue;
                    result.add(handler.getStackInSlot(i));
                }
            }
            if (client) VISIBLE_MEMO.put(entity, new VisibleMemo(tick, result));
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
