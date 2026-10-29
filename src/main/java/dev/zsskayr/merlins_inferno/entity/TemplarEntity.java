package dev.zsskayr.merlins_inferno.entity;

import java.util.EnumSet;
import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * A Templar - the armoured arm of the Sacred Church, and a stronger kind of {@link SacredCultistEntity}: same brotherhood
 * (strike one and the cultists, priests and other Templars within earshot turn on the attacker), same hunt of undead and
 * demons on sight, same humans/neutral-mob tags. Where it differs:
 * <ul>
 *     <li><b>Circle 2 only.</b> It spawns rarely and only near a player who has reached Circle 2 (see {@link #checkTemplarSpawnRules}).</li>
 *     <li><b>Patrols in threes.</b> A natural spawn is a group of three; the first is the leader, who roams, and the other two
 *     keep close behind (see {@link PatrolGoal} and {@link FollowLeaderGoal}). A leader that falls is replaced by a survivor.</li>
 *     <li><b>Neutral to the player</b> - even in Circle 2, unlike the cultists. It turns on a player only when struck, or on sight of
 *     the demonic: anything in the inventory tagged {@code merlins_inferno:demonic_items} (Demonblood and Ebony gear, Infernal
 *     Essence, Dark Coven...), see {@link #isDemonic}.</li>
 *     <li><b>Stronger</b> - a little above the Sacred Priest, and always armed with a Seraphium sword (never dropped).</li>
 * </ul>
 * Animated with GeckoLib ({@code geo/templar.geo.json}, {@code animations/templar.animation.json}).
 */
public class TemplarEntity extends SacredCultistEntity {
    public static final double MAX_HEALTH = 180.0;
    /** Added to the Seraphium sword's own damage. */
    public static final double ATTACK_DAMAGE = 6.0;
    public static final double MOVEMENT_SPEED = 0.26;

    /** A natural spawn attempt only goes through one time in this many - on top of the biome's own weight. */
    private static final int SPAWN_ATTEMPT_ODDS = 12;
    private static final double PATROL_RANGE = 28.0;
    private static final double FOLLOW_CLOSE = 4.0;

    private static final RawAnimation ATTACK_ANIMATION = RawAnimation.begin().thenPlay("animation.templar.attack");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin().thenPlayAndHold("animation.templar.death");
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.templar.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("animation.templar.walk");
    private static final RawAnimation RUN_ANIMATION = RawAnimation.begin().thenLoop("animation.templar.run");
    private static final float MOVING_LIMB_SWING = 0.08F;

    /** The patrol's leader, or null when this Templar leads (or walks alone). */
    @Nullable
    private UUID leaderId;

    public TemplarEntity(EntityType<? extends TemplarEntity> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    /**
     * Circle 2 only, rare, on solid ground, and never in peaceful. Spawn eggs, spawners and commands skip the gate. The circle is
     * read from the nearest player, like Cain's rule.
     */
    public static boolean checkTemplarSpawnRules(EntityType<TemplarEntity> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos,
            RandomSource random) {
        if (spawnType == MobSpawnType.SPAWNER || spawnType == MobSpawnType.SPAWN_EGG || spawnType == MobSpawnType.COMMAND) {
            return true;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (!level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type)
                || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!ProgressionHelper.hasReached(serverLevel, ProgressionHelper.SECOND_CIRCLE)) {
            return false;
        }
        return random.nextInt(SPAWN_ATTEMPT_ODDS) == 0;
    }

    /** True when the player carries, wears or holds anything of the {@link ModTags.Items#DEMONIC_ITEMS demonic} set. */
    public static boolean isDemonic(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModTags.Items.DEMONIC_ITEMS)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.is(ModTags.Items.DEMONIC_ITEMS)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(ModTags.Items.DEMONIC_ITEMS)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(4, new FollowLeaderGoal(this));
        this.goalSelector.addGoal(5, new PatrolGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // Retaliation: strike one and the whole brotherhood answers.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, SacredCultistEntity.class).setAlertOthers(SacredCultistEntity.class));
        // Neutral to the player - unless it sees something demonic on them.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                living -> living instanceof Player player && isDemonic(player)));
        // The faithful hunt the unholy: undead and demons, on sight.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false,
                living -> living.getType().is(EntityTypeTags.UNDEAD) || living.getType().is(ModTags.EntityTypes.DEMON)));
    }

    // ------------------------------------------------------------------------------------------
    // Patrol: the leader roams, the other two shadow it. Group membership is the leader's UUID.
    // ------------------------------------------------------------------------------------------

    /** Shared between the members of one natural spawn; the first Templar to spawn leads. */
    private static class PatrolData implements SpawnGroupData {
        final UUID leader;

        PatrolData(UUID leader) {
            this.leader = leader;
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData instanceof PatrolData ? null : spawnGroupData);
        // Always a Seraphium sword (the cultist's random pick is overridden); never dropped.
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SERAPHIUM_SWORD.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        if (spawnGroupData instanceof PatrolData patrol) {
            this.leaderId = patrol.leader;
            return patrol;
        }
        return new PatrolData(this.getUUID());
    }

    /** The living leader of this patrol, or null if this Templar leads (or its leader is gone - then it takes over). */
    @Nullable
    private TemplarEntity leader() {
        if (this.leaderId == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        if (level.getEntity(this.leaderId) instanceof TemplarEntity leader && leader.isAlive() && leader != this) {
            return leader;
        }
        this.leaderId = null;
        return null;
    }

    /** A leader (or lone Templar) wanders far afield at a steady march. */
    private static class PatrolGoal extends Goal {
        private final TemplarEntity templar;
        private int cooldown;

        PatrolGoal(TemplarEntity templar) {
            this.templar = templar;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.templar.getTarget() != null || this.templar.leader() != null) {
                return false;
            }
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            return this.templar.getNavigation().isDone();
        }

        @Override
        public boolean canContinueToUse() {
            return this.templar.getTarget() == null && !this.templar.getNavigation().isDone();
        }

        @Override
        public void start() {
            Vec3 spot = DefaultRandomPos.getPos(this.templar, (int) PATROL_RANGE, 7);
            if (spot == null) {
                this.cooldown = 40;
                return;
            }
            this.templar.getNavigation().moveTo(spot.x, spot.y, spot.z, 0.6);
            this.cooldown = 20 + this.templar.getRandom().nextInt(60);
        }
    }

    /** A follower stays close behind its leader, hurrying when it falls behind. */
    private static class FollowLeaderGoal extends Goal {
        private final TemplarEntity templar;
        private int recompute;

        FollowLeaderGoal(TemplarEntity templar) {
            this.templar = templar;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            TemplarEntity leader = this.templar.leader();
            return leader != null && this.templar.getTarget() == null
                    && this.templar.distanceToSqr(leader) > FOLLOW_CLOSE * FOLLOW_CLOSE;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            TemplarEntity leader = this.templar.leader();
            if (leader == null || --this.recompute > 0) {
                return;
            }
            this.recompute = 10;
            double behind = this.templar.distanceToSqr(leader);
            this.templar.getNavigation().moveTo(leader, behind > 144.0 ? 1.1 : 0.65);
        }

        @Override
        public void stop() {
            this.templar.getNavigation().stop();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.leaderId != null) {
            tag.putUUID("Leader", this.leaderId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.leaderId = tag.hasUUID("Leader") ? tag.getUUID("Leader") : null;
    }

    /**
     * Unlike the church's cultists, patrols are not permanent fixtures: out of sight and far from every player they fade away
     * (unless named). The cultist base class pins its members in place, so both are undone here.
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return true;
    }

    @Override
    public boolean isPersistenceRequired() {
        return this.hasCustomName() && super.isPersistenceRequired();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 6, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(DEATH_ANIMATION);
            }
            if (state.getLimbSwingAmount() > MOVING_LIMB_SWING) {
                return state.setAndContinue(this.isAggressive() ? RUN_ANIMATION : WALK_ANIMATION);
            }
            return state.setAndContinue(IDLE_ANIMATION);
        }).triggerableAnim("attack", ATTACK_ANIMATION));
    }
}
