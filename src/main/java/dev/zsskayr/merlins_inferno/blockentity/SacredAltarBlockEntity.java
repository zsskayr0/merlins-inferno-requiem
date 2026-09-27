package dev.zsskayr.merlins_inferno.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.entity.EliasEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.worldgen.structure.GreatBell;

/**
 * The Sacred Altar's memory: where the Great Bell hangs, where Elias appears, and when
 * the vigil may be kept again, and whether the Sacred Priest still guards it. The church's structure piece fills this in ({@link #configure}) when it
 * places the altar; Elias's death starts the cooldown ({@link #startCooldown}), and a diamond laid on
 * the altar after it ({@link #invoke}) rehangs the bell and wakes a new Elias.
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

    public SacredAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SACRED_ALTAR.get(), pos, state);
    }

    public void configure(BlockPos bell, BlockPos spawn, boolean priestPending) {
        this.bellPos = bell.immutable();
        this.spawnPos = spawn.immutable();
        this.priestPending = priestPending;
        this.setChanged();
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.readyAt = tag.getLong("ReadyAt");
        this.priestPending = tag.getBoolean("PriestPending");
        this.bellPos = tag.contains("Bell") ? BlockPos.of(tag.getLong("Bell")) : null;
        this.spawnPos = tag.contains("SpawnPos") ? BlockPos.of(tag.getLong("SpawnPos")) : null;
    }
}
