package dev.zsskayr.merlins_inferno.event;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.GehennaItem;
import dev.zsskayr.merlins_inferno.network.GehennaStatePayload;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;

/**
 * Gehenna's Unique, Bloody (see item.GehennaItem). State lives on the bearer (ModAttachments.GEHENNA_*): blows left
 * of the "fed" boon, the tick the frenzy runs out, and the last damage dealt. The client gets it for the HUD clock.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class GehennaHandler {
    /** Killing in one blow: this many next blows hit harder. */
    public static final int FED_HITS = 3;
    public static final float FED_MULTIPLIER = 1.25F;
    /** A blow that does not kill: harder still, for this long, renewed by every further blow. */
    public static final int FRENZY_SECONDS = 6;
    public static final float FRENZY_MULTIPLIER = 1.5F;
    /** The frenzy only wakes on a bearer with at least this much health. */
    public static final int FRENZY_MIN_HEALTH = 13;
    /** A frenzy that ends without a kill: the bearer takes this share of the last damage they dealt. */
    public static final int RECOIL_PERCENT = 66;

    private static final ResourceKey<DamageType> RECOIL = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "gehenna"));
    /** Whether the target was unhurt when a Gehenna blow came in, read when the blow lands. */
    private static final Map<LivingEntity, Boolean> UNHURT = new WeakHashMap<>();

    private GehennaHandler() {
    }

    private static ServerPlayer wielder(DamageSource source) {
        return source.getEntity() instanceof ServerPlayer player && source.getDirectEntity() == player
                && player.getMainHandItem().getItem() instanceof GehennaItem ? player : null;
    }

    private static boolean frenzied(ServerPlayer player) {
        return player.getData(ModAttachments.GEHENNA_FRENZY_UNTIL) > player.level().getGameTime();
    }

    /** The boons, applied to the blow before armor. */
    @SubscribeEvent
    static void onIncoming(LivingIncomingDamageEvent event) {
        ServerPlayer player = wielder(event.getSource());
        if (player == null) {
            return;
        }
        LivingEntity target = event.getEntity();
        UNHURT.put(target, target.getHealth() >= target.getMaxHealth());
        int fed = player.getData(ModAttachments.GEHENNA_FED);
        if (fed > 0) {
            event.setAmount(event.getAmount() * FED_MULTIPLIER);
            player.setData(ModAttachments.GEHENNA_FED, fed - 1);
        } else if (frenzied(player)) {
            event.setAmount(event.getAmount() * FRENZY_MULTIPLIER);
        }
    }

    /** What the blow did: a one-blow kill feeds it, any other blow wakes or renews the frenzy. */
    @SubscribeEvent
    static void onDamaged(LivingDamageEvent.Post event) {
        ServerPlayer player = wielder(event.getSource());
        if (player == null) {
            return;
        }
        LivingEntity target = event.getEntity();
        boolean unhurt = Boolean.TRUE.equals(UNHURT.remove(target));
        if (target.isDeadOrDying()) {
            if (unhurt) {
                player.setData(ModAttachments.GEHENNA_FED, FED_HITS);
                endFrenzy(player);
                player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
            sync(player);
            return;
        }
        player.setData(ModAttachments.GEHENNA_LAST_DAMAGE, event.getNewDamage());
        boolean renewing = frenzied(player);
        if (renewing || (player.getData(ModAttachments.GEHENNA_FED) == 0 && player.getHealth() >= FRENZY_MIN_HEALTH)) {
            player.setData(ModAttachments.GEHENNA_FRENZY_UNTIL, player.level().getGameTime() + FRENZY_SECONDS * 20L);
            if (!renewing) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 0.7F);
            }
        }
        sync(player);
    }

    /** A kill ends the frenzy without its toll: a hostile mob, or, on a bearer of the Mark of Cain, any creature. */
    @SubscribeEvent
    static void onKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && frenzied(player)
                && (event.getEntity() instanceof Enemy || MarkOfCainHandler.bearsCain(player))) {
            endFrenzy(player);
            sync(player);
        }
    }

    /** The toll of a frenzy that ran out. */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // A player left with NaN health (or absorption) by some damage overflow can never die nor heal: put them right.
        if (Float.isNaN(player.getHealth()) || Float.isNaN(player.getAbsorptionAmount())) {
            player.setAbsorptionAmount(0.0F);
            player.setHealth(player.getMaxHealth());
        }
        long until = player.getData(ModAttachments.GEHENNA_FRENZY_UNTIL);
        if (until > 0 && player.level().getGameTime() >= until) {
            float toll = player.getData(ModAttachments.GEHENNA_LAST_DAMAGE) * RECOIL_PERCENT / 100.0F;
            endFrenzy(player);
            if (toll > 0.0F && player.isAlive()) {
                player.hurt(new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(RECOIL)), toll);
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            sync(player);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endFrenzy(player);
            player.setData(ModAttachments.GEHENNA_FED, 0);
            sync(player);
        }
    }

    private static void endFrenzy(ServerPlayer player) {
        player.setData(ModAttachments.GEHENNA_FRENZY_UNTIL, 0L);
    }

    private static void sync(ServerPlayer player) {
        long until = player.getData(ModAttachments.GEHENNA_FRENZY_UNTIL);
        int left = until > 0 ? (int) Math.max(0, until - player.level().getGameTime()) : 0;
        PacketDistributor.sendToPlayer(player, new GehennaStatePayload(player.getData(ModAttachments.GEHENNA_FED), left));
    }
}
