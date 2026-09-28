package dev.zsskayr.merlins_inferno.event;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import dev.zsskayr.merlins_inferno.compat.lootr.LootrCompat;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * A boss's loot goes into a chest where it fell, instead of scattering on the ground - the way Twilight Forest's
 * bosses leave a treasure chest. The chest is a Lootr chest, so every player opens their own instance of the loot
 * ({@link LootrCompat}); without Lootr installed no chest is made and the loot drops on the ground as usual.
 * <p>
 * Which entities count is data: the {@code merlins_inferno:reward_chest_bosses} entity-type tag (other mods' bosses
 * can be added). The chest's table is {@code <entity namespace>:chests/boss/<entity>}; ours simply reference the
 * boss's own entity table, so its loot is defined once. If Lootr is missing, the boss has no such table, or there is no
 * room for a chest, the drops fall as usual - the loot is never lost.
 */
public class BossRewardChestHandler {
    private static final int SEARCH_RADIUS = 3;

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        LivingEntity boss = event.getEntity();
        if (!(boss.level() instanceof ServerLevel level) || !boss.getType().is(ModTags.EntityTypes.REWARD_CHEST_BOSSES)) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType());
        ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "chests/boss/" + id.getPath()));
        if (level.getServer().reloadableRegistries().getLootTable(table) == LootTable.EMPTY) {
            return;
        }
        Optional<Block> lootrChest = LootrCompat.rewardChest();
        if (lootrChest.isEmpty()) {
            return; // no Lootr: the loot drops as usual
        }
        BlockPos spot = findSpot(level, boss.blockPosition());
        if (spot == null) {
            return;
        }

        BlockState chest = lootrChest.get().defaultBlockState();
        if (chest.hasProperty(ChestBlock.FACING)) {
            chest = chest.setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(level.random));
        }
        level.setBlock(spot, chest, Block.UPDATE_ALL);
        RandomizableContainer.setBlockEntityLootTable(level, level.random, spot, table);
        level.sendParticles(ParticleTypes.END_ROD, spot.getX() + 0.5, spot.getY() + 0.8, spot.getZ() + 0.5, 24, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, spot, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.8F);
        event.setCanceled(true); // the chest holds the loot now
    }

    /** The free spot nearest the death (same layer first, then one up/down), or {@code null} if there is none. */
    private static BlockPos findSpot(ServerLevel level, BlockPos origin) {
        for (int r = 0; r <= SEARCH_RADIUS; r++) {
            for (int dy : new int[] {0, -1, 1}) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        BlockPos p = origin.offset(dx, dy, dz);
                        if (canHoldChest(level, p)) {
                            return p;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean canHoldChest(ServerLevel level, BlockPos p) {
        if (!level.isInWorldBounds(p)) {
            return false;
        }
        BlockState state = level.getBlockState(p);
        BlockPos below = p.below();
        if (!state.canBeReplaced() || !state.getFluidState().isEmpty() || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                || !level.getFluidState(below).isEmpty()) {
            return false;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) { // keep clear of other chests, so it never merges into a double
            if (level.getBlockState(p.relative(side)).getBlock() instanceof ChestBlock) {
                return false;
            }
        }
        return true;
    }
}
