package dev.zsskayr.merlins_inferno.client;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Wraps the models of blocks with connected textures. The sheets are stitched through assets/minecraft/atlases/blocks.json. */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class ConnectedTextures {
    private ConnectedTextures() {}

    @SubscribeEvent
    public static void onBake(ModelEvent.ModifyBakingResult event) {
        wrap(event, ModBlocks.POLISHED_BLIGHTSTONE.get(), "block/polished_blightstone_connected");
    }

    private static void wrap(ModelEvent.ModifyBakingResult event, Block block, String sheetPath) {
        var sheet = event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, sheetPath)));
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
            BakedModel model = event.getModels().get(key);
            if (model != null) {
                event.getModels().put(key, new ConnectedBlockModel(model, sheet));
            }
        }
    }
}
