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

import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import dev.zsskayr.merlins_inferno.registry.ModEffects;

/**
 * The Dark Coven - the mythic sword of Cain (not craftable; he drops it very rarely), a Circle 2 weapon far above the ordinary Ebony Sword. It exists as seven items, one per
 * stage (I to VII): each stage is the next step of the same blade, reached at a smithing table with one Ebony Bar and a
 * Withered Bone as the template - the last step (VI to VII) asks for a Nether Star instead of the bar. Smithing keeps
 * the enchantments.
 * <p>
 * <b>Holding right-click</b> keeps the coven going; a second after the button is let go, it ends. <b>Sneak + right-click</b>
 * invokes it for a fixed time instead - {@value #BASE_SECONDS} seconds at stage I, {@value #SECONDS_PER_STAGE} more per
 * stage, 50 at stage VI - and sneak + right-click again dismisses it. Once a coven ends there is a
 * {@value #COOLDOWN_SECONDS}-second cooldown. The Wither is the price, and it is also the coven's life: Strength, Speed and Night
 * Vision (and, at the later stages, more) last exactly as long as the Wither does. When the Wither goes - it ran out, or
 * milk or a totem took it - every other effect goes with it and the coven is dismissed. Putting the sword away dismisses
 * it too.
 * <p>
 * The last stage (the Demonic Pact) is different: a plain right-click invokes it for {@value #PACT_SECONDS} seconds, with
 * no cooldown, and it keeps going with the sword put away - even swapped for another weapon - until the Wither runs out
 * or it is dismissed with sneak + right-click. No stage can be thrown out of the inventory (see DarkCovenKeepHandler). Night Vision is lent long enough never to flicker, and goes the moment the coven does.
 * <p>
 * Holding is read from the game's own right-click repeat (a held button uses the item every 4 ticks), so the sword is
 * never "in use": you keep moving, sprinting and swinging while it lasts.
 * <p>
 * <b>Visual cue:</b> invoking sets off a ring of dust that climbs with the stage - violet and thin at stage I, crimson,
 * dense and with soul flames at stage VII - and the action bar names the stage. The item's own texture also gains more
 * glowing runes along the blade with each stage, and the blade glows while the coven lasts.
 */
public class DarkCovenItem extends SwordItem {
    public static final int MAX_STAGE = 6;
    /** Seconds the sneak-invoked coven lasts at stage I; every later stage adds {@value #SECONDS_PER_STAGE}. */
    public static final int BASE_SECONDS = 20;
    public static final int SECONDS_PER_STAGE = 6;
    /** The Demonic Pact lasts this long. */
    public static final int PACT_SECONDS = 60;
    /** After a coven of stages I to VI ends. */
    public static final int COOLDOWN_SECONDS = 6;
    /** A held right-click repeats every 4 ticks; this long without one and the button is let go. */
    private static final int HOLD_GRACE_TICKS = 5;
    /** What a held coven lends at a time, topped up while the button stays down. */
    private static final int HOLD_LENT_TICKS = 60;
    private static final int HOLD_TOP_UP_BELOW = 30;
    /** What is left of the coven once the button is let go: one second. */
    private static final int RELEASE_TICKS = 20;
    /** Night Vision flickers in its last ten seconds, so it is never lent for less than this (it is removed with the coven). */
    private static final int NIGHT_VISION_MIN_TICKS = 300;
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

    /** Wither level (0 = I) of the Pact at a stage. */
    public static int witherLevel(int stage) {
        return WITHER[stage];
    }

    /** Strength and Speed amplifier (0 = level I) at a stage. */
    public static int boon(int stage) {
        return BOON[stage];
    }

    /** Jump Boost amplifier at a stage, or -1 for none. */
    public static int jump(int stage) {
        return JUMP[stage];
    }
    /** Strength and Speed amplifier per stage (the original's values: III up to X). */
    private static final int[] BOON = {2, 3, 4, 5, 6, 7, 9};
    /** Jump Boost amplifier per stage, or -1 for none. */
    private static final int[] JUMP = {-1, -1, -1, -1, -1, 3, 1};

    private final int stage;

