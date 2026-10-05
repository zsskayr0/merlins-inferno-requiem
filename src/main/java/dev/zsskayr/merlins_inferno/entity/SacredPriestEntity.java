package dev.zsskayr.merlins_inferno.entity;

import java.util.EnumSet;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import dev.zsskayr.merlins_inferno.blockentity.SacredAltarBlockEntity;
import dev.zsskayr.merlins_inferno.entity.ai.ChurchServiceGoal;
import dev.zsskayr.merlins_inferno.event.LyriumVillagerTrades;

/**
 * The Sacred Priest - the Angelical path's Circle 1 miniboss and the guardian of the Sacred Church. A neutral
 * NPC, not a monster: he never attacks on his own and trades Refined Lyrium like the vanilla Cleric does (the same
 * offers, see {@link LyriumVillagerTrades#priestOffers}) - until he is struck, which turns him on the aggressor, his
 * cultists join him, and the boss bar appears for the length of the fight. He will not trade while kneeling in
 * vigil or while angry. Elias
 * cannot be woken at the altar until he has fallen. By day he wanders near the church; from dusk to dawn he takes his
 * place behind the altar, facing the congregation, and kneels in silent vigil with the broadsword planted before him
 * ({@link ChurchServiceGoal}) - until he is struck or warned, which ends it till the next dawn. Mid-fight he intones a
 * healing prayer every so often instead.
 * <ul>
 *     <li><b>Stats:</b> 150 health, 10 damage.</li>
 *     <li><b>Loot:</b> Lyrium Shards (data/.../loot_table/entities/sacred_priest.json).</li>
 * </ul>
 * Animated with GeckoLib ({@code geo/sacred_priest.geo.json}, {@code animations/sacred_priest.animation.json}); the
 * broadsword is baked into the model, not a held item.
 */
public class SacredPriestEntity extends PathfinderMob implements GeoEntity, ChurchServiceGoal.Attendee, Merchant {
    public static final double MAX_HEALTH = 150.0;
    public static final double ATTACK_DAMAGE = 10.0;
    public static final double MOVEMENT_SPEED = 0.23;

    private static final int PRAYER_INTERVAL = 160;
    private static final float PRAYER_HEAL = 6.0F;
    private static final int HOME_RADIUS = 24;
    /** Cultists this close rally to him when he is struck (see {@link #hurt}). */
    private static final double CULTIST_ALERT_RADIUS = 16.0;

    /** The death clip is 2.5 s; the body lingers a little past it so the last pose is held before it vanishes. */
    private static final int DEATH_ANIMATION_TICKS = 55;

    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation.sacred_priest.attack");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("animation.sacred_priest.death");
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.sacred_priest.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.sacred_priest.walk");
    private static final RawAnimation RUN_ANIMATION = RawAnimation.begin().thenLoop("animation.sacred_priest.run");
    /** Kneels down (pray_start), then holds the prayer. */
    private static final RawAnimation PRAY_ANIMATION = RawAnimation.begin().thenPlay("animation.sacred_priest.pray_start").thenLoop("animation.sacred_priest.pray");
    /** Vanilla's limb-swing amount (about 4 x blocks moved per tick, capped at 1) above which the legs are visibly working. */
    private static final float MOVING_LIMB_SWING = 0.08F;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int deathTicks;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.merlins_inferno.sacred_priest"),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

    @Nullable
    private BlockPos altarPos;
    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers offers;
    private long restockDay = -1;
    /** Where he stands during the night service - behind the altar, facing the nave - or null if he has no post. */
    @Nullable
    private Vec3 servicePost;
    private float serviceYaw;
    /** Struck or warned: he stays out of the service until dawn. */
    private boolean alarmed;

