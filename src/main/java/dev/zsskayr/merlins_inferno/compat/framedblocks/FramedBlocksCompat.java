package dev.zsskayr.merlins_inferno.compat.framedblocks;

import java.lang.reflect.Method;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.client.ConnectedBlockModel;

/**
 * Soft bridge to Framed Blocks (reflection, no compile dependency) so connected textures work on a camouflaged block.
 * Framed registers one model property per mod through the {@code add_ct_property} message and feeds it back to the
 * camouflage's model; {@link #resolve} lets that model see a Framed block as the block it is camouflaged as, so Framed
 * blocks wearing the same block connect to each other. Without Framed, or if its API changed, nothing changes.
 */
public final class FramedBlocksCompat {
    private static final String MOD_ID = "framedblocks";
    private static final String FRAMED_BLOCK = "xfacthd.framedblocks.api.block.IFramedBlock";

    private static boolean resolved;
    private static boolean available;
    private static Class<?> framedBlock;
    private static Method getCamo;
    private static Method getContent;
    private static Method getAsBlockState;

    private FramedBlocksCompat() {
    }

    public static void enqueue(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded(MOD_ID)) {
            InterModComms.sendTo(MOD_ID, "add_ct_property", () -> ConnectedBlockModel.NEIGHBOURS);
        }
    }

    /** The block a Framed block is camouflaged as; every other state comes back untouched. */
    public static BlockState resolve(BlockGetter level, BlockPos pos, BlockState state) {
        if (!ModList.get().isLoaded(MOD_ID) || !resolve() || !framedBlock.isInstance(state.getBlock())) {
            return state;
        }
        try {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                return state;
            }
            Object camo = getCamo.invoke(entity);
            Object content = camo == null ? null : getContent.invoke(camo);
            Object camoState = content == null ? null : getAsBlockState.invoke(content);
            return camoState instanceof BlockState resolved ? resolved : state;
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
            framedBlock = Class.forName(FRAMED_BLOCK);
            getCamo = Class.forName("xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity").getMethod("getCamo");
            getContent = Class.forName("xfacthd.framedblocks.api.camo.CamoContainer").getMethod("getContent");
            getAsBlockState = Class.forName("xfacthd.framedblocks.api.camo.CamoContent").getMethod("getAsBlockState");
            available = true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            org.slf4j.LoggerFactory.getLogger(Merlins_inferno.class).warn("Framed Blocks bridge unavailable", exception);
        }
        return available;
    }
}
