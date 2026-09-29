package dev.zsskayr.merlins_inferno.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.entity.OstaraEntity;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Rare Monster Leather from Circle 2 on: from anything in {@code #minecraft:undead} and, a bit less rarely, from Ostara.
 * Lives here rather than in loot tables because the Circle is per player.
 */
public final class MonsterLeatherDropHandler {
    private static final float UNDEAD_CHANCE = 0.04F;
    private static final float OSTARA_CHANCE = 0.25F;

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Player player) || !(victim.level() instanceof ServerLevel level)
                || !ProgressionHelper.hasReached(player, ProgressionHelper.SECOND_CIRCLE)) {
            return;
        }
        float chance;
        if (victim instanceof OstaraEntity) {
            chance = OSTARA_CHANCE;
        } else if (victim.getType().is(EntityTypeTags.UNDEAD)) {
            chance = UNDEAD_CHANCE;
        } else {
            return;
        }
        if (level.random.nextFloat() < chance) {
            event.getDrops().add(new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), new ItemStack(ModItems.MONSTER_LEATHER.get())));
        }
    }
}
