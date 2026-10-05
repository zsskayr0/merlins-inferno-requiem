package dev.zsskayr.merlins_inferno.blockentity;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.block.SacredAltarBlock;
import dev.zsskayr.merlins_inferno.entity.EliasEntity;
import dev.zsskayr.merlins_inferno.entity.SacredCultistEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModParticles;
import dev.zsskayr.merlins_inferno.worldgen.structure.ChurchCongregation;
import dev.zsskayr.merlins_inferno.worldgen.structure.GreatBell;

/**
 * The Sacred Altar's memory: where the Great Bell hangs, where Elias appears, and when
 * the vigil may be kept again, and whether the Sacred Priest still guards it. The church's structure piece fills this in ({@link #configure}) when it
 * places the altar; Elias's death starts the cooldown ({@link #startCooldown}), and a diamond laid on
 * the altar after it ({@link #invoke}) rehangs the bell and wakes a new Elias.
 * <p>
 * The altar is a GeckoLib block entity ({@code celestial_altar}): its idle clip loops forever, and invoking it plays the
 * activate clip (see {@link #invoke}); Elias is woken when the clip reaches its peak.
 * <p>
 * It also keeps the congregation: every seven in-game days it refills the pews that stand empty
 * ({@link ChurchCongregation}), and the first time a Circle 2 player is near it seats the extra row of cultists.
 */
public class SacredAltarBlockEntity extends BlockEntity implements GeoBlockEntity {
    /** One in-game day (20 minutes) between Elias's death and the next vigil - stops the 1% drop from being spammed. */
    public static final long COOLDOWN_TICKS = 24000L;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.celestial_altar.idle");
    private static final RawAnimation ACTIVATE = RawAnimation.begin().thenPlay("animation.celestial_altar.activate");
    /** The idle clip's length (9.6 s). The activate clip starts and ends exactly on the idle's first frame, so it is only
     *  ever started on a multiple of this since {@link #idleEpoch}. */
    public static final int IDLE_TICKS = 192;
    /** The activate clip's length (11.3 s) and the moment Elias appears in it (9.0 s, the spin's peak). */
    public static final int ACTIVATE_TICKS = 226;
    public static final int SPAWN_TICKS = 180;
    /** The cold state's two transitions (4.0 s and 4.5 s): the altar powers down, and later wakes again. */
    public static final int POWER_DOWN_TICKS = 80;
    public static final int AWAKEN_TICKS = 90;
    /** {@link #coldState}: awake (idle / ritual), powering down, cold, waking. */
    private static final int AWAKE = 0, POWERING_DOWN = 1, COLD = 2, AWAKENING = 3;
    private static final RawAnimation COLD_ANIM = RawAnimation.begin().thenLoop("animation.celestial_altar.cold");
    private static final RawAnimation POWER_DOWN_ANIM = RawAnimation.begin().thenPlay("animation.celestial_altar.power_down");
    private static final RawAnimation AWAKEN_ANIM = RawAnimation.begin().thenPlay("animation.celestial_altar.awaken");

    /** What the model is playing at a given moment. */
    public enum Clip { IDLE, ACTIVATE, POWER_DOWN, COLD, AWAKEN }
    /** The earliest the clip may start after the diamond is laid: lets every nearby client learn of it first. */
    private static final int LEAD_TICKS = 20;

    /** A Elias this close to the altar counts as "already awake". */
    private static final double ELIAS_SEARCH_RADIUS = 64.0;

