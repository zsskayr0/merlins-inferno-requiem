package dev.zsskayr.merlins_inferno.blockentity;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.entity.EliasEntity;
import dev.zsskayr.merlins_inferno.entity.SacredCultistEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.worldgen.structure.ChurchCongregation;
import dev.zsskayr.merlins_inferno.worldgen.structure.GreatBell;

/**
 * The Sacred Altar's memory: where the Great Bell hangs, where Elias appears, and when
 * the vigil may be kept again, and whether the Sacred Priest still guards it. The church's structure piece fills this in ({@link #configure}) when it
 * places the altar; Elias's death starts the cooldown ({@link #startCooldown}), and a diamond laid on
 * the altar after it ({@link #invoke}) rehangs the bell and wakes a new Elias.
 * <p>
 * It also keeps the congregation: every seven in-game days it refills the pews that stand empty
 * ({@link ChurchCongregation}), and the first time a Circle 2 player is near it seats the extra row of cultists.
 */
public class SacredAltarBlockEntity extends BlockEntity {
    /** One in-game day (20 minutes) between Elias's death and the next vigil - stops the 1% drop from being spammed. */
    public static final long COOLDOWN_TICKS = 24000L;
    /** A Elias this close to the altar counts as "already awake". */
    private static final double ELIAS_SEARCH_RADIUS = 64.0;

    private long readyAt;
    @Nullable
    private BlockPos bellPos;
    @Nullable
    private BlockPos spawnPos;
    /** True from the church's generation until its Sacred Priest falls: Elias cannot be woken meanwhile. */
    private boolean priestPending;

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

    /** Rehangs the bell and wakes Elias at the altar, with a flash of (harmless) lightning. */
    public boolean invoke(ServerLevel level) {
        if (this.spawnPos == null) {
            return false;
        }
        this.restoreBell(level);
        EliasEntity elias = ModEntityTypes.ELIAS.get().create(level);
        if (elias == null) {
            return false;
        }
        elias.moveTo(this.spawnPos.getX() + 0.5, this.spawnPos.getY(), this.spawnPos.getZ() + 0.5, 180.0F, 0.0F);
        elias.setHome(this.worldPosition);
        elias.finalizeSpawn(level, level.getCurrentDifficultyAt(this.worldPosition), MobSpawnType.MOB_SUMMONED, null);
        level.addFreshEntity(elias);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(this.worldPosition));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("ReadyAt", this.readyAt);
        tag.putBoolean("PriestPending", this.priestPending);
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
