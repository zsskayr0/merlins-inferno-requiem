package dev.zsskayr.merlins_inferno.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.registry.ModEnchantments;

/**
 * Reaping is a Circle 2 enchantment, so it only turns up as loot: with {@code chance}, a Reaping book (level I-III) is
 * added to the tables this modifier's conditions select, and only when the player opening the chest has reached
 * Circle 2.
 */
public class ReapingBookModifier extends LootModifier {
    public static final MapCodec<ReapingBookModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
            .and(com.mojang.serialization.Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(m -> m.chance))
            .apply(inst, ReapingBookModifier::new));

    private final float chance;

    public ReapingBookModifier(LootItemCondition[] conditions, float chance) {
        super(conditions);
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        Player player = DoubleEssenceModifier.findPlayer(context);
        if (player == null || !ProgressionHelper.hasReached(player, ProgressionHelper.SECOND_CIRCLE)
                || context.getRandom().nextFloat() >= chance) {
            return loot;
        }
        Holder<Enchantment> reaping = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.REAPING).orElse(null);
        if (reaping != null) {
            loot.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(reaping, 1 + context.getRandom().nextInt(3))));
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
