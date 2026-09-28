package dev.zsskayr.merlins_inferno.entity;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * The Hallowed Grove's guardian spirit - "stats de um zombie" (same base numbers as
 * {@link net.minecraft.world.entity.monster.Zombie}, see {@link #createAttributes()}), but not a
 * {@code Zombie} subclass: that would drag in zombie-specific baggage this mob shouldn't have
 * (burning in daylight, converting to a drowned in water, calling reinforcements, breaking doors,
 * attacking villagers/players). Instead this is a fresh {@link PathfinderMob} that just copies the
 * numbers.
 * <p>
 * Neutral toward the player by default - never attacks first, and (per the design doc, 4.1) never
 * retaliates either, even if hit; that's a deliberate combat-balance decision left for later, not
 * an oversight. It does proactively hunt anything tagged {@link EntityTypeTags#UNDEAD} on sight -
 * except the {@link DullahanEntity}, which is far too strong for it: it flees from that one.
 * <p>
 * 3 skin variants (see {@link #getVariant()}), randomized on spawn - no actual textures yet.
 * Implements {@link Merchant} directly (rather than extending {@code AbstractVillager}, which
 * drags in the profession/reputation/leveling system this doesn't need) for a single fixed trade -
 * emeralds for Otherworld Essence, see {@link #updateTrades()} - closing the loop the design doc
 * describes: without this trade (or, narratively, killing a Dullahan - not implemented), raw
 * Rowanwood collected from the tree has no way to become the finished Rowanwood Bar. Only spawns
 * in the Hallowed Grove (see {@code ModBiomeProvider}'s mob spawn list - it's simply never added
 * to any other biome); a dedicated grove/sanctuary landmark structure to guarantee a spawn is
 * recommended but out of scope for now (design doc 4.3).
 */
public class DruidEntity extends PathfinderMob implements Merchant {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(DruidEntity.class, EntityDataSerializers.INT);

    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers offers;

    public DruidEntity(EntityType<? extends DruidEntity> type, Level level) {
        super(type, level);
    }

    /** Same base numbers as {@code Zombie.createAttributes()} - see the class javadoc for why this isn't just extending Zombie. */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.FOLLOW_RANGE, 35.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
    }

    /** Like vanilla animals: a wandering Druid in the grove never despawns just because the player walked off. */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new StandStillWhileTradingGoal());
        // Flees the Dullahan (which shares the Grove and now counts as undead - see below): a Druid
        // has 20 health against its 15 damage, so hunting it would be suicide. Priority 2, after the
        // trade stand-still, so a customer isn't left with a Druid running off mid-trade.
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, DullahanEntity.class, 20.0F, 1.0, 1.3));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // No player-targeting goal on purpose - this mob is passive toward players and never
        // retaliates, even if attacked (no HurtByTargetGoal either). It only ever proactively
        // goes after undead mobs - every one except the Dullahan, which it runs from instead.
        this.targetSelector.addGoal(1,
                new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                        livingEntity -> livingEntity.getType().is(EntityTypeTags.UNDEAD) && !(livingEntity instanceof DullahanEntity)));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        this.entityData.set(DATA_VARIANT, this.random.nextInt(3));
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        // Not inherited for free: PathfinderMob/Mob never calls this on its own (only Monster's
        // own finalizeSpawn does, which we don't have since this isn't a Monster) - Zombie and
        // Skeleton both work this way under the hood, we just have to trigger it ourselves.
        this.populateDefaultEquipmentSlots(this.random, difficulty);
        this.populateDefaultEquipmentEnchantments(level, this.random, difficulty);
        return result;
    }

    /** Independent 15% roll per armor slot, matching vanilla's own "sometimes" cadence for monster gear. */
    private static final float ARMOR_PIECE_CHANCE = 0.15F;
    /** Weapon roll is rarer - it's a bigger combat swing than one armor piece. */
    private static final float WEAPON_CHANCE = 0.05F;

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        // Deliberately NOT calling super here: Mob's own default rolls the vanilla leather/chain/
        // iron/diamond/netherite ladder, which isn't what's wanted - every piece here is either
        // plain leather or Rowanwood ("Druid" armor - see the lang file, rowanwood_helmet etc. are
        // displayed as "Druid Helmet"), independently per slot, so a Druid can end up in a mismatched
        // leather-and-Rowanwood mix, which reads fine for a forest spirit's gear.
        if (random.nextFloat() < ARMOR_PIECE_CHANCE) {
            equip(EquipmentSlot.HEAD, random, Items.LEATHER_HELMET, ModItems.ROWANWOOD_HELMET.get());
        }
        if (random.nextFloat() < ARMOR_PIECE_CHANCE) {
            equip(EquipmentSlot.CHEST, random, Items.LEATHER_CHESTPLATE, ModItems.ROWANWOOD_CHESTPLATE.get());
        }
        if (random.nextFloat() < ARMOR_PIECE_CHANCE) {
            equip(EquipmentSlot.LEGS, random, Items.LEATHER_LEGGINGS, ModItems.ROWANWOOD_LEGGINGS.get());
        }
        if (random.nextFloat() < ARMOR_PIECE_CHANCE) {
            equip(EquipmentSlot.FEET, random, Items.LEATHER_BOOTS, ModItems.ROWANWOOD_BOOTS.get());
        }

        // Weapon: iron or Rowanwood, sword or axe - 4 equally likely outcomes.
        if (random.nextFloat() < WEAPON_CHANCE) {
            boolean sword = random.nextBoolean();
            boolean rowanwood = random.nextBoolean();
            Item weapon = sword
                    ? (rowanwood ? ModItems.ROWANWOOD_SWORD.get() : Items.IRON_SWORD)
                    : (rowanwood ? ModItems.ROWANWOOD_AXE.get() : Items.IRON_AXE);
            equip(EquipmentSlot.MAINHAND, weapon, rowanwood);
        }
    }

    /**
     * Picks leather/iron or Rowanwood for one slot and equips it. Every {@code Mob} defaults to an
     * 8.5% per-slot drop chance ({@code Arrays.fill(armorDropChances/handDropChances, 0.085F)} in
     * the base class constructor) - fine for leather/iron, but Rowanwood is meant to be
     * craft-only, so any Rowanwood piece rolled here gets its drop chance zeroed out right after
     * (otherwise killing enough Druids would passively farm gear that's supposed to require
     * Rowanwood bars and a crafting table).
     */
    private void equip(EquipmentSlot slot, RandomSource random, Item plainTier, Item rowanwood) {
        boolean isRowanwood = random.nextBoolean();
        equip(slot, isRowanwood ? rowanwood : plainTier, isRowanwood);
    }

    private void equip(EquipmentSlot slot, Item item, boolean isRowanwood) {
        this.setItemSlot(slot, new ItemStack(item));
        if (isRowanwood) {
            this.setDropChance(slot, 0.0F);
        }
    }

    /** Which of the 3 skins (0-2) this individual uses - no textures assigned to them yet, see class javadoc. */
    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    private static final String TAG_VARIANT = "Variant";
    private static final String TAG_OFFERS = "Offers";

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(TAG_VARIANT, this.getVariant());
        compound.putLong(TAG_LAST_RESTOCK_DAY, this.lastRestockDay);
        if (!this.level().isClientSide) {
            MerchantOffers currentOffers = this.getOffers();
            if (!currentOffers.isEmpty()) {
                compound.put(TAG_OFFERS,
                        MerchantOffers.CODEC.encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), currentOffers).getOrThrow());
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        // finalizeSpawn (where DATA_VARIANT is normally rolled) only ever runs on initial spawn,
        // never on load-from-disk - without this, every Druid would silently reset to variant 0
        // the moment its chunk unloads and reloads.
        if (compound.contains(TAG_VARIANT)) {
            this.entityData.set(DATA_VARIANT, compound.getInt(TAG_VARIANT));
        }
        if (compound.contains(TAG_LAST_RESTOCK_DAY)) {
            this.lastRestockDay = compound.getLong(TAG_LAST_RESTOCK_DAY);
        }
        if (compound.contains(TAG_OFFERS)) {
            MerchantOffers.CODEC.parse(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), compound.get(TAG_OFFERS))
                    .resultOrPartial(error -> {
                    })
                    .ifPresent(loaded -> this.offers = loaded);
        }
    }

    // --- Daily restock: the trade's uses come back once per in-game day (design decision - a
    // Druid is a renewable source of Mundane Essence, not a one-shot). Vanilla's Villager does
    // this through its profession/work-station system, which this mob deliberately doesn't have. ---

    private static final long TICKS_PER_DAY = 24000L;
    private static final String TAG_LAST_RESTOCK_DAY = "LastRestockDay";
    private long lastRestockDay = Long.MIN_VALUE;

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % 100 == 0 && this.offers != null) {
            long day = this.level().getDayTime() / TICKS_PER_DAY;
            if (this.lastRestockDay == Long.MIN_VALUE) {
                this.lastRestockDay = day;
            } else if (day > this.lastRestockDay) {
                this.lastRestockDay = day;
                // Never mid-trade: the open screen already holds a reference to these offers.
                if (!this.isTrading()) {
                    for (MerchantOffer offer : this.offers) {
                        offer.resetUses();
                    }
                }
            }
        }
    }

    /**
     * Plays the part of vanilla's TradeWithPlayerGoal/LookAtTradingPlayerGoal, which are tied to
     * AbstractVillager: stands still and faces the customer, and ends the trade when the player
     * walks off (MerchantMenu itself never checks distance, so without this a player could keep
     * trading from across the map while the Druid wandered away).
     */
    private class StandStillWhileTradingGoal extends Goal {
        private static final double MAX_TRADE_DISTANCE_SQR = 64.0;

        StandStillWhileTradingGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return DruidEntity.this.isAlive() && DruidEntity.this.tradingPlayer != null;
        }

        @Override
        public void start() {
            DruidEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player customer = DruidEntity.this.tradingPlayer;
            if (customer == null) {
                return;
            }
            if (!customer.isAlive() || DruidEntity.this.distanceToSqr(customer) > MAX_TRADE_DISTANCE_SQR) {
                customer.closeContainer();
                DruidEntity.this.setTradingPlayer(null);
                return;
            }
            DruidEntity.this.getLookControl().setLookAt(customer);
        }

        @Override
        public void stop() {
            // The menu clears the trading player itself when the screen closes.
        }
    }

    // --- Merchant: a single fixed trade, no profession/leveling/reputation system - see class javadoc. ---

    private static final int EMERALD_PRICE = 20;
    private static final int MAX_USES = 4;
    private static final int TRADE_XP = 5;
    private static final float PRICE_MULTIPLIER = 0.05F;

    private void updateTrades() {
        MerchantOffers currentOffers = this.getOffers();
        if (currentOffers.isEmpty()) {
            currentOffers.add(new MerchantOffer(new ItemCost(Items.EMERALD, EMERALD_PRICE),
                    new ItemStack(ModItems.MUNDANE_ESSENCE.get()), MAX_USES, TRADE_XP, PRICE_MULTIPLIER));
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isAlive() && !this.isTrading() && hand == InteractionHand.MAIN_HAND) {
            if (!this.level().isClientSide) {
                if (this.getOffers().isEmpty()) {
                    return InteractionResult.CONSUME;
                }
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void setTradingPlayer(@Nullable Player tradingPlayer) {
        this.tradingPlayer = tradingPlayer;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    public boolean isTrading() {
        return this.tradingPlayer != null;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.level().isClientSide) {
            throw new IllegalStateException("Cannot load Druid offers on the client");
        }
        if (this.offers == null) {
            this.offers = new MerchantOffers();
            this.updateTrades();
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        // Never called for a non-leveling merchant like this one - vanilla only invokes it when
        // restocking a Villager's profession-based trade list.
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
        // No leveling system - see class javadoc.
    }

    @Override
    public boolean showProgressBar() {
        return true;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        this.setTradingPlayer(null);
    }
}
