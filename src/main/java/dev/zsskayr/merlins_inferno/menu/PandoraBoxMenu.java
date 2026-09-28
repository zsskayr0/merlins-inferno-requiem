package dev.zsskayr.merlins_inferno.menu;

import java.util.function.Predicate;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import dev.zsskayr.merlins_inferno.registry.ModMenuTypes;

/**
 * The Pandora Box's screen: three slots around a central one. The three boss key items (Book of Contracts, Eve's
 * Secret, Flame of God) go around, a Nether Star in the middle; the Perform Ritual button (menu button
 * {@link #BUTTON_RITUAL}) then swallows them, opens Circle 2 and - outside Hardcore - kills the player. Once
 * Circle 2 is open the same screen forges the Oblivion and Purgatory keys ({@link #BUTTON_OBLIVION_KEY},
 * {@link #BUTTON_PURGATORY_KEY}).
 * <p>
 * All logic runs in {@link #clickMenuButton} on the server; the client only sees the slots and one synced value
 * (the player's Circle, via {@link ContainerData}).
 */
public class PandoraBoxMenu extends AbstractContainerMenu {
    public static final int BUTTON_RITUAL = 0;
    public static final int BUTTON_OBLIVION_KEY = 1;
    public static final int BUTTON_PURGATORY_KEY = 2;

    public static final int SLOT_BOOK = 0, SLOT_EVE = 1, SLOT_FLAME = 2, SLOT_STAR = 3;
    private static final int KEY_SLOTS = 4;
    private static final int INVENTORY_START = KEY_SLOTS;
    private static final int INVENTORY_END = INVENTORY_START + 36;

    /** Slot positions (top-left of each 16x16 slot) relative to the screen: three in a triangle, the star in the middle. */
    public static final int[][] SLOT_POS = {{80, 10}, {47, 67}, {113, 67}, {80, 46}};
    public static final int PLAYER_INV_Y = 140;

    private final Container container = new SimpleContainer(KEY_SLOTS);
    private final ContainerData data;
    private final Player player;

