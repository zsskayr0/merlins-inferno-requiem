package dev.zsskayr.merlins_inferno.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * A compass forged around a captured wisp of Mundane and Fae essence: its needle points at the
 * nearest boss/miniboss (see {@link ModTags.EntityTypes#BOSS_COMPASS_TARGETS}) instead of world
 * spawn or a lodestone. The needle is a baked "angle" item property (16 frame textures under
 * {@code textures/item/champion_seeker/}) driven by a {@code ClampedItemPropertyFunction}
 * registered client-side in {@code client.ModEntityRenderers}.
 */
public class ChampionSeekerItem extends Item {
    public ChampionSeekerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.merlins_inferno.champion_seeker.tooltip").withStyle(ChatFormatting.GRAY));
    }

    /** Nearest tagged boss to {@code holder} within {@code searchRadius} blocks, or {@code null} if none is loaded nearby. */
    public static LivingEntity findNearestBoss(LivingEntity holder, double searchRadius) {
        Level level = holder.level();
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                holder.getBoundingBox().inflate(searchRadius),
                e -> e.isAlive() && e.getType().is(ModTags.EntityTypes.BOSS_COMPASS_TARGETS));

        LivingEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double distSq = candidate.distanceToSqr(holder);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = candidate;
            }
        }
        return nearest;
    }

    /** Needle angle in [0, 1) for the "angle" item property - 0 is "pointing towards the target". */
    public static float getAngle(LivingEntity holder, Vec3 target) {
        double dx = target.x - holder.getX();
        double dz = target.z - holder.getZ();
        double directionTerm = 0.5D - (Mth.atan2(dz, dx) / (Math.PI * 2));
        return (float) Mth.positiveModulo(directionTerm - holder.getYRot() / 360.0D, 1.0D);
    }
}
