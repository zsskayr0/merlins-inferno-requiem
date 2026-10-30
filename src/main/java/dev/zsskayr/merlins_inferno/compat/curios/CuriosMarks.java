package dev.zsskayr.merlins_inferno.compat.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import dev.zsskayr.merlins_inferno.item.MarkItem;
import dev.zsskayr.merlins_inferno.mark.MarkHelper;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * The Curios side of Marks: the {@code mark} slot (data/merlins_inferno/curios) takes any item of the
 * {@code curios:mark} tag. Only loaded when Curios is present - Marks work without it (see {@link MarkHelper}).
 * A Mark in the slot is bound for good: it cannot be taken out, except by a creative player; death keeps it.
 */
public final class CuriosMarks {
    private CuriosMarks() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        for (var item : ModItems.ITEMS.getEntries()) {
            if (item.get() instanceof MarkItem mark) {
                event.registerItem(CuriosCapability.ITEM, (stack, context) -> new MarkCurio(stack, mark), mark);
            }
        }
    }

    /** Puts the Mark into the first free {@code mark} slot, if the player has one; {@code false} when there is none. */
    public static boolean place(Player player, ItemStack stack) {
        return CuriosApi.getCuriosInventory(player).map(inventory -> inventory.getStacksHandler("mark").map(handler -> {
            var slots = handler.getStacks();
            for (int i = 0; i < slots.getSlots(); i++) {
                if (slots.getStackInSlot(i).isEmpty()) {
                    slots.setStackInSlot(i, stack);
                    return true;
                }
            }
            return false;
        }).orElse(false)).orElse(false);
    }

    /** Removes the Mark item from the slot without asking it (a cure is not an unequip). */
    public static void removeEquipped(Player player) {
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            IItemHandlerModifiable slots = inventory.getEquippedCurios();
            for (int i = 0; i < slots.getSlots(); i++) {
                if (slots.getStackInSlot(i).getItem() instanceof MarkItem) {
                    slots.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        });
    }

    private record MarkCurio(ItemStack stack, MarkItem mark) implements ICurio {
        @Override
        public ItemStack getStack() {
            return stack;
        }

        @Override
        public boolean canEquip(SlotContext context) {
            // Fine for a player with no Mark, or whose Mark is this very one (placed by MarkHelper.bind, state already set).
            return !MarkHelper.hasMark(context.entity()) || MarkHelper.hasMark(context.entity(), mark);
        }

        @Override
        public void onEquip(SlotContext context, ItemStack previous) {
            if (context.entity() instanceof Player player) {
                MarkHelper.equip(player, mark);
            }
        }

        @Override
        public boolean canUnequip(SlotContext context) {
            LivingEntity entity = context.entity();
            return entity instanceof Player player && player.isCreative();
        }

        @Override
        public void onUnequip(SlotContext context, ItemStack newStack) {
            if (context.entity() instanceof Player player && player.isCreative() && MarkHelper.hasMark(player, mark)) {
                player.setData(dev.zsskayr.merlins_inferno.registry.ModAttachments.EQUIPPED_MARK, "");
            }
        }

        @Override
        public DropRule getDropRule(SlotContext context, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
            return DropRule.ALWAYS_KEEP;
        }
    }
}
