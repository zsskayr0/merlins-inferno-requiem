package dev.zsskayr.merlins_inferno.client;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.block.GateBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

/** Wraps the models of blocks with connected textures. The sheets are stitched through assets/minecraft/atlases/blocks.json. */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class ConnectedTextures {
    private ConnectedTextures() {}

    @SubscribeEvent
    public static void onBake(ModelEvent.ModifyBakingResult event) {
        wrap(event, ModBlocks.POLISHED_BLIGHTSTONE.get(), "block/polished_blightstone_connected");
        wrap(event, ModBlocks.POLISHED_PORCELAIN.get(), "block/polished_porcelain_connected");
        wrap(event, ModBlocks.TILED_PORCELAIN.get(), "block/tiled_porcelain_connected", true);
        wrap(event, ModBlocks.CRYPT_GLASS.get(), "block/crypt_glass_connected");
        wrap(event, ModBlocks.HELLGLASS.get(), "block/hellglass_connected");
        wrap(event, ModBlocks.BLIGHTSTEEL_CASING.get(), "block/blightsteel_casing_connected");
        wrap(event, ModBlocks.COMPRESSED_NETHERRACK.get(), "block/netherrack_compressed_connected");
        wrapPane(event, ModBlocks.CRYPT_GLASS_PANE.get(), "block/crypt_glass_pane_connected", "block/crypt_glass_pane");
        wrapPane(event, ModBlocks.HELLGLASS_PANE.get(), "block/hellglass_connected", "block/hellglass");
        wrapGate(event, ModBlocks.RUNIC_GATE.get());
        wrapGate(event, ModBlocks.MUNDANE_GATE.get());
        wrapGate(event, ModBlocks.DEVIL_GATE.get());
        wrapGate(event, ModBlocks.ANGEL_GATE.get());
        wrapGate(event, ModBlocks.FAERIC_GATE.get());
        vanillaTranslucentItem(event, ModBlocks.CRYPT_GLASS.getId());
        vanillaTranslucentItem(event, ModBlocks.HELLGLASS.getId());
    }

    /**
     * A model with {@code render_type: translucent} gets NeoForge's own layered item type, which shader packs do not
     * know and draw as solid in the hand. Vanilla stained glass has no such hint and uses the standard translucent
     * sheet, so the glass items are asked for that one as well.
     */
    private static void vanillaTranslucentItem(ModelEvent.ModifyBakingResult event, ResourceLocation id) {
        ModelResourceLocation key = ModelResourceLocation.inventory(id);
        BakedModel model = event.getModels().get(key);
        if (model != null) {
            event.getModels().put(key, new BakedModelWrapper<>(model) {
                @Override
                public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
                    return List.of(RenderTypeHelper.getEntityRenderType(RenderType.translucent(), fabulous));
                }
            });
        }
    }

    /** A pane shows the glass's own sheet on its faces; only the quads with the glass texture are remapped, not the edge. */
    private static void wrapPane(ModelEvent.ModifyBakingResult event, Block block, String sheetPath, String facePath) {
        var sheet = event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, sheetPath)));
        var face = event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, facePath)));
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
            BakedModel model = event.getModels().get(key);
            if (model != null) {
                event.getModels().put(key, new ConnectedBlockModel(model, sheet, face));
            }
        }
    }

    /**
     * A gate joins its neighbours at every stage of its look, with the sheet of that stage (stone, on to haze, a ghost; only the
     * quads with the stage's plain tile are remapped), and carries an animated rune on every face that dims with the stone.
     */
    private static void wrapGate(ModelEvent.ModifyBakingResult event, Block gate) {
        String name = BuiltInRegistries.BLOCK.getKey(gate).getPath();
        var sprites = event.getTextureGetter();
        java.util.function.Function<String, net.minecraft.client.renderer.texture.TextureAtlasSprite> sprite = path ->
                sprites.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "block/" + path)));
        var runes = sprite.apply(name + "_runes");
        for (BlockState state : gate.getStateDefinition().getPossibleStates()) {
            int stage = GateBlock.stage(state);
            String tile = stage == 0 ? name : stage == GateBlock.GHOST_STAGE ? name + "_ghost" : name + "_stage" + stage;
            ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
            BakedModel model = event.getModels().get(key);
            if (model != null) {
                var joined = new ConnectedBlockModel(model, sprite.apply(tile + "_connected"), sprite.apply(tile));
                var framed = new ConnectedBlockModel(joined, sprite.apply(name + "_frame_connected"), sprite.apply(name + "_frame"));
                event.getModels().put(key, new GateRuneModel(framed, runes, stage));
            }
        }
    }

    private static void wrap(ModelEvent.ModifyBakingResult event, Block block, String sheetPath) {
        wrap(event, block, sheetPath, false);
    }

    private static void wrap(ModelEvent.ModifyBakingResult event, Block block, String sheetPath, boolean pairs) {
        var sheet = event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS,
                ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, sheetPath)));
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
            BakedModel model = event.getModels().get(key);
            if (model != null) {
                event.getModels().put(key, new ConnectedBlockModel(model, sheet, null, pairs));
            }
        }
    }
}
