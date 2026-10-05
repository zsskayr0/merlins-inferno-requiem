package dev.zsskayr.merlins_inferno.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import dev.zsskayr.merlins_inferno.menu.PandoraBoxMenu;
import dev.zsskayr.merlins_inferno.registry.ModBlockEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModParticles;

/**
 * A placed Pandora Box. It opens while somebody has its screen open and closes (the open clip played backwards)
 * when the last one leaves. The server polls the players' open menus, so no close hook is needed.
 */
public class PandoraBoxBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final int EVENT_OPEN_STATE = 1;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.pandora_box.idle");
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("animation.pandora_box.open");
    private static final RawAnimation CLOSE = RawAnimation.begin().thenPlayAndHold("animation.pandora_box.close");
    /** The idle clip only plays while a player is this close. */
    private static final double IDLE_RANGE = 4.0;
    /** Length of the close clip (1.3 s) plus a little, after which the box is back at rest. */
    private static final long CLOSE_TICKS = 28;

    private enum Phase { IDLE, OPEN, CLOSE }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Server only: is the lid up. */
    private boolean open;
    /** Client only: which clip to show. */
    private Phase phase = Phase.IDLE;
    private long closedAt;
    /** Client: the idle/open/close clip is running, so the eyes glow. */
    private boolean awake;

    public PandoraBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.PANDORA_BOX.get(), pos, state);
    }

    /** Client: runes are drawn into the box while a player is close, in a flurry while it is open. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, PandoraBoxBlockEntity box) {
        boolean opened = box.phase == Phase.OPEN;
        boolean near = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, IDLE_RANGE, false) != null;
        if (!opened && !near) {
            return;
        }
        var random = level.random;
        int count = opened ? 2 : (random.nextInt(4) == 0 ? 1 : 0);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 0.9 + random.nextDouble() * 0.8;
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;
            double oy = random.nextDouble() * 1.2 - 0.2;
            level.addParticle(ModParticles.PANDORA_RUNE.get(), pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, ox, oy, oz);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PandoraBoxBlockEntity box) {
        if (level.getGameTime() % 5 == 0) {
            box.setOpen(box.hasViewer(level));
        }
    }

    private boolean hasViewer(Level level) {
        for (var player : level.players()) {
            if (player instanceof ServerPlayer && player.containerMenu instanceof PandoraBoxMenu menu
                    && this.worldPosition.equals(menu.getBoxPos())) {
                return true;
            }
        }
        return false;
    }

    /** Called when a screen is opened so the lid starts moving at once instead of at the next poll. */
    public void startOpen() {
        if (this.level != null && !this.level.isClientSide) {
            setOpen(true);
        }
    }

    private void setOpen(boolean value) {
        if (value == this.open || this.level == null) {
            return;
        }
        this.open = value;
        this.level.blockEvent(this.worldPosition, getBlockState().getBlock(), EVENT_OPEN_STATE, value ? 1 : 0);
        this.level.playSound(null, this.worldPosition, value ? SoundEvents.ENDER_CHEST_OPEN : SoundEvents.ENDER_CHEST_CLOSE,
                SoundSource.BLOCKS, 0.6F, this.level.random.nextFloat() * 0.1F + 0.9F);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == EVENT_OPEN_STATE) {
            this.phase = param == 1 ? Phase.OPEN : Phase.CLOSE;
            if (this.level != null) {
                this.closedAt = this.level.getGameTime();
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "box", 2, this::animate));
    }

    private PlayState animate(AnimationState<PandoraBoxBlockEntity> state) {
        if (this.phase == Phase.CLOSE && this.level != null && this.level.getGameTime() - this.closedAt > CLOSE_TICKS) {
            this.phase = Phase.IDLE;
        }
        this.awake = true;
        switch (this.phase) {
            case OPEN:
                return state.setAndContinue(OPEN);
            case CLOSE:
                return state.setAndContinue(CLOSE);
            default:
                boolean near = this.level != null && this.level.getNearestPlayer(this.worldPosition.getX() + 0.5,
                        this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, IDLE_RANGE, false) != null;
                this.awake = near;
                return near ? state.setAndContinue(IDLE) : PlayState.STOP;
        }
    }

    public boolean isAwake() {
        return this.awake;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
