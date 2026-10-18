package dev.zsskayr.merlins_inferno.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.entity.GrymnEntity;
import dev.zsskayr.merlins_inferno.registry.ModEnchantments;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Extra essence drops for kills by a player (plus the Grymn's Circle 2 Otherworld Essence): Infernal Essence from Nether mobs (Circle 2 on) and Mundane Essence from
 * the Overworld's vanilla hostile mobs (undead drop Otherworld Essence instead, from Circle 2 on; demons never drop Mundane). These live here rather than in loot tables because the Circle is per player.
 * The Circle 2 doubling of loot-table essences is {@link dev.zsskayr.merlins_inferno.loot.DoubleEssenceModifier};
 * drops added here are already past that point, so they are doubled directly.
 */
public final class EssenceDropHandler {
    private static final float DROP_CHANCE = 0.3F;

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Player player) || !(victim.level() instanceof ServerLevel level)
                || !(victim instanceof Mob)) {
            return;
        }
        if (victim instanceof GrymnEntity) {
            // The Grymn is the source of Otherworld Essence, guaranteed, from Circle 2 on. It also takes part in
            // the random Nether roll below like any other Nether mob (on top of its loot-table Infernal Essence).
            if (ProgressionHelper.hasReached(player, ProgressionHelper.SECOND_CIRCLE)) {
                ItemStack stack = new ItemStack(ModItems.OTHERWORLD_ESSENCE.get(), (1 + level.random.nextInt(2)) * 2);
                event.getDrops().add(new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), stack));
            }
        }
        boolean circleTwo = ProgressionHelper.hasReached(player, ProgressionHelper.SECOND_CIRCLE);
        // Reaping is a Circle 2 enchantment: it does nothing for a player who has not reached it.
        int reaping = circleTwo ? ModEnchantments.getReapingLevel(level, player.getMainHandItem()) : 0;
        if (reaping <= 0 && level.random.nextFloat() >= DROP_CHANCE) {
            return;
        }
        Item essence = null;
        boolean eligible = false;
        if (level.dimension() == Level.NETHER) {
            eligible = true;
            if (circleTwo) {
                essence = ModItems.INFERNAL_ESSENCE.get();
            }
        } else if (level.dimension() == Level.OVERWORLD && victim.getType().getCategory() == MobCategory.MONSTER
                && "minecraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).getNamespace())) {
            if (victim.getType().is(EntityTypeTags.UNDEAD)) {
                eligible = true;
                if (circleTwo) {
                    essence = ModItems.OTHERWORLD_ESSENCE.get();
                }
            } else if (!victim.getType().is(ModTags.EntityTypes.DEMON)) {
                eligible = true;
                essence = ModItems.MUNDANE_ESSENCE.get();
            }
        }
        int count = circleTwo ? 2 : 1;
        if (reaping > 0 && eligible) {
            // Reaping guarantees the drop: before Circle 2 the gated essences fall back to Mundane. The count is final
            // (1-2 at I, 2-4 at II, 3-6 at III), so the Circle 2 doubling does not stack on top.
            if (essence == null) {
                essence = ModItems.MUNDANE_ESSENCE.get();
            }
            count = reaping + level.random.nextInt(reaping + 1);
        }
        if (essence != null) {
            ItemStack stack = new ItemStack(essence, count);
            event.getDrops().add(new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), stack));
        }
    }
}
