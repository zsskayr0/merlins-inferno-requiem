package dev.zsskayr.merlins_inferno.item;

import java.util.List;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.entity.HauntedBoat;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;

/** Places a {@link HauntedBoat} on water or lava, the same way vanilla boat items do. */
public class HauntedBoatItem extends Item {
    public HauntedBoatItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }
        Vec3 view = player.getViewVector(1.0F);
        List<Entity> nearby = level.getEntities(player, player.getBoundingBox().expandTowards(view.scale(5.0)).inflate(1.0),
                EntitySelector.NO_SPECTATORS.and(Entity::isPickable));
        Vec3 eye = player.getEyePosition();
        for (Entity entity : nearby) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
            if (box.contains(eye)) {
                return InteractionResultHolder.pass(stack);
            }
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        HauntedBoat boat = new HauntedBoat(ModEntityTypes.HAUNTED_BOAT.get(), level);
        boat.setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        boat.setYRot(player.getYRot());
        if (!level.noCollision(boat, boat.getBoundingBox())) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hit.getLocation());
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
