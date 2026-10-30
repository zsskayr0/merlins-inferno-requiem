package dev.zsskayr.merlins_inferno.mixin;

import java.util.List;

import dev.zsskayr.merlins_inferno.client.ConnectedBlockModel;
import dev.zsskayr.merlins_inferno.compat.ae2.Ae2FacadeCompat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE2 asks the facade's source block for quads with empty model data; for a connected-texture block this asks the model
 * for its data first, from the facade's own view (see {@link Ae2FacadeCompat}). Optional: without AE2 the target never loads.
 */
@Mixin(targets = "appeng.client.render.cablebus.FacadeBuilder", remap = false)
public class Ae2FacadeBuilderMixin {
    private static final String MESH = "getFacadeMesh(Lappeng/client/render/cablebus/CableBusRenderState;Ljava/util/function/Supplier;"
            + "Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/EnumMap;Lnet/minecraft/client/renderer/RenderType;)Lappeng/thirdparty/fabric/Mesh;";

    @Redirect(method = MESH, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BakedModel;getQuads("
            + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;"
            + "Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)Ljava/util/List;"), require = 0)
    private List<BakedQuad> merlins_inferno$quads(BakedModel model, BlockState state, Direction face, RandomSource random, ModelData data, RenderType type) {
        Ae2FacadeCompat.Context context = Ae2FacadeCompat.context();
        if (context != null && model instanceof ConnectedBlockModel) {
            data = model.getModelData(context.level(), context.pos(), state, data);
        }
        return model.getQuads(state, face, random, data, type);
    }

    @Inject(method = MESH, at = @At("RETURN"), require = 0)
    private void merlins_inferno$forget(CallbackInfoReturnable<?> cir) {
        Ae2FacadeCompat.end();
    }
}
