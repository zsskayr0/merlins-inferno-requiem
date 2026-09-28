package dev.zsskayr.merlins_inferno.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
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
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Extra essence drops for kills by a player: Infernal Essence from Nether mobs (Circle 2 on) and Mundane Essence from
 * the Overworld's vanilla hostile mobs. These live here rather than in loot tables because the Circle is per player.
 * The Circle 2 doubling of loot-table essences is {@link dev.zsskayr.merlins_inferno.loot.DoubleEssenceModifier};
 * drops added here are already past that point, so they are doubled directly.
 */
public final class EssenceDropHandler {
    private static final float DROP_CHANCE = 0.3F;

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Player player) || !(victim.level() instanceof ServerLevel level)
                || !(victim instanceof Mob) || level.random.nextFloat() >= DROP_CHANCE) {
            return;
        }
        boolean circleTwo = ProgressionHelper.hasReached(player, ProgressionHelper.SECOND_CIRCLE);
        Item essence = null;
        if (level.dimension() == Level.NETHER) {
            if (circleTwo) {
                essence = ModItems.INFERNAL_ESSENCE.get();
            }
        } else if (level.dimension() == Level.OVERWORLD && victim.getType().getCategory() == MobCategory.MONSTER
                && "minecraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).getNamespace())) {
            essence = ModItems.MUNDANE_ESSENCE.get();
        }
        if (essence != null) {
            ItemStack stack = new ItemStack(essence, circleTwo ? 2 : 1);
            event.getDrops().add(new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), stack));
        }
    }
}