    private long readyAt;
    @Nullable
    private BlockPos bellPos;
    @Nullable
    private BlockPos spawnPos;
    /** True from the church's generation until its Sacred Priest falls: Elias cannot be woken meanwhile. */
    private boolean priestPending;
    /** The game tick at which the idle clip is at its first frame (moves forward each time the activate clip ends). */
    private long idleEpoch;
    /** The game tick the activate clip starts on, or -1 while the altar rests. */
    private long activationStart = -1L;
    private boolean eliasSpawned;
    /** The cold state: see {@link #tickCold}. {@link #coldStart} is the game tick of the transition that is under way. */
    private int coldState;
    private long coldStart;
    /** Server only: whether an Elias lives near (refreshed once a second). */
    private boolean eliasAlive;
    /** Not saved: the parts are re-checked once each time the altar loads. */
    private boolean partsChecked;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Client only: where the cross currently looks (radians, in the model's frame) and when that was last updated. */
    public float crossYaw;
    public long crossClock;

    /** How often the congregation is checked, in ticks, and how close a player must be for the church to be active. */
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final double ACTIVE_RADIUS = 96.0;
    /** A cultist with its post this close to a seat's post is that seat's occupant. */
    private static final double SEAT_MATCH_SQR = 0.25;
    /** Seats beside the aisle that make up the Circle 1 congregation, the extra row for Circle 2, and the middle of the nave. */
    private final List<BlockPos> baseSeats = new ArrayList<>();
    private final List<BlockPos> extraSeats = new ArrayList<>();
    @Nullable
    private BlockPos churchCenter;
    private long nextRefill;
    private boolean extrasSeated;

    public SacredAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SACRED_ALTAR.get(), pos, state);
    }

    public void configure(BlockPos bell, BlockPos spawn, boolean priestPending) {
        this.bellPos = bell.immutable();
        this.spawnPos = spawn.immutable();
        this.priestPending = priestPending;
        this.setChanged();
    }

    /** Records the pews the congregation sits in and starts the seven-day refill clock. */
    public void setCongregation(BlockPos center, List<BlockPos> base, List<BlockPos> extra, long gameTime) {
        this.churchCenter = center.immutable();
        this.baseSeats.clear();
        this.baseSeats.addAll(base);
        this.extraSeats.clear();
        this.extraSeats.addAll(extra);
        this.nextRefill = gameTime + ChurchCongregation.REFILL_INTERVAL_TICKS;
        this.setChanged();
    }

    /** Run by the altar block's ticker on the server. */
    public void serverTick(ServerLevel level) {
        this.tickCold(level);
        if (!this.partsChecked) {
            // altars that predate the hitbox parts (or lost some to a half-generated chunk) get them back, in free cells only
            this.partsChecked = true;
            SacredAltarBlock.placeParts(level, this.worldPosition);
            this.faceTheCongregation(level);
        }
        if (this.activationStart >= 0L) {
            this.tickRitual(level);
        }
        if (this.churchCenter == null || level.getGameTime() % CHECK_INTERVAL_TICKS != 0L) {
            return;
        }
        boolean circleTwoNear = false;
        boolean anyoneNear = false;
        for (Player player : level.players()) {
            if (player.distanceToSqr(this.worldPosition.getX() + 0.5, this.worldPosition.getY(), this.worldPosition.getZ() + 0.5) <= ACTIVE_RADIUS * ACTIVE_RADIUS) {
                anyoneNear = true;
                circleTwoNear |= ProgressionHelper.hasReached(player, ProgressionHelper.SECOND_CIRCLE);
            }
        }
        if (!anyoneNear) {
            return; // the pews are only tended while somebody is around to see them
        }
        boolean refillDue = level.getGameTime() >= this.nextRefill;
        boolean seatExtras = circleTwoNear && (refillDue || !this.extrasSeated);
        if (!refillDue && !seatExtras) {
            return;
        }
        if (refillDue) {
            this.fillEmptySeats(level, this.baseSeats);
            this.nextRefill = level.getGameTime() + ChurchCongregation.REFILL_INTERVAL_TICKS;
        }
        if (seatExtras) {
            this.fillEmptySeats(level, this.extraSeats);
            this.extrasSeated = true;
        }
        this.setChanged();
    }

    private void fillEmptySeats(ServerLevel level, List<BlockPos> seats) {
        List<SacredCultistEntity> present = level.getEntitiesOfClass(SacredCultistEntity.class,
                new AABB(this.worldPosition).inflate(ACTIVE_RADIUS), c -> c.isAlive() && c.servicePost() != null);
        for (BlockPos seat : seats) {
            Vec3 post = ChurchCongregation.postFor(seat);
            boolean taken = false;
            for (SacredCultistEntity cultist : present) {
                if (cultist.servicePost().distanceToSqr(post) < SEAT_MATCH_SQR) {
                    taken = true;
                    break;
                }
            }
            if (!taken && this.churchCenter != null) {
                ChurchCongregation.spawn(level, seat, this.churchCenter);
            }
        }
    }

    public boolean isPriestPending() {
        return this.priestPending;
    }

    public void setPriestPending(boolean pending) {
        this.priestPending = pending;
        this.setChanged();
    }

    /** The bell block at the heart of the Great Bell, or null if this altar was never given one. */
    @Nullable
    public BlockPos getBellPos() {
        return this.bellPos;
    }

    public boolean isReady(Level level) {
        return level.getGameTime() >= this.readyAt;
    }

    /** Minutes are what the player needs to know, not ticks. */
    public long remainingMinutes(Level level) {
        return Math.max(0L, (this.readyAt - level.getGameTime() + 1199L) / 1200L);
    }

    public void startCooldown(Level level) {
        this.readyAt = level.getGameTime() + COOLDOWN_TICKS;
        this.setChanged();
    }

    public boolean hasLivingElias(ServerLevel level) {
        return !level.getEntitiesOfClass(EliasEntity.class, new AABB(this.worldPosition).inflate(ELIAS_SEARCH_RADIUS), Entity::isAlive).isEmpty();
    }

    /** Rehangs the Great Bell where it was broken (only in empty spots - never over something the player built). */
    public void restoreBell(ServerLevel level) {
        if (this.bellPos != null) {
            GreatBell.build(level, null, this.bellPos);
        }
    }

    /** An altar that belongs to a church always faces its nave - the congregation - however it was generated or placed before. */
    private void faceTheCongregation(ServerLevel level) {
        if (this.churchCenter == null) {
            return;
        }
        Direction towards = Direction.getNearest(this.churchCenter.getX() - this.worldPosition.getX(), 0.0,
                this.churchCenter.getZ() - this.worldPosition.getZ());
        BlockState state = this.getBlockState();
        if (state.getBlock() instanceof SacredAltarBlock && towards.getAxis().isHorizontal() && state.getValue(SacredAltarBlock.FACING) != towards) {
            level.setBlock(this.worldPosition, state.setValue(SacredAltarBlock.FACING, towards), 3);
        }
    }

    /** True while the ritual runs. */
    public boolean isActivating() {
        return this.activationStart >= 0L;
    }

    /**
     * The altar is cold - it has lost its power - while the Sacred Priest still guards the church, while the cooldown after
     * Elias's death runs and while an Elias is awake. It powers down on an idle loop boundary (so the clips join without a
     * cut), stays still, and wakes again as soon as none of that holds.
     */
    private void tickCold(ServerLevel level) {
        long now = level.getGameTime();
        if (now % 20L == 0L) {
            this.eliasAlive = this.hasLivingElias(level);
        }
        boolean wantCold = this.priestPending || !this.isReady(level) || this.eliasAlive;
        switch (this.coldState) {
            case AWAKE -> {
                if (wantCold && this.activationStart < 0L) {
                    this.coldStart = this.nextIdleBoundary(now);
                    this.coldState = POWERING_DOWN;
                    this.sync();
                }
            }
            case POWERING_DOWN -> {
                if (now >= this.coldStart + POWER_DOWN_TICKS) {
                    this.coldState = COLD;
                    this.sync();
                }
            }
            case COLD -> {
                if (!wantCold) {
                    this.coldStart = now + LEAD_TICKS;
                    this.coldState = AWAKENING;
                    this.sync();
                }
            }
            case AWAKENING -> {
                if (now >= this.coldStart + AWAKEN_TICKS) {
                    this.idleEpoch = this.coldStart + AWAKEN_TICKS; // the awaken clip ends on the idle's first frame
                    this.coldState = AWAKE;
                    this.sync();
                }
            }
            default -> this.coldState = AWAKE;
        }
    }

    /** The next first frame of the idle loop that is far enough ahead for every nearby client to have been told. */
    private long nextIdleBoundary(long now) {
        long loops = Math.floorDiv(now + LEAD_TICKS - this.idleEpoch + IDLE_TICKS - 1, IDLE_TICKS);
        return this.idleEpoch + loops * IDLE_TICKS;
    }

    /**
     * Starts the ritual: the altar plays its activate clip from the next idle loop boundary (so the two clips join
     * without a cut), and Elias is woken {@link #SPAWN_TICKS} into it (see {@link #tickRitual}).
     */
    public boolean invoke(ServerLevel level) {
        if (this.isActivating() || this.coldState != AWAKE) {
            return false;
        }
        this.activationStart = this.nextIdleBoundary(level.getGameTime());
        this.eliasSpawned = false;
        this.sync();
        return true;
    }

    private void tickRitual(ServerLevel level) {
        long t = level.getGameTime() - this.activationStart;
        if (t < 0) {
            return;
        }
        double cx = this.worldPosition.getX() + 0.5;
        double cy = this.worldPosition.getY() + 1.7;
        double cz = this.worldPosition.getZ() + 0.5;
        if (!this.eliasSpawned && t >= SPAWN_TICKS - 60 && t < SPAWN_TICKS) {
            level.sendParticles(ModParticles.SANCTIFIED.get(), cx, cy, cz, 1 + (int) ((t - (SPAWN_TICKS - 60)) / 12), 0.5, 0.6, 0.5, 0.02);
        }
        if (!this.eliasSpawned && t >= SPAWN_TICKS) {
            this.eliasSpawned = true;
            level.sendParticles(ModParticles.SANCTIFIED.get(), cx, cy, cz, 80, 0.7, 0.8, 0.7, 0.12);
            this.wakeElias(level);
            this.setChanged();
        }
        if (t >= ACTIVATE_TICKS) {
            this.idleEpoch = this.activationStart + ACTIVATE_TICKS;
            this.activationStart = -1L;
            this.sync();
        }
    }

    /** Rehangs the bell and wakes Elias at the altar. */
    private void wakeElias(ServerLevel level) {
        this.restoreBell(level);
        // an altar placed by hand was never given a spot by a church: Elias appears three blocks in front of it
        BlockPos spawn = this.spawnPos != null ? this.spawnPos
                : this.worldPosition.relative(this.getBlockState().getValue(SacredAltarBlock.FACING), 3);
        EliasEntity elias = ModEntityTypes.ELIAS.get().create(level);
        if (elias == null) {
            return;
        }
        elias.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 180.0F, 0.0F);
        elias.setHome(this.worldPosition);
        elias.finalizeSpawn(level, level.getCurrentDifficultyAt(this.worldPosition), MobSpawnType.MOB_SUMMONED, null);
        level.addFreshEntity(elias);
    }

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    // --- client side: animation ---

    /** The clip the model plays at game time {@code now} (in ticks, with the partial tick). */
    public Clip currentClip(double now) {
        if (this.activationStart >= 0L && now >= this.activationStart && now < this.activationStart + ACTIVATE_TICKS) {
            return Clip.ACTIVATE;
        }
        switch (this.coldState) {
            case POWERING_DOWN:
                if (now >= this.coldStart) {
                    return now < this.coldStart + POWER_DOWN_TICKS ? Clip.POWER_DOWN : Clip.COLD;
                }
                break;
            case COLD:
                return Clip.COLD;
            case AWAKENING:
                if (now < this.coldStart) {
                    return Clip.COLD;
                }
                if (now < this.coldStart + AWAKEN_TICKS) {
                    return Clip.AWAKEN;
                }
                break;
            default:
                break;
        }
        return Clip.IDLE;
    }

    /** The tick the idle loop is at its first frame, allowing for a clip that has just ended before the server has said so. */
    private long idleEpochAt(double now) {
        if (this.activationStart >= 0L && now >= this.activationStart + ACTIVATE_TICKS) {
            return this.activationStart + ACTIVATE_TICKS;
        }
        if (this.coldState == AWAKENING && now >= this.coldStart + AWAKEN_TICKS) {
            return this.coldStart + AWAKEN_TICKS;
        }
        return this.idleEpoch;
    }

    private double idlePhaseTicks(double now) {
        return (((now - this.idleEpochAt(now)) % IDLE_TICKS) + IDLE_TICKS) % IDLE_TICKS;
    }

    /** Seconds into the clip that is playing, for the glow layer. */
    public double clipSeconds(float partialTick) {
        if (this.level == null) {
            return 0.0;
        }
        double now = this.level.getGameTime() + partialTick;
        return switch (this.currentClip(now)) {
            case ACTIVATE -> (now - this.activationStart) / 20.0;
            case POWER_DOWN, AWAKEN -> (now - this.coldStart) / 20.0;
            case COLD -> 0.0;
            default -> this.idlePhaseTicks(now) / 20.0;
        };
    }

    public boolean isInActivation(float partialTick) {
        return this.level != null && this.currentClip(this.level.getGameTime() + partialTick) == Clip.ACTIVATE;
    }

    /** The altar is awake: it shows its idle motion or the ritual, so the cross may turn to the players. */
    public boolean isAwakeClip(float partialTick) {
        if (this.level == null) {
            return true;
        }
        Clip clip = this.currentClip(this.level.getGameTime() + partialTick);
        return clip == Clip.IDLE || clip == Clip.ACTIVATE;
    }

    /** 1 while the glow is lit, fading out as the altar powers down (before the crystal is swapped) and in again as it wakes. */
    public double glowScale(float partialTick) {
        if (this.level == null) {
            return 1.0;
        }
        double now = this.level.getGameTime() + partialTick;
        double t = (now - this.coldStart) / 20.0;
        return switch (this.currentClip(now)) {
            case POWER_DOWN -> 1.0 - smooth(t / 1.0);
            case COLD -> 0.0;
            case AWAKEN -> smooth((t - 3.4) / 1.0);
            default -> 1.0;
        };
    }

    private static double smooth(double x) {
        x = Math.max(0.0, Math.min(1.0, x));
        return x * x * (3 - 2 * x);
    }

    /** A controller that can be pinned to the shared idle clock (see {@link #IDLE_TICKS}). */
    private static class AltarController extends AnimationController<SacredAltarBlockEntity> {
        private boolean idleAligned;

        AltarController(SacredAltarBlockEntity altar) {
            super(altar, "altar", 0, AltarController::animate);
        }

        /** Makes the idle clip's current time {@code phaseTicks}, so every client shows the same moment of it. */
        void alignIdle(double seekTick, double phaseTicks) {
            this.tickOffset = seekTick - phaseTicks;
        }

        private static PlayState animate(AnimationState<SacredAltarBlockEntity> state) {
            SacredAltarBlockEntity altar = state.getAnimatable();
            AltarController controller = (AltarController) state.getController();
            if (altar.level == null) {
                return state.setAndContinue(IDLE);
            }
            float partial = state.getPartialTick();
            double now = altar.level.getGameTime() + partial;
            Clip clip = altar.currentClip(now);
            if (clip != Clip.IDLE) {
                controller.idleAligned = false;
                return state.setAndContinue(switch (clip) {
                    case ACTIVATE -> ACTIVATE;
                    case POWER_DOWN -> POWER_DOWN_ANIM;
                    case AWAKEN -> AWAKEN_ANIM;
                    default -> COLD_ANIM;
                });
            }
            PlayState play = state.setAndContinue(IDLE);
            if (!controller.idleAligned && controller.getAnimationState() == State.RUNNING && IDLE.equals(controller.getCurrentRawAnimation())) {
                controller.alignIdle(state.getAnimationTick(), altar.idlePhaseTicks(now));
                controller.idleAligned = true;
            }
            return play;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AltarController(this));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("ReadyAt", this.readyAt);
        tag.putBoolean("PriestPending", this.priestPending);
        tag.putLong("IdleEpoch", this.idleEpoch);
        tag.putLong("ActivationStart", this.activationStart);
        tag.putBoolean("EliasSpawned", this.eliasSpawned);
        tag.putInt("ColdState", this.coldState);
        tag.putLong("ColdStart", this.coldStart);
        if (this.bellPos != null) {
            tag.putLong("Bell", this.bellPos.asLong());
        }
        if (this.spawnPos != null) {
            tag.putLong("SpawnPos", this.spawnPos.asLong());
        }
        if (this.churchCenter != null) {
            tag.putLong("Center", this.churchCenter.asLong());
        }
        tag.putLongArray("BaseSeats", this.baseSeats.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLongArray("ExtraSeats", this.extraSeats.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLong("NextRefill", this.nextRefill);
        tag.putBoolean("ExtrasSeated", this.extrasSeated);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.readyAt = tag.getLong("ReadyAt");
        this.priestPending = tag.getBoolean("PriestPending");
        this.idleEpoch = tag.getLong("IdleEpoch");
        this.activationStart = tag.contains("ActivationStart") ? tag.getLong("ActivationStart") : -1L;
        this.eliasSpawned = tag.getBoolean("EliasSpawned");
        this.coldState = tag.getInt("ColdState");
        this.coldStart = tag.getLong("ColdStart");
        this.bellPos = tag.contains("Bell") ? BlockPos.of(tag.getLong("Bell")) : null;
        this.spawnPos = tag.contains("SpawnPos") ? BlockPos.of(tag.getLong("SpawnPos")) : null;
        this.churchCenter = tag.contains("Center") ? BlockPos.of(tag.getLong("Center")) : null;
        this.baseSeats.clear();
        for (long seat : tag.getLongArray("BaseSeats")) {
            this.baseSeats.add(BlockPos.of(seat));
        }
        this.extraSeats.clear();
        for (long seat : tag.getLongArray("ExtraSeats")) {
            this.extraSeats.add(BlockPos.of(seat));
        }
        this.nextRefill = tag.getLong("NextRefill");
        this.extrasSeated = tag.getBoolean("ExtrasSeated");
    }
}
