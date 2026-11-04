package dev.zsskayr.merlins_inferno.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.DarkCovenItem;
import dev.zsskayr.merlins_inferno.registry.ModEffects;

/**
 * The Dark Coven's one effect. Its level is the stage, and it carries everything the coven used to lend as separate
 * potion effects (which showed up as a row of icons): Speed as an attribute modifier, Strength as one that only holds
 * while a Dark Coven is in hand (see syncStrength), Jump Boost as jump
 * strength and safe fall distance, the Pact's bad luck, and the Wither as a damage tick of its own (same damage type, so
 * a Mark of Cain still shrugs it off). The Pact's Resistance lives in {@code event.DemonicPactHandler}. Because it is one
 * effect, milk, a totem or running out ends all of it at once, and the modifiers go with it.
 */
public class DemonicPactEffect extends MobEffect {
    private static final ResourceLocation STRENGTH = id("demonic_pact_strength");
    private static final ResourceLocation SPEED = id("demonic_pact_speed");
    private static final ResourceLocation JUMP = id("demonic_pact_jump");
    private static final ResourceLocation FALL = id("demonic_pact_fall");
    private static final ResourceLocation LUCK = id("demonic_pact_luck");

    public DemonicPactEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, path);
    }

    private static void put(AttributeMap map, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        var instance = map.getInstance(attribute);
        if (instance != null) {
            instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void drop(AttributeMap map, Holder<Attribute> attribute, ResourceLocation id) {
        var instance = map.getInstance(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    /**
     * The Strength half of the coven belongs to the blade: +3 damage a level, and only while a Dark Coven is in the main
     * hand, so a Pact kept going with the sword put away no longer lends its damage to another weapon. Called every tick
     * (event.DemonicPactHandler); everything else the effect carries stays on for as long as the effect does.
     */
    public static void syncStrength(LivingEntity entity) {
        var instance = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (instance == null) {
            return;
        }
        var pact = entity.getEffect(ModEffects.DEMONIC_PACT);
        if (pact != null && entity.getMainHandItem().getItem() instanceof DarkCovenItem) {
            double amount = 3.0 * (DarkCovenItem.strength(pact.getAmplifier()) + 1);
            var current = instance.getModifier(STRENGTH);
            if (current == null || current.amount() != amount) {
                instance.addOrReplacePermanentModifier(new AttributeModifier(STRENGTH, amount, AttributeModifier.Operation.ADD_VALUE));
            }
        } else if (instance.hasModifier(STRENGTH)) {
            instance.removeModifier(STRENGTH);
        }
    }

    @Override
    public void addAttributeModifiers(AttributeMap map, int stage) {
        int boon = DarkCovenItem.boon(stage) + 1;
        // Speed: +20% a level - what the potion effect gave. Strength is synced per tick, see syncStrength.
        put(map, Attributes.MOVEMENT_SPEED, SPEED, 0.2 * boon, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        int jump = DarkCovenItem.jump(stage);
        if (jump >= 0) {
            put(map, Attributes.JUMP_STRENGTH, JUMP, 0.1 * (jump + 1), AttributeModifier.Operation.ADD_VALUE);
            put(map, Attributes.SAFE_FALL_DISTANCE, FALL, jump + 1, AttributeModifier.Operation.ADD_VALUE);
        }
        if (stage == DarkCovenItem.MAX_STAGE) {
            put(map, Attributes.LUCK, LUCK, -7.0, AttributeModifier.Operation.ADD_VALUE);
        }
    }

    @Override
    public void removeAttributeModifiers(AttributeMap map) {
        drop(map, Attributes.ATTACK_DAMAGE, STRENGTH);
        drop(map, Attributes.MOVEMENT_SPEED, SPEED);
        drop(map, Attributes.JUMP_STRENGTH, JUMP);
        drop(map, Attributes.SAFE_FALL_DISTANCE, FALL);
        drop(map, Attributes.LUCK, LUCK);
    }

    /** The Wither's pace: every 40 ticks, twice as often per level. */
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int stage) {
        int interval = 40 >> DarkCovenItem.witherLevel(stage);
        return interval <= 0 || duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int stage) {
        entity.hurt(entity.damageSources().wither(), 1.0F);
        return true;
    }
}
