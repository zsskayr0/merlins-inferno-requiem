package dev.zsskayr.merlins_inferno.item;

import java.util.List;
import dev.zsskayr.merlins_inferno.compat.OptionalCurios;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Elytra-style wings that glide like an elytra (chest slot, or the Curios back slot) but are boosted by a keybind instead
 * of fireworks. A boost pulls along the look direction for a second, like a firework rocket, a little harder.
 * Gliding wears them down, faster or slower depending on the world; what they cost and how hard they pull is in their
 * {@link WingsProfile}.
 *
 * <p>Gliding from the Curios slot needs the three {@code getItemBySlot(CHEST)} reads of the vanilla glide checks to see
 * the wings: see {@link #glideStack}, used by the mixins.
 */
public final class WingsItem extends Item implements Equipable {
    /** A firework rocket pulls the player towards 1.5 times their look vector for about a second; the wings pull a bit harder. */
    private static final double BOOST_SPEED = 1.9;
    private static final int BOOST_TICKS = 30;
    /** Wings pull less when they hang on the back instead of the chest, and again when other heavy armour weighs on the wearer. */
    private static final double BACK_SLOT_FACTOR = 0.8;
    private static final double FOREIGN_ARMOR_FACTOR = 0.8;
    private final WingsProfile profile;

    public WingsItem(Properties properties, WingsProfile profile) {
        super(properties.stacksTo(1));
        this.profile = profile;
        DispenserBlock.registerBehavior(this, ArmorItem.DISPENSE_ITEM_BEHAVIOR);
    }

    public ResourceLocation texture() { return profile.texture(); }

    /** The wings the entity wears, chest slot first, then Curios; empty when it has none. */
    public static ItemStack find(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof WingsItem) return chest;
        for (ItemStack stack : OptionalCurios.equipped(entity)) if (stack.getItem() instanceof WingsItem) return stack;
        return ItemStack.EMPTY;
    }

    /** The wings to draw: like {@link #find}, but Curios wings hidden with the slot's eye toggle are skipped. */
    public static ItemStack findVisible(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof WingsItem) return chest;
        for (ItemStack stack : OptionalCurios.visible(entity)) if (stack.getItem() instanceof WingsItem) return stack;
        return ItemStack.EMPTY;
    }

    /** What the vanilla glide checks should treat as the elytra: the chest stack, unless Curios wings take its place. */
    public static ItemStack glideStack(LivingEntity entity, ItemStack chest) {
        if (chest.canElytraFly(entity)) return chest;
        ItemStack wings = find(entity);
        return wings.isEmpty() ? chest : wings;
    }

    /** Server side of the keybind: boosts a gliding player whose wings are ready. */
    public static void tryBoost(Player player) {
        ItemStack stack = find(player);
        if (player.level() instanceof ServerLevel level && stack.getItem() instanceof WingsItem wings
                && player.isFallFlying() && !player.getCooldowns().isOnCooldown(wings)) {
            wings.boost(player, level);
        }
    }

    private void boost(Player player, ServerLevel level) {
        player.setData(ModAttachments.WINGS_BOOST_TICKS, BOOST_TICKS);
        int cooldown = profile.cooldownIn(level);
        player.getCooldowns().addCooldown(this, cooldown);
        player.setData(ModAttachments.WINGS_READY_AT, level.getGameTime() + cooldown);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0F, 1.2F);
        tickBoost(player);
    }

    /**
     * On the ground the cooldown runs twice as fast: each tick takes one extra tick off, and the displayed cooldown is
     * re-set every few ticks to what is left (it is only re-sent that often, not every tick).
     */
    public static void tickGroundRecharge(Player player) {
        if (!player.onGround() || !(find(player).getItem() instanceof WingsItem wings)
                || !player.getCooldowns().isOnCooldown(wings)) return;
        long now = player.level().getGameTime();
        long readyAt = player.getData(ModAttachments.WINGS_READY_AT) - 1;
        if (readyAt <= now) {
            player.setData(ModAttachments.WINGS_READY_AT, now);
            player.getCooldowns().removeCooldown(wings);
        } else {
            player.setData(ModAttachments.WINGS_READY_AT, readyAt);
            if ((readyAt - now) % 5 == 0) player.getCooldowns().addCooldown(wings, (int) (readyAt - now));
        }
    }

    /** One tick of an ongoing boost, driven by the server player tick; a boost ends as soon as the player stops gliding. */
    public static void tickBoost(Player player) {
        int ticks = player.getData(ModAttachments.WINGS_BOOST_TICKS);
        if (ticks <= 0) return;
        if (!player.isFallFlying() || !(find(player).getItem() instanceof WingsItem wings)) {
            player.setData(ModAttachments.WINGS_BOOST_TICKS, 0);
            return;
        }
        player.setData(ModAttachments.WINGS_BOOST_TICKS, ticks - 1);
        double speed = wings.boostSpeed(player);
        Vec3 look = player.getLookAngle();
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.add(
                look.x * 0.1 + (look.x * speed - motion.x) * 0.5,
                look.y * 0.1 + (look.y * speed - motion.y) * 0.5,
                look.z * 0.1 + (look.z * speed - motion.z) * 0.5));
        player.hurtMarked = true;
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(wings.profile.trail(), player.getX(), player.getY() + 0.9, player.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
        }
    }

    /** How hard this boost pulls the wearer: the world they are in, the slot the wings hang in and the armour they wear. */
    public double boostSpeed(Player player) {
        double speed = BOOST_SPEED * profile.boost().in(player.level());
        if (!(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof WingsItem)) speed *= BACK_SLOT_FACTOR;
        return wearsForeignHeavyArmor(player) ? speed * FOREIGN_ARMOR_FACTOR : speed;
    }

    /** Armour stronger than iron, piece for piece, that is not of this route. */
    private boolean wearsForeignHeavyArmor(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack worn = player.getItemBySlot(slot);
            if (worn.getItem() instanceof ArmorItem armor && !worn.is(profile.route())
                    && armor.getDefense() > ArmorMaterials.IRON.value().getDefense(armor.getType())) return true;
        }
        return false;
    }

    /** Durability lost over the seconds of a glide: every full second, scaled by the world, rounded so it averages out. */
    private int wearThisTick(LivingEntity entity, int flightTicks) {
        if ((flightTicks + 1) % 20 != 0) return 0;
        double factor = profile.wear().in(entity.level());
        int second = (flightTicks + 1) / 20;
        return (int) (Math.floor(second * factor) - Math.floor((second - 1) * factor));
    }

    @Override public boolean canElytraFly(ItemStack stack, LivingEntity entity) { return stack.getDamageValue() < stack.getMaxDamage() - 1; }
    @Override public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!entity.level().isClientSide) {
            int wear = wearThisTick(entity, flightTicks);
            if (wear > 0) stack.hurtAndBreak(wear, entity, EquipmentSlot.CHEST);
        }
        return true;
    }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack material) {
        return profile.repair() != null && material.is(profile.repair().get());
    }
    /** Wings with no repair material cannot be mended at all, not by an anvil nor by combining two of them. */
    @Override public boolean isRepairable(ItemStack stack) { return profile.repair() != null && super.isRepairable(stack); }
    @Override public EquipmentSlot getEquipmentSlot() { return EquipmentSlot.CHEST; }
    @Override public Holder<SoundEvent> getEquipSound() { return SoundEvents.ARMOR_EQUIP_ELYTRA; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.merlins_inferno.wings.boost", Component.keybind("key.merlins_inferno.wings_boost")).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.merlins_inferno.wings.cooldown", profile.cooldownTicks() / 20).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.merlins_inferno.wings.ground_recharge").withStyle(ChatFormatting.DARK_GRAY));
        if (profile.recharge().oblivion() != 1.0) {
            tooltip.add(Component.translatable("item.merlins_inferno.wings.cooldown_oblivion",
                    Math.round(profile.cooldownTicks() * profile.recharge().oblivion() / 20.0)).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