    public DarkCovenItem(int stage, Properties properties) {
        super(ModTiers.MYTHIC, properties);
        this.stage = stage;
    }

    public int getStage() {
        return this.stage;
    }

    /** How long this stage's coven lasts, in ticks. */
    public int getDurationTicks() {
        return (this.stage == MAX_STAGE ? PACT_SECONDS : BASE_SECONDS + SECONDS_PER_STAGE * this.stage) * 20;
    }

    public static boolean isInvoked(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(INVOKED_TAG) && data.copyTag().getBoolean(INVOKED_TAG);
    }

    public static void setInvoked(ItemStack stack, boolean invoked) {
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
            long now = serverLevel.getGameTime();
            // A held button re-uses the item every 4 ticks: only a use after a pause is a fresh press.
            boolean fresh = now - player.getData(ModAttachments.COVEN_LAST_USE) > HOLD_GRACE_TICKS;
            player.setData(ModAttachments.COVEN_LAST_USE, now);
            boolean invoked = isInvoked(stack);
            boolean hold = player.getData(ModAttachments.COVEN_HOLD_UNTIL) >= 0;
            if (this.stage == MAX_STAGE) {
                // The pact needs no button held and no sword in hand: one click, a minute.
                if (fresh) {
                    if (invoked && player.isShiftKeyDown()) {
                        this.dismiss(stack, player);
                        serverLevel.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
                        serverLevel.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.5F, 0.8F);
                        player.displayClientMessage(Component.translatable("item.merlins_inferno.dark_coven.dismissed").withStyle(ChatFormatting.GRAY), true);
                    } else if (!invoked) {
                        this.invoke(serverLevel, stack, player, this.getDurationTicks());
                        player.swing(hand, true);
                    }
                }
            } else if (player.isShiftKeyDown()) {
                if (fresh) {
                    if (invoked && !hold) {
                        this.dismiss(stack, player);
                        serverLevel.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
                        serverLevel.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.5F, 0.8F);
                        player.displayClientMessage(Component.translatable("item.merlins_inferno.dark_coven.dismissed").withStyle(ChatFormatting.GRAY), true);
                    } else {
                        // The fixed-time coven: no button to keep down.
                        player.setData(ModAttachments.COVEN_HOLD_UNTIL, -1L);
                        if (invoked) {
                            this.lend(player, this.getDurationTicks(), true);
                        } else {
                            this.invoke(serverLevel, stack, player, this.getDurationTicks());
                        }
                        player.swing(hand, true);
                    }
                }
            } else if (!invoked) {
                player.setData(ModAttachments.COVEN_HOLD_UNTIL, now + HOLD_GRACE_TICKS);
                this.invoke(serverLevel, stack, player, HOLD_LENT_TICKS);
                player.swing(hand, true);
            } else if (hold) {
                player.setData(ModAttachments.COVEN_HOLD_UNTIL, now + HOLD_GRACE_TICKS);
                MobEffectInstance pact = player.getEffect(ModEffects.DEMONIC_PACT);
                if (pact == null || pact.getDuration() < HOLD_TOP_UP_BELOW) {
                    this.lend(player, HOLD_LENT_TICKS, false);
                }
            }
        }
        // PASS, not CONSUME: a consumed use resets the first-person "item used" bob, and a held button would make the
        // sword dip every 4 ticks. The server does its own work above; the swing on invoking is sent from there.
        return InteractionResultHolder.pass(stack);
    }

    /**
     * Lends the coven for {@code ticks}: the Demonic Pact, which carries the boons and the Wither as one effect, and Night
     * Vision (a screen effect that cannot be an attribute). {@code replace} drops them first, so a shorter time takes hold
     * too (effects only ever keep the longer one otherwise).
     */
    private void lend(Player player, int ticks, boolean replace) {
        if (replace) {
            player.removeEffect(ModEffects.DEMONIC_PACT);
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
        player.addEffect(new MobEffectInstance(ModEffects.DEMONIC_PACT, ticks, this.stage, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Math.max(ticks, NIGHT_VISION_MIN_TICKS), 0, false, false, false));
    }

    private void invoke(ServerLevel level, ItemStack stack, Player player, int ticks) {
        boolean pact = this.stage == MAX_STAGE;
        setInvoked(stack, true);
        this.lend(player, ticks, false);

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

    /** Ends the coven: the flag goes and so do the effects it lent, the Wither included. */
    private void dismiss(ItemStack stack, Player player) {
        setInvoked(stack, false);
        if (this.stage != MAX_STAGE) {
            player.getCooldowns().addCooldown(this, COOLDOWN_SECONDS * 20);
        }
        player.setData(ModAttachments.COVEN_HOLD_UNTIL, -1L);
        this.removeBoon(player, ModEffects.DEMONIC_PACT, this.stage);
        this.removeBoon(player, MobEffects.NIGHT_VISION, 0);
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
        if (level instanceof ServerLevel serverLevel && entity instanceof Player swinger && swinger.swinging
                && (isSelected || swinger.getOffhandItem() == stack)) {
            this.swingTrail(serverLevel, swinger, isInvoked(stack));
        }
        if (level.isClientSide || !(entity instanceof Player player) || !isInvoked(stack)) {
            return;
        }
        boolean held = isSelected || player.getOffhandItem() == stack;
        boolean pact = this.stage == MAX_STAGE;
        long holdUntil = player.getData(ModAttachments.COVEN_HOLD_UNTIL);
        if (!pact && held && holdUntil >= 0 && level.getGameTime() > holdUntil) {
            // The button was let go: one more second, then the Wither runs out and takes the coven with it.
            player.setData(ModAttachments.COVEN_HOLD_UNTIL, -1L);
            this.lend(player, RELEASE_TICKS, true);
        }
        // The pact outlives the sword in hand: only its effect ending (or sneak + right-click) puts it out.
        if ((!held && !pact) || !player.hasEffect(ModEffects.DEMONIC_PACT)) {
            this.dismiss(stack, player);
            if (held && level instanceof ServerLevel) {
                player.displayClientMessage(Component.translatable("item.merlins_inferno.dark_coven.dismissed").withStyle(ChatFormatting.GRAY), true);
            }
        }
    }

    /** A red ribbon along the blade's arc while it swings; the invoked coven adds soul fire to it. */
    private void swingTrail(ServerLevel level, Player player, boolean invoked) {
        float progress = Mth.clamp(player.swingTime / 6.0F, 0.0F, 1.0F);
        double sweep = Math.toRadians(player.getYRot() + 70.0 - 140.0 * progress);
        double pitch = Math.toRadians(-player.getXRot());
        double eyeY = player.getEyeY() - 0.35;
        DustParticleOptions blood = new DustParticleOptions(new Vector3f(0.75F, 0.02F, 0.06F), 1.3F);
        for (int i = 1; i <= 4; i++) {
            double reach = 0.8 + i * 0.4;
            double x = player.getX() - Math.sin(sweep) * reach * Math.cos(pitch);
            double z = player.getZ() + Math.cos(sweep) * reach * Math.cos(pitch);
            double y = eyeY + Math.sin(pitch) * reach;
            level.sendParticles(blood, x, y, z, 1, 0.03, 0.03, 0.03, 0.0);
            if (invoked && i % 2 == 0) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target, net.minecraft.world.entity.LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel level) {
            level.sendParticles(new DustParticleOptions(new Vector3f(0.75F, 0.02F, 0.06F), 1.2F),
                    target.getX(), target.getY(0.6), target.getZ(), 10, 0.25, 0.3, 0.25, 0.0);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isInvoked(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.merlins_inferno.dark_coven.tooltip.mythic").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.merlins_inferno.dark_coven.stage", ROMAN[this.stage], ROMAN[MAX_STAGE])
                .withStyle(ChatFormatting.DARK_PURPLE));
        if (this.stage == MAX_STAGE) {
            tooltip.add(Component.translatable("item.merlins_inferno.dark_coven.tooltip.invoke_pact", PACT_SECONDS).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.merlins_inferno.dark_coven.tooltip.invoke", this.getDurationTicks() / 20).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.merlins_inferno.dark_coven.tooltip.cooldown", COOLDOWN_SECONDS).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.merlins_inferno.dark_coven.tooltip.no_drop").withStyle(ChatFormatting.DARK_GRAY));
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
