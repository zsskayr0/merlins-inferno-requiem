package dev.zsskayr.merlins_inferno.entity;

import javax.annotation.Nullable;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * The Hallowed Grove's guardian spirit - "stats de um zombie" (same base numbers as
 * {@link net.minecraft.world.entity.monster.Zombie}, see {@link #createAttributes()}), but not a
 * {@code Zombie} subclass: that would drag in zombie-specific baggage this mob shouldn't have
 * (burning in daylight, converting to a drowned in water, calling reinforcements, breaking doors,
 * attacking villagers/players). Instead this is a fresh {@link PathfinderMob} that just copies the
 * numbers.
 * <p>
 * Passive/neutral toward the player (no target selector ever picks a {@link Player} - it will
 * never attack back even if hit), but proactively hunts anything tagged
 * {@link EntityTypeTags#UNDEAD} on sight.
 * <p>
 * 3 skin variants (see {@link #getVariant()}), randomized on spawn - no actual textures yet, and
 * no trading, per the current design pass. Only spawns in the Hallowed Grove (see
 * {@code ModBiomeProvider}'s mob spawn list - it's simply never added to any other biome).
 */
public class DruidEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(DruidEntity.class, EntityDataSerializers.INT);

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

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // No player-targeting goal on purpose - this mob is passive toward players and never
        // retaliates, even if attacked (no HurtByTargetGoal either). It only ever proactively
        // goes after undead mobs.
        this.targetSelector.addGoal(1,
                new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                        livingEntity -> livingEntity.getType().is(EntityTypeTags.UNDEAD)));
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
}
