package dev.zsskayr.merlins_inferno.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import dev.zsskayr.merlins_inferno.registry.ModEnchantments;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * "Toque do Druida"'s sword effects (see the design doc, 3.3) - everything else the enchantment
 * does is pure data/block-override: the axe's Rowanwood gating (see
 * {@code block.RowanwoodLogBlock} and {@code data/merlins_inferno/loot_table/blocks/rowanwood_log.json}).
 * The pickaxe's ore bonus ({@link #onBlockDrops}) and "more XP/gold from any kill, more loot from magical mobs" need Java, since neither XP
 * amount nor a flat bonus drop across every mob is expressible as a loot-table condition.
 */
public final class DruidsTouchHandler {
    private static final float XP_MULTIPLIER = 1.5F;
    private static final float GOLD_DROP_CHANCE = 0.25F;
    private static final int MAGICAL_LOOT_MULTIPLIER = 2;
    /** Roughly what Fortune I adds to an ore, on top of whatever the block already dropped. */
    private static final float ORE_BONUS = 1.0F / 3.0F;

    @SubscribeEvent
    public void onExperienceDrop(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player == null || getLevel(player, player.getMainHandItem()) <= 0) {
            return;
        }
        event.setDroppedExperience(Math.round(event.getDroppedExperience() * XP_MULTIPLIER));
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (getLevel(player, player.getMainHandItem()) <= 0) {
            return;
        }

        LivingEntity victim = event.getEntity();

        if (victim.getType().is(ModTags.EntityTypes.MAGICAL_MOBS)) {
            for (ItemEntity drop : event.getDrops()) {
                drop.getItem().setCount(drop.getItem().getCount() * MAGICAL_LOOT_MULTIPLIER);
            }
        }

        if (level.random.nextFloat() < GOLD_DROP_CHANCE) {
            event.getDrops().add(new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), new ItemStack(Items.GOLD_NUGGET)));
        }
    }

    /**
     * Ores in {@link ModTags.Blocks#DRUIDS_TOUCH_ORES} drop about a third more. Works on the
     * finished drop list instead of overriding the ores' loot tables, so it stacks with Fortune and
     * never clashes with other mods touching those tables. Silk Touch (the ore itself dropping) is left alone.
     */
    @SubscribeEvent
    public void onBlockDrops(BlockDropsEvent event) {
        if (!event.getState().is(ModTags.Blocks.DRUIDS_TOUCH_ORES)
                || getLevel(event.getLevel(), event.getTool()) <= 0) {
            return;
        }
        Item self = event.getState().getBlock().asItem();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(self)) {
                return;
            }
        }
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            float extra = stack.getCount() * ORE_BONUS;
            int whole = (int) extra;
            if (event.getLevel().random.nextFloat() < extra - whole) {
                whole++;
            }
            stack.grow(whole);
        }
    }

    private static int getLevel(Level level, ItemStack tool) {
        return ModEnchantments.getDruidsTouchLevel(level, tool);
    }

    private static int getLevel(Player player, ItemStack weapon) {
        return ModEnchantments.getDruidsTouchLevel(player.level(), weapon);
    }
}
