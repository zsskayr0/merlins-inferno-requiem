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

import dev.zsskayr.merlins_inferno.entity.PenitentEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.worldgen.structure.GreatBell;

/**
 * The Vigil Altar's memory: where the Great Bell hangs, where the Penitent appears, and when
 * the vigil may be kept again. The shrine's structure piece fills this in ({@link #configure}) when it
 * places the altar; a Penitent's death starts the cooldown ({@link #startCooldown}), and a diamond laid on
 * the altar after it ({@link #invoke}) rehangs the bell and wakes a new Penitent.
 */
public class VigilAltarBlockEntity extends BlockEntity {
    /** One in-game day (20 minutes) between a Penitent's death and the next vigil - stops the 1% drop from being spammed. */
    public static final long COOLDOWN_TICKS = 24000L;
    /** A Penitent this close to the altar counts as "already awake". */
    private static final double PENITENT_SEARCH_RADIUS = 64.0;

    private long readyAt;
    @Nullable
    private BlockPos bellPos;
    @Nullable
    private BlockPos spawnPos;

    public VigilAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.VIGIL_ALTAR.get(), pos, state);
    }

    public void configure(BlockPos bell, BlockPos spawn) {
        this.bellPos = bell.immutable();
        this.spawnPos = spawn.immutable();
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

    public boolean hasLivingPenitent(ServerLevel level) {
        return !level.getEntitiesOfClass(PenitentEntity.class, new AABB(this.worldPosition).inflate(PENITENT_SEARCH_RADIUS), Entity::isAlive).isEmpty();
    }

    /** Rehangs the Great Bell where it was broken (only in empty spots - never over something the player built). */
    public void restoreBell(ServerLevel level) {
        if (this.bellPos != null) {
            GreatBell.build(level, null, this.bellPos);
        }
    }

    /** Rehangs the bell and wakes a Penitent at the altar, with a flash of (harmless) lightning. */
    public boolean invoke(ServerLevel level) {
        if (this.spawnPos == null) {
            return false;
        }
        this.restoreBell(level);
        PenitentEntity penitent = ModEntityTypes.PENITENT.get().create(level);
        if (penitent == null) {
            return false;
        }
        penitent.moveTo(this.spawnPos.getX() + 0.5, this.spawnPos.getY(), this.spawnPos.getZ() + 0.5, 180.0F, 0.0F);
        penitent.setHome(this.worldPosition);
        penitent.finalizeSpawn(level, level.getCurrentDifficultyAt(this.worldPosition), MobSpawnType.MOB_SUMMONED, null);
        level.addFreshEntity(penitent);
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
        this.bellPos = tag.contains("Bell") ? BlockPos.of(tag.getLong("Bell")) : null;
        this.spawnPos = tag.contains("SpawnPos") ? BlockPos.of(tag.getLong("SpawnPos")) : null;
    }
}
