package dev.zsskayr.merlins_inferno.compat.ae2;

import java.lang.reflect.Method;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * Soft bridge to Applied Energistics 2 (reflection, no compile dependency) so connected textures work on facades.
 * AE2 draws a facade from the source block's model with empty model data, so the connection data never exists; the
 * mixins remember which facade is being drawn ({@link Context}) and ask the model for its data with it, and
 * {@link #resolve} lets that model see a neighbouring cable's facade on the same side as the block it shows.
 * Without AE2, or if its API changed, nothing changes.
 */
public final class Ae2FacadeCompat {
    private static final String MOD_ID = "ae2";
    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    private static boolean resolved;
    private static boolean available;
    private static Class<?> cableBus;
    private static Method getFacadeContainer;
    private static Method getFacade;
    private static Method getBlockState;

    private Ae2FacadeCompat() {
    }

    /** The facade being drawn: AE2's block view (it answers with the facade's state at {@code pos}) and its side. */
    public record Context(BlockAndTintGetter level, BlockPos pos, Direction side) {
    }

    public static void begin(Context context) {
        CONTEXT.set(context);
    }

    public static void end() {
        CONTEXT.remove();
    }

    public static Context context() {
        return CONTEXT.get();
    }

    /** The source block of the facade a cable shows on the side being drawn; every other state comes back untouched. */
    public static BlockState resolve(BlockGetter level, BlockPos pos, BlockState state) {
        Context context = CONTEXT.get();
        if (context == null || !ModList.get().isLoaded(MOD_ID) || !resolve()) {
            return state;
        }
        try {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null || !cableBus.isInstance(entity)) {
                return state;
            }
            Object facade = getFacade.invoke(getFacadeContainer.invoke(entity), context.side());
            return facade != null && getBlockState.invoke(facade) instanceof BlockState source ? source : state;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException | NullPointerException exception) {
            return state;
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return available;
        }
        resolved = true;
        try {
            cableBus = Class.forName("appeng.blockentity.networking.CableBusBlockEntity");
            getFacadeContainer = cableBus.getMethod("getFacadeContainer");
            getFacade = Class.forName("appeng.api.parts.IFacadeContainer").getMethod("getFacade", Direction.class);
            getBlockState = Class.forName("appeng.api.parts.IFacadePart").getMethod("getBlockState");
            available = true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            org.slf4j.LoggerFactory.getLogger(Merlins_inferno.class).warn("Applied Energistics 2 bridge unavailable", exception);
        }
        return available;
    }
}
