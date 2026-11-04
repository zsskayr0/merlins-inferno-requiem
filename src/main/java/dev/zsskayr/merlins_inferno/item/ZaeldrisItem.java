package dev.zsskayr.merlins_inferno.item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import dev.zsskayr.merlins_inferno.entity.ZaeldrisSpear;
import dev.zsskayr.merlins_inferno.network.ZaeldrisPosePayload;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;

/**
 * Zaeldris - the angelic lance of Circle 3 and the strongest weapon in the mod. A left click is an ordinary (if enormous) hit.
 * Holding the barrage key (right mouse by default, rebindable; see {@code client.ZaeldrisKeys}) looses a spear of light every
 * {@value #FIRE_INTERVAL} ticks, free of any cost, at the foe nearest the crosshair within {@value #RANGE} blocks and a
 * {@value #CONE_DEGREES}-degree cone - or straight ahead when there is none. Each spear is summoned above and behind the
 * wielder and hangs there {@value #HOVER_TICKS} ticks before it flies. The first spear of a press comes out at once, so even a
 * quick tap looses one. See {@link ZaeldrisSpear} for what a spear does.
 * <p>
 * The client only reports the key going down and up ({@code network.ZaeldrisBarragePayload}); the server keeps the rhythm and
 * stops on its own the moment the lance leaves the main hand.
 */
public class ZaeldrisItem extends SwordItem {
    public static final int FIRE_INTERVAL = 4;
    public static final double RANGE = 32.0;
    public static final double CONE_DEGREES = 40.0;
    /** Far past every other weapon in the mod (the MYTHIC tier's 35, Luminite's 30, gold's 22): the best rolls at the table. */
    public static final int ENCHANTABILITY = 60;
    /** Ticks a summoned spear hangs behind the wielder before it is loosed. */
    public static final int HOVER_TICKS = 5;
    private static final Set<UUID> BARRAGING = new HashSet<>();
    /** Game time each wielder may next fire at; a press fires at once when the last shot is that far behind. */
    private static final Map<UUID, Long> NEXT_SHOT = new HashMap<>();

    public ZaeldrisItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public int getEnchantmentValue() {
        return ENCHANTABILITY;
    }

    /** The key went down (fires at once if ready, so a quick tap always looses one spear) or up. */
    public static void setBarrage(ServerPlayer player, boolean held) {
        boolean changed = held ? BARRAGING.add(player.getUUID()) : BARRAGING.remove(player.getUUID());
        if (changed) {
            // Everyone who sees the wielder draws them holding the lance back like a trident (client.ZaeldrisPose).
            PacketDistributor.sendToPlayersTrackingEntity(player, new ZaeldrisPosePayload(player.getId(), held));
        }
        if (held) {
            tryFire(player);
        }
    }

    public static boolean isBarraging(UUID player) {
        return BARRAGING.contains(player);
    }

    public static void forget(UUID player) {
        BARRAGING.remove(player);
        NEXT_SHOT.remove(player);
    }

    /** Called every server tick for every player (event.ZaeldrisHandler). */
    public static void tickBarrage(ServerPlayer player) {
        if (BARRAGING.contains(player.getUUID())) {
            tryFire(player);
        }
    }

    private static void tryFire(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive() || !(player.getMainHandItem().getItem() instanceof ZaeldrisItem)) {
            return;
        }
        long now = player.level().getGameTime();
        Long next = NEXT_SHOT.get(player.getUUID());
        if (next == null || now >= next || next - now > FIRE_INTERVAL) {
            NEXT_SHOT.put(player.getUUID(), now + FIRE_INTERVAL);
            fire(player);
        }
    }

    private static void fire(ServerPlayer player) {
        ZaeldrisSpear spear = new ZaeldrisSpear(ModEntityTypes.ZAELDRIS_SPEAR.get(), player.level());
        spear.setOwner(player);
        // The spear carries the lance as it is and the wielder's whole attack (Strength, the lance's own attributes and any
        // affix or gear bonus to attack damage), read now, the way an arrow carries its bow.
        spear.arm(player.getMainHandItem().copy(), (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        // Summoned on a halo above and behind the shoulders, somewhere along its arc, so the volley fans out like a crown of
        // light; the spear hangs there for HOVER_TICKS (riding along with the wielder) and is then loosed (see ZaeldrisSpear).
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 back = new Vec3(Mth.sin(yaw), 0.0, -Mth.cos(yaw));
        Vec3 side = new Vec3(Mth.cos(yaw), 0.0, Mth.sin(yaw));
        double arc = (player.getRandom().nextDouble() - 0.5) * Math.PI * 0.85;
        Vec3 offset = new Vec3(0.0, player.getEyeHeight() + 0.35 + Math.cos(arc) * 0.75, 0.0)
                .add(side.scale(Math.sin(arc) * 1.5)).add(back.scale(0.6));
        Vec3 from = player.position().add(offset);
        spear.setPos(from);
        spear.summon(offset, HOVER_TICKS);

        Entity target = findTarget(player, spear);
        spear.setHomingTarget(target);
        spear.face(target == null ? player.getLookAngle() : target.getBoundingBox().getCenter().subtract(from).normalize());
        player.level().addFreshEntity(spear);
        player.level().playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F,
                1.6F + player.getRandom().nextFloat() * 0.3F);
    }

    /** The foe closest to the crosshair - hostile, hunting the wielder, or an End Crystal - in sight, in range and in the cone. */
    private static Entity findTarget(ServerPlayer player, ZaeldrisSpear spear) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double minDot = Math.cos(Math.toRadians(CONE_DEGREES));
        Entity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity candidate : player.level().getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(RANGE),
                candidate -> candidate instanceof EndCrystal || candidate instanceof LivingEntity living && spear.isFoe(living)
                        && (living instanceof Enemy || living instanceof Mob mob && mob.getTarget() == player))) {
            Vec3 to = candidate.getBoundingBox().getCenter().subtract(eye);
            double distance = to.length();
            if (distance > RANGE || distance < 1.0E-3) {
                continue;
            }
            double dot = to.scale(1.0 / distance).dot(look);
            if (dot < minDot || !player.hasLineOfSight(candidate)) {
                continue;
            }
            // Mostly by angle; distance only breaks near-ties.
            double score = (1.0 - dot) * 40.0 + distance * 0.05;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.merlins_inferno.zaeldris.tooltip.barrage",
                Component.keybind("key.merlins_inferno.zaeldris_barrage")).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.merlins_inferno.zaeldris.tooltip.lodge").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.merlins_inferno.zaeldris.tooltip.burst").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.merlins_inferno.zaeldris.tooltip.god_touch").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.merlins_inferno.zaeldris.tooltip.light").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.merlins_inferno.zaeldris.tooltip.yours").withStyle(ChatFormatting.GRAY));
    }
}
