package dev.zsskayr.merlins_inferno.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;

/**
 * From Circle 2 on, every essence a loot table hands out is doubled. The player is the killer, the container's
 * opener, or - for piglin bartering, which has no player in its context - whoever stands next to the piglin.
 */
public class DoubleEssenceModifier extends LootModifier {
    public static final MapCodec<DoubleEssenceModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, DoubleEssenceModifier::new));
    private static final double NEARBY_PLAYER_RANGE = 16.0;

    public DoubleEssenceModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        Player player = findPlayer(context);
        if (player == null || !ProgressionHelper.hasReached(player, ProgressionHelper.SECOND_CIRCLE)) {
            return loot;
        }
        ObjectArrayList<ItemStack> result = new ObjectArrayList<>(loot.size());
        for (ItemStack stack : loot) {
            if (!EssenceHelper.isEssence(stack)) {
                result.add(stack);
                continue;
            }
            int remaining = stack.getCount() * 2;
            int max = stack.getMaxStackSize();
            while (remaining > 0) {
                result.add(stack.copyWithCount(Math.min(remaining, max)));
                remaining -= max;
            }
        }
        return result;
    }

    private static Player findPlayer(LootContext context) {
        Player killer = context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
        if (killer != null) {
            return killer;
        }
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (entity instanceof Player player) {
            return player;
        }
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null && entity != null) {
            origin = entity.position();
        }
        return origin == null ? null : context.getLevel().getNearestPlayer(origin.x, origin.y, origin.z, NEARBY_PLAYER_RANGE, false);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