    /**
     * Both sides use this constructor (it's what {@code MenuType<PandoraBoxMenu>} calls to rebuild the menu from
     * the open packet on the client, and what {@code PandoraBoxItem} calls directly on the server): the ritual
     * slots start empty either way, and {@code data} reads the opener's own Circle live (server) or as last
     * synced (client) - one value, so a plain {@code SimpleContainerData} works on both sides.
     */
    public PandoraBoxMenu(int containerId, Inventory inventory) {
        super(ModMenuTypes.PANDORA_BOX.get(), containerId);
        this.player = inventory.player;
        this.data = new SimpleContainerData(1) {
            @Override
            public int get(int index) {
                return ProgressionHelper.circle(PandoraBoxMenu.this.player);
            }
        };
        // Circle 1: the ritual's four offerings. Circle 2: the same slots take the Key of Oblivion's ingredients -
        // Otherworld Essence on top, Infernal Essence bottom-left, Celestial Essence bottom-right, a Void Block in the middle.
        this.addSlot(new KeySlot(0, stack -> stack.is(ModItems.BOOK_OF_CONTRACTS.get()), stack -> stack.is(ModItems.OTHERWORLD_ESSENCE.get())));
        this.addSlot(new KeySlot(1, stack -> stack.is(ModItems.EVES_SECRET.get()), stack -> stack.is(ModItems.INFERNAL_ESSENCE.get())));
        this.addSlot(new KeySlot(2, stack -> stack.is(ModItems.FLAME_OF_GOD.get()), stack -> stack.is(ModItems.CELESTIAL_ESSENCE.get())));
        this.addSlot(new KeySlot(3, stack -> stack.is(Items.NETHER_STAR), stack -> stack.is(ModBlocks.VOID_BLOCK.get().asItem())));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, PLAYER_INV_Y + 58));
        }
        this.addDataSlots(data);
    }

    /** The opener's Circle (server: live; client: as last synced). */
    public int getCircle() {
        return this.data.get(0);
    }

    /** True when all four slots hold what the ritual needs. */
    public boolean isRitualReady() {
        return this.slots.get(SLOT_BOOK).hasItem() && this.slots.get(SLOT_EVE).hasItem()
                && this.slots.get(SLOT_FLAME).hasItem() && this.slots.get(SLOT_STAR).hasItem();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        switch (id) {
            case BUTTON_RITUAL:
                return this.performRitual(serverPlayer);
            case BUTTON_OBLIVION_KEY:
                return this.forgeOblivionKey(serverPlayer);
            case BUTTON_PURGATORY_KEY:
                return this.forgeKey(serverPlayer, ModItems.PURGATORY_KEY.get(),
                        new Ingredient(Items.BLAZE_ROD, 1), new Ingredient(ModItems.DEMON_BLOOD.get(), 2));
            default:
                return false;
        }
    }

    private boolean performRitual(ServerPlayer player) {
        if (ProgressionHelper.circle(player) >= ProgressionHelper.SECOND_CIRCLE) {
            player.displayClientMessage(Component.translatable("message.merlins_inferno.pandora_box.already_open"), true);
            return false;
        }
        if (!this.isRitualReady()) {
            player.displayClientMessage(Component.translatable("message.merlins_inferno.pandora_box.incomplete"), true);
            return false;
        }
        this.container.clearContent(); // the centre swallows the rest
        ProgressionHelper.setCircle(player, ProgressionHelper.SECOND_CIRCLE);
        player.level().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.0F, 0.6F);
        boolean spared = player.level().getLevelData().isHardcore() || player.isCreative();
        player.displayClientMessage(Component.translatable(spared
                ? "message.merlins_inferno.pandora_box.opened_spared" : "message.merlins_inferno.pandora_box.opened"), false);
        if (!spared) {
            player.closeContainer();
            player.kill(); // the box stays with them - see event.PandoraHandler
        }
        return true;
    }

    private record Ingredient(Item item, int count) {
    }

    /** Forges the Key of Oblivion from the four slots (Circle 2 only): each must hold its ingredient, and all are consumed. */
    private boolean forgeOblivionKey(ServerPlayer player) {
        if (ProgressionHelper.circle(player) < ProgressionHelper.SECOND_CIRCLE) {
            return false;
        }
        if (!this.slots.get(SLOT_BOOK).hasItem() || !this.slots.get(SLOT_EVE).hasItem()
                || !this.slots.get(SLOT_FLAME).hasItem() || !this.slots.get(SLOT_STAR).hasItem()) {
            player.displayClientMessage(Component.translatable("message.merlins_inferno.pandora_box.key_missing"), true);
            return false;
        }
        for (int i = 0; i < KEY_SLOTS; i++) {
            this.container.setItem(i, ItemStack.EMPTY);
        }
        ItemStack result = new ItemStack(ModItems.OBLIVION_KEY.get());
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    /** Forges a key from items in the player's inventory (Circle 2 only). */
    private boolean forgeKey(ServerPlayer player, Item key, Ingredient... ingredients) {
        if (ProgressionHelper.circle(player) < ProgressionHelper.SECOND_CIRCLE) {
            return false;
        }
        Inventory inventory = player.getInventory();
        for (Ingredient ingredient : ingredients) {
            if (countItem(inventory, ingredient.item()) < ingredient.count()) {
                player.displayClientMessage(Component.translatable("message.merlins_inferno.pandora_box.key_missing"), true);
                return false;
            }
        }
        for (Ingredient ingredient : ingredients) {
            removeItem(inventory, ingredient.item(), ingredient.count());
        }
        ItemStack result = new ItemStack(key);
        if (!inventory.add(result)) {
            player.drop(result, false);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    private static int countItem(Inventory inventory, Item item) {
        int total = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void removeItem(Inventory inventory, Item item, int count) {
        for (ItemStack stack : inventory.items) {
            if (count <= 0) {
                return;
            }
            if (stack.is(item)) {
                int taken = Math.min(count, stack.getCount());
                stack.shrink(taken);
                count -= taken;
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < KEY_SLOTS) {
                if (!this.moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, KEY_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.PANDORA_BOX.get()));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.container); // whatever was not swallowed goes back
    }

    /** One of the four ritual slots: takes only its item, one at a time, and only while Circle 2 is still closed. */
    private class KeySlot extends Slot {
        private final Predicate<ItemStack> ritualItem;
        private final Predicate<ItemStack> keyIngredient;

        KeySlot(int index, Predicate<ItemStack> ritualItem, Predicate<ItemStack> keyIngredient) {
            super(PandoraBoxMenu.this.container, index, SLOT_POS[index][0], SLOT_POS[index][1]);
            this.ritualItem = ritualItem;
            this.keyIngredient = keyIngredient;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return (PandoraBoxMenu.this.getCircle() < ProgressionHelper.SECOND_CIRCLE ? this.ritualItem : this.keyIngredient).test(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
