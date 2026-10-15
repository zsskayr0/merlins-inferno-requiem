package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Soul Shroomlight: glows cyan and sheds the odd wisp of soul from its open faces. */
public class SoulShroomlightBlock extends Block {
    public SoulShroomlightBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) {
            return;
        }
        Direction face = Direction.getRandom(random);
        BlockPos side = pos.relative(face);
        if (level.getBlockState(side).isSolidRender(level, side)) {
            return;
        }
        double x = pos.getX() + 0.5 + face.getStepX() * 0.55 + (face.getAxis() == Direction.Axis.X ? 0.0 : (random.nextDouble() - 0.5) * 0.9);
        double y = pos.getY() + 0.5 + face.getStepY() * 0.55 + (face.getAxis() == Direction.Axis.Y ? 0.0 : (random.nextDouble() - 0.5) * 0.9);
        double z = pos.getZ() + 0.5 + face.getStepZ() * 0.55 + (face.getAxis() == Direction.Axis.Z ? 0.0 : (random.nextDouble() - 0.5) * 0.9);
        level.addParticle(ParticleTypes.SOUL, x, y, z, 0.0, 0.02, 0.0);
    }
}