    public SacredPriestEntity(EntityType<? extends SacredPriestEntity> type, Level level) {
        super(type, level);
        this.xpReward = 30;
        this.setPersistenceRequired();
        this.bossEvent.setVisible(false); // neutral: the bar only shows while he is actually fighting
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    /** Ties him to a church: he stays near it, and its altar learns of his death. */
    public void setHome(BlockPos altar) {
        this.altarPos = altar.immutable();
        this.restrictTo(this.altarPos, HOME_RADIUS);
    }

    /** Gives him his place at the altar: where his feet go and the direction he faces while praying. */
    public void setServicePost(Vec3 post, float yaw) {
        this.servicePost = post;
        this.serviceYaw = yaw;
    }

    @Override
    @Nullable
    public Vec3 servicePost() {
        return this.servicePost;
    }

    @Override
    public float serviceYaw() {
        return this.serviceYaw;
    }

    @Override
    public boolean isAlarmed() {
        return this.alarmed;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (target != null) {
            this.alarmed = true;
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.alarmed && this.getTarget() == null && !ChurchServiceGoal.isServiceTime(this.level())) {
            this.alarmed = false; // a new day: peace again, ready for the next night's service
        }
    }

    /** A kneeling priest is not shoved away from the altar. */
    @Override
    public boolean isPushable() {
        return this.getPose() != Pose.CROUCHING && super.isPushable();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(3, new ChurchServiceGoal<>(this, 0.7));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Retaliation only, like his flock.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, SacredCultistEntity.class));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(this.getTarget() != null);
        long day = this.level().getDayTime() / 24000L;
        if (day != this.restockDay) {
            this.restockDay = day;
            if (this.offers != null) {
                this.offers.forEach(MerchantOffer::resetUses);
            }
        }
        if (this.isAlive() && this.getTarget() != null && this.tickCount % PRAYER_INTERVAL == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(PRAYER_HEAL);
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.8F);
        }
    }

    /** Cultists nearby rally to him - "defend the clergy" - the moment he is struck. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            this.alarmed = true;
        }
        if (hurt && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker && this.level() instanceof ServerLevel serverLevel) {
            for (SacredCultistEntity cultist : serverLevel.getEntitiesOfClass(SacredCultistEntity.class,
                    new AABB(this.blockPosition()).inflate(CULTIST_ALERT_RADIUS), SacredCultistEntity::isAlive)) {
                cultist.setTarget(attacker);
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        this.setTradingPlayer(null);
        if (this.level() instanceof ServerLevel serverLevel && this.altarPos != null
                && serverLevel.getBlockEntity(this.altarPos) instanceof SacredAltarBlockEntity altar) {
            altar.setPriestPending(false);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!this.level().isClientSide) {
            this.triggerAnim("move", "attack");
        }
        return super.doHurtTarget(target);
    }

    // ------------------------------------------------------------------------------------------
    // Trading: the Cleric's Refined Lyrium offers (and its Holy book), open to anyone he is not angry with.
    // ------------------------------------------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isAlive() || this.isTrading() || hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }
        if (this.getTarget() != null || this.getPose() == Pose.CROUCHING) {
            // angry, or kneeling in vigil: the villager head-shake
            if (!this.level().isClientSide) {
                this.playSound(SoundEvents.VILLAGER_NO, this.getSoundVolume(), this.getVoicePitch());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!this.level().isClientSide) {
            this.setTradingPlayer(player);
            this.openTradingScreen(player, this.getDisplayName(), 1);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public boolean isTrading() {
        return this.tradingPlayer != null;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = LyriumVillagerTrades.priestOffers(this.registryAccess());
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        if (!this.level().isClientSide && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
            this.ambientSoundTime = -this.getAmbientSoundInterval();
            this.playSound(stack.isEmpty() ? SoundEvents.VILLAGER_NO : SoundEvents.VILLAGER_YES);
        }
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
        // no leveling: his offers are fixed
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }

    /** Stands still and faces the customer while the trading screen is open; lets go if they walk off or close it. */
    private static class TradeWithPlayerGoal extends Goal {
        private final SacredPriestEntity priest;

        TradeWithPlayerGoal(SacredPriestEntity priest) {
            this.priest = priest;
            this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player customer = this.priest.tradingPlayer;
            return customer != null && this.priest.isAlive() && this.priest.getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.priest.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player customer = this.priest.tradingPlayer;
            if (customer == null || customer.isRemoved() || this.priest.distanceToSqr(customer) > 64.0
                    || !(customer.containerMenu instanceof MerchantMenu)) {
                this.priest.setTradingPlayer(null);
                return;
            }
            this.priest.getLookControl().setLookAt(customer, 30.0F, 30.0F);
        }

        @Override
        public void stop() {
            this.priest.setTradingPlayer(null);
        }
    }

    /** Lingers for the whole death clip instead of vanilla's 20 ticks, like the Sacred Cultist's. */
    @Override
    protected void tickDeath() {
        ++this.deathTicks;
        if (this.deathTicks >= DEATH_ANIMATION_TICKS && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    // ------------------------------------------------------------------------------------------
    // GeckoLib animation: one controller owns the whole pose - loops, the kneeling bracket and the
    // attack trigger. Praying is read straight from the vanilla pose ChurchServiceGoal already sets.
    // ------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The standard GeckoLib shape, decided fresh every frame: the kneeling pose ChurchServiceGoal sets shows the prayer,
        // otherwise vanilla's limb swing - worked out from how far the entity really moved each tick, so it is reliable for
        // remote entities where GeckoLib's isMoving (delta movement) is not - picks walk or idle, and run while it is
        // chasing (the aggressive flag is synced). No hand-rolled state to get stuck.
        controllers.add(new AnimationController<>(this, "move", 8, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(DEATH_ANIMATION);
            }
            if (this.getPose() == Pose.CROUCHING) {
                return state.setAndContinue(PRAY_ANIMATION);
            }
            if (state.getLimbSwingAmount() > MOVING_LIMB_SWING) {
                return state.setAndContinue(this.isAggressive() ? RUN_ANIMATION : WALK_ANIMATION);
            }
            return state.setAndContinue(IDLE_ANIMATION);
        }).triggerableAnim("attack", ATTACK_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    // ------------------------------------------------------------------------------------------
    // Boss bar, save
    // ------------------------------------------------------------------------------------------

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.altarPos != null) {
            compound.putLong("Altar", this.altarPos.asLong());
        }
        if (this.servicePost != null) {
            compound.putDouble("PostX", this.servicePost.x);
            compound.putDouble("PostY", this.servicePost.y);
            compound.putDouble("PostZ", this.servicePost.z);
            compound.putFloat("PostYaw", this.serviceYaw);
        }
        compound.putBoolean("Alarmed", this.alarmed);
        if (this.offers != null) {
            MerchantOffers.CODEC.encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), this.offers)
                    .ifSuccess(tag -> compound.put("Offers", tag));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Altar")) {
            this.setHome(BlockPos.of(compound.getLong("Altar")));
        }
        if (compound.contains("PostX")) {
            this.servicePost = new Vec3(compound.getDouble("PostX"), compound.getDouble("PostY"), compound.getDouble("PostZ"));
            this.serviceYaw = compound.getFloat("PostYaw");
        }
        this.alarmed = compound.getBoolean("Alarmed");
        if (compound.contains("Offers")) {
            MerchantOffers.CODEC.parse(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), compound.get("Offers"))
                    .ifSuccess(loaded -> this.offers = loaded);
        }
    }

    // Vanilla placeholders until the mod has its own audio.
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }
}
