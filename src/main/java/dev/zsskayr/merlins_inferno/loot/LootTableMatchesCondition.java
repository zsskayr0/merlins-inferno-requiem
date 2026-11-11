package dev.zsskayr.merlins_inferno.loot;

import java.util.List;
import java.util.regex.Pattern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import dev.zsskayr.merlins_inferno.registry.ModLootConditions;

/**
 * Loot tables cannot be tagged, so compat with other mods' chests matches the queried table id against regexes
 * ("namespace:path", full match). Lets one modifier cover every mod that names its tables alike, without listing ids.
 */
public record LootTableMatchesCondition(List<String> patterns) implements LootItemCondition {
    public static final MapCodec<LootTableMatchesCondition> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.STRING.listOf().fieldOf("patterns").forGetter(LootTableMatchesCondition::patterns)
    ).apply(inst, LootTableMatchesCondition::new));

    public LootTableMatchesCondition {
        patterns = List.copyOf(patterns);
        for (String pattern : patterns) {
            Pattern.compile(pattern);
        }
    }

    @Override
    public boolean test(LootContext context) {
        ResourceLocation id = context.getQueriedLootTableId();
        if (id == null) {
            return false;
        }
        String text = id.toString();
        for (String pattern : patterns) {
            if (Pattern.matches(pattern, text)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public LootItemConditionType getType() {
        return ModLootConditions.TABLE_MATCHES.get();
    }
}
