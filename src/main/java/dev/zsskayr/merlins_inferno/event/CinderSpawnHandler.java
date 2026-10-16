package dev.zsskayr.merlins_inferno.event;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.entity.CinderEntity;
import dev.zsskayr.merlins_inferno.portal.OblivionDimension;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Lava is the rarity gate: no biome modifier that accidentally populates the Nether. */
public final class CinderSpawnHandler {
    private static final String NEXT_SPAWN = "MerlinsCinderNextSpawn";
    @SubscribeEvent public void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != OblivionDimension.LEVEL
                || level.getDifficulty() == Difficulty.PEACEFUL || level.getGameTime() % 200 != 0
                || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) return;
        for (var player : level.players()) {
            if (player.isCreative() || player.isSpectator() || !ProgressionHelper.hasReached(player, ProgressionHelper.SECOND_CIRCLE)
                    || player.getPersistentData().getLong(NEXT_SPAWN) > level.getGameTime() || level.random.nextInt(9) != 0) continue;
            for (int attempt = 0; attempt < 24; attempt++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double radius = 32 + level.random.nextDouble() * 32;
                int x = player.getBlockX() + (int)(Math.cos(angle)*radius), z = player.getBlockZ() + (int)(Math.sin(angle)*radius);
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                for (int y = Math.min(level.getMaxBuildHeight()-4, player.getBlockY()+16); y >= Math.max(level.getMinBuildHeight()+4,player.getBlockY()-72); y--) {
                    BlockPos surface = new BlockPos(x,y,z);
                    if (!level.getFluidState(surface).is(FluidTags.LAVA) || level.getFluidState(surface.above()).is(FluidTags.LAVA)) continue;
                    BlockPos pos = surface.below();
                    if (!CinderEntity.checkCinderSpawnRules(ModEntityTypes.CINDER.get(), level, MobSpawnType.NATURAL, pos, level.random)) break;
                    if (level.getNearestPlayer(x+.5, y, z+.5, 32, false) != null) break;
                    var nearby = level.getEntitiesOfClass(CinderEntity.class, new AABB(pos).inflate(96), Entity -> Entity.isAlive());
                    if (nearby.size() >= 2 || nearby.stream().anyMatch(e -> e.distanceToSqr(x+.5,pos.getY(),z+.5) < 48*48)) break;
                    float yaw = level.random.nextFloat() * 360;
                    if (!fitsLava(level,pos,yaw)) break;
                    CinderEntity cinder = ModEntityTypes.CINDER.get().create(level);
                    if (cinder == null) return;
                    cinder.moveTo(x+.5, pos.getY()-.2, z+.5, yaw, 0);
                    cinder.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
                    if (!level.noCollision(cinder)) { cinder.discard(); break; }
                    level.addFreshEntity(cinder);
                    player.getPersistentData().putLong(NEXT_SPAWN, level.getGameTime()+6000+level.random.nextInt(3601));
                    attempt = 24;
                    break;
                }
            }
        }
    }
    private static boolean fitsLava(ServerLevel level, BlockPos pos, float yaw) {
        double angle = Math.toRadians(yaw);
        for (int i = 0; i <= 15; i++) {
            BlockPos sample = pos.offset((int)(Math.sin(angle)*i), 0, -(int)(Math.cos(angle)*i));
            if (!level.hasChunkAt(sample) || !level.getFluidState(sample).is(FluidTags.LAVA)
                    || !level.getFluidState(sample.below()).is(FluidTags.LAVA)) return false;
        }
        return true;
    }
}
