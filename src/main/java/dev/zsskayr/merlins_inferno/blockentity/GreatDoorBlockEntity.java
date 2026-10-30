package dev.zsskayr.merlins_inferno.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import dev.zsskayr.merlins_inferno.block.GreatDoorBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;

/**
 * Sits in the bottom middle cell of a {@link GreatDoorBlock} and draws the whole door (the other cells are invisible).
 * The two leaves swing open and shut; the clip follows the {@code open} property of the block.
 */
public class GreatDoorBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("animation.great_door.open");
    private static final RawAnimation CLOSE = RawAnimation.begin().thenPlayAndHold("animation.great_door.close");
    private static final RawAnimation OPENED = RawAnimation.begin().thenPlayAndHold("animation.great_door.opened");
    private static final RawAnimation CLOSED = RawAnimation.begin().thenPlayAndHold("animation.great_door.closed");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Client: the state the door was last drawn in; null until it is drawn the first time (it then starts at rest, no swing). */
    private Boolean shownOpen;

    public GreatDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.GREAT_DOOR.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "door", 0, state -> {
            boolean open = getBlockState().getValue(GreatDoorBlock.OPEN);
            if (shownOpen == null) {
                shownOpen = open;
                state.setAnimation(open ? OPENED : CLOSED);
            } else if (shownOpen != open) {
                shownOpen = open;
                state.setAnimation(open ? OPEN : CLOSE);
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
