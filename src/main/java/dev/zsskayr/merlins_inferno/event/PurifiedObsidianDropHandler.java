package dev.zsskayr.merlins_inferno.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.entity.EliasEntity;
import dev.zsskayr.merlins_inferno.entity.SacredCultistEntity;
import dev.zsskayr.merlins_inferno.entity.SacredPriestEntity;
import dev.zsskayr.merlins_inferno.entity.TemplarEntity;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * The Angelical route's mobs may leave Purified Obsidian, the frame of El'ysium's portal, for a player who has reached
 * Circle 2: Elias most often, the Sacred Cultist rarely. A per-player Circle gate, so it lives here and not in a loot table.
 */
public final class PurifiedObsidianDropHandler {
    private static final float ELIAS = 0.60F;
    private static final float SACRED_PRIEST = 0.30F;
    private static final float TEMPLAR = 0.08F;
    private static final float SACRED_CULTIST = 0.04F;

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Player player) || !(victim.level() instanceof ServerLevel level)
                || !ProgressionHelper.hasReached(player, ProgressionHelper.SECOND_CIRCLE)) {
            return;
        }
        float chance = victim instanceof EliasEntity ? ELIAS
                : victim instanceof SacredPriestEntity ? SACRED_PRIEST
                : victim instanceof TemplarEntity ? TEMPLAR
                : victim instanceof SacredCultistEntity ? SACRED_CULTIST : 0.0F;
        if (chance > 0.0F && level.random.nextFloat() < chance) {
            event.getDrops().add(new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), new ItemStack(ModItems.PURIFIED_OBSIDIAN_ITEM.get())));
        }
    }
}
