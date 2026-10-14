package dev.zsskayr.merlins_inferno.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * The Dark Coven - the legendary sword of Cain, a Circle 2 weapon crafted from Ebony. It exists as seven items, one per
 * stage (I to VII): each stage is the next step of the same blade, reached at a smithing table with one Ebony Bar and a
 * Withered Bone as the template - the last step (VI to VII) asks for a Nether Star instead of the bar. Smithing keeps
 * the enchantments.
 * <p>
 * <b>Right-click</b> invokes the coven (and, while it lasts, right-click again dismisses it). The Wither is the price, and
 * it is also the coven's life: Strength, Speed and Night Vision (and, at the later stages, more) last exactly as long as
 * the Wither does - {@value #BASE_SECONDS} seconds at stage I, one more second per stage. When the Wither goes - it ran
 * out, or milk or a totem took it - every other effect goes with it and the coven is dismissed. There is no cooldown:
 * invoke it again whenever you like. Putting the sword away dismisses it too.
 * <p>
 * <b>Visual cue:</b> invoking sets off a ring of dust that climbs with the stage - violet and thin at stage I, crimson,
 * dense and with soul flames at stage VII - and the action bar names the stage. The item's own texture also gains more
 * glowing runes along the blade with each stage, and the blade glows while the coven lasts.
 */
public class DarkCovenItem extends SwordItem {
    public static final int MAX_STAGE = 6;
    /** Seconds the coven lasts at stage I; every later stage adds one. */
    public static final int BASE_SECONDS = 7;
    private static final String INVOKED_TAG = "coven_invoked";
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII"};
    /** Dust colours per stage: violet at I, blood red at VII. */
    private static final float[][] COLORS = {
            {0.55F, 0.35F, 0.80F}, {0.62F, 0.32F, 0.80F}, {0.70F, 0.28F, 0.76F}, {0.78F, 0.24F, 0.68F},
            {0.85F, 0.20F, 0.55F}, {0.90F, 0.16F, 0.40F}, {0.80F, 0.04F, 0.10F}};

    /**
     * Wither amplifier per stage. With the durations above it costs roughly 3 health at stage I and about half of a full
     * health bar at stages IV to VII (the original's Wither XI was lethal in a second).
     */
    private static final int[] WITHER = {0, 0, 0, 1, 1, 1, 1};
    /** Strength and Speed amplifier per stage (the original's values: III up to X). */
    private static final int[] BOON = {2, 3, 4, 5, 6, 7, 9};
    /** Jump Boost amplifier per stage, or -1 for none. */
    private static final int[] JUMP = {-1, -1, -1, -1, -1, 3, 1};

    private final int stage;

    public DarkCovenItem(int stage, Properties properties) {
        super(ModTiers.LEGENDARY, properties);
        this.stage = stage;
    }

    public int getStage() {
        return this.stage;
    }

    /** How long this stage's coven lasts, in ticks. */
    public int getDurationTicks() {
        return (BASE_SECONDS + this.stage) * 20;
    }

    public static boolean isInvoked(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(INVOKED_TAG) && data.copyTag().getBoolean(INVOKED_TAG);
    }

    private static void setInvoked(ItemStack stack, boolean invoked) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag tag) -> {
            if (invoked) {
                tag.putBoolean(INVOKED_TAG, true);
            } else {
                tag.remove(INVOKED_TAG);
            }
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            if (isInvoked(stack)) {
                this.dismiss(stack, player);
                serverLevel.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.5F, 0.8F);
                player.displayClientMessage(Component.translatable("item.merlins_inferno.dark_coven.dismissed").withStyle(ChatFormatting.GRAY), true);
            } else {
                this.invoke(serverLevel, stack, player);
            }
        }
        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void invoke(ServerLevel level, ItemStack stack, Player player) {
        boolean pact = this.stage == MAX_STAGE;
        int duration = this.getDurationTicks();
        setInvoked(stack, true);
        // The Wither is the price and the coven's life; every other effect borrows its duration.
        player.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, WITHER[this.stage]));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, BOON[this.stage]));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, BOON[this.stage]));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false));
        if (JUMP[this.stage] >= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, JUMP[this.stage]));
        }
        if (pact) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1));
            player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, duration, 6));
        }

        // The visual cue: the dust ring climbs and thickens with the stage.
        float[] c = COLORS[this.stage];
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 1.0F + 0.15F * this.stage);
        int points = 12 + 6 * this.stage;
        double radius = 0.8 + 0.08 * this.stage;
        for (int i = 0; i < points; i++) {
            double angle = i * (Math.PI * 2.0 / points);
            double rise = (double) i / points * (1.0 + 0.15 * this.stage);
            level.sendParticles(dust, player.getX() + Math.cos(angle) * radius, player.getY() + 0.1 + rise * 1.8,
                    player.getZ() + Math.sin(angle) * radius, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.2, player.getZ(),
                2 + 3 * this.stage, 0.5, 0.1, 0.5, 0.02);
        if (this.stage >= 3) {
            level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.0, player.getZ(), this.stage * 2, 0.4, 0.8, 0.4, 0.04);
        }
        if (pact) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.9, 0.5, 0.03);
            level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6F, 0.7F);
        } else {
            level.playSound(null, player.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.5F,
                    Mth.clamp(0.7F + this.stage * 0.08F, 0.7F, 1.3F));
        }
        Component name = pact
                ? Component.translatable("item.merlins_inferno.dark_coven.pact").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                : Component.translatable("item.merlins_inferno.dark_coven.stage", ROMAN[this.stage], ROMAN[MAX_STAGE])
                        .withStyle(ChatFormatting.DARK_PURPLE);
        player.displayClientMessage(name, true);
    }

    /** Ends the coven: the flag goes and so do the boons it lent (the Wither, if still there, runs its course). */
    private void dismiss(ItemStack stack, Player player) {
        setInvoked(stack, false);
        this.removeBoon(player, MobEffects.DAMAGE_BOOST, BOON[this.stage]);
        this.removeBoon(player, MobEffects.MOVEMENT_SPEED, BOON[this.stage]);
        this.removeBoon(player, MobEffects.NIGHT_VISION, 0);
        if (JUMP[this.stage] >= 0) {
            this.removeBoon(player, MobEffects.JUMP, JUMP[this.stage]);
        }
        if (this.stage == MAX_STAGE) {
            this.removeBoon(player, MobEffects.DAMAGE_RESISTANCE, 1);
            this.removeBoon(player, MobEffects.UNLUCK, 6);
        }
    }

    /** Only the coven's own boon goes: an effect from another source (a stronger potion, say) is left alone. */
    private void removeBoon(Player player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance active = player.getEffect(effect);
        if (active != null && active.getAmplifier() == amplifier) {
            player.removeEffect(effect);
        }
    }

    /**
     * Watches over an invoked coven: no Wither (it ran out or was cured), or the sword put away, and it is dismissed.
     * The boons were given exactly the Wither's duration, so a natural end is just the Wither running out.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof Player player) || !isInvoked(stack)) {
            return;
        }
        boolean held = isSelected || player.getOffhandItem() == stack;
        if (!held || !player.hasEffect(MobEffects.WITHER)) {
            this.dismiss(stack, player);
            if (held && level instanceof ServerLevel) {
                player.displayClientMessage(Component.translatable("item.merlins_inferno.dark_coven.dismissed").withStyle(ChatFormatting.GRAY), true);
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isInvoked(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.merlins_inferno.dark_coven.stage", ROMAN[this.stage], ROMAN[MAX_STAGE])
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.merlins_inferno.dark_coven.tooltip.invoke", this.getDurationTicks() / 20).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.merlins_inferno.dark_coven.tooltip.cost").withStyle(ChatFormatting.DARK_RED));
        if (isInvoked(stack)) {
            tooltip.add(Component.translatable("item.merlins_inferno.dark_coven.tooltip.active").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (this.stage < MAX_STAGE - 1) {
            tooltip.add(Component.translatable("item.merlins_inferno.dark_coven.tooltip.upgrade").withStyle(ChatFormatting.DARK_GRAY));
        } else if (this.stage == MAX_STAGE - 1) {
            tooltip.add(Component.translatable("item.merlins_inferno.dark_coven.tooltip.upgrade_final").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("item.merlins_inferno.dark_coven.tooltip.awakened").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
