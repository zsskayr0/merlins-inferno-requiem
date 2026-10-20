package dev.zsskayr.merlins_inferno.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.zsskayr.merlins_inferno.Merlins_inferno;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;

/**
 * The enchantment glint of an active Hell Sight, in red: the vanilla glint render types with their own texture.
 * ItemRendererMixin swaps them in while that stack is being drawn. This class extends RenderType only to reach the
 * protected render state shards.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class HellSightGlint extends RenderType {
    /** True only while an active Hell Sight is being drawn (see ItemRendererMixin). */
    public static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    /** True while the stack being drawn wants the stronger glint (an active Dark Coven, stage IV and up). */
    public static final ThreadLocal<Boolean> STRONG = ThreadLocal.withInitial(() -> false);

    private static final ResourceLocation ITEM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/misc/hell_sight_glint_item.png");
    private static final ResourceLocation ENTITY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/misc/hell_sight_glint_entity.png");

    public static final RenderType GLINT = create("merlins_inferno_hell_sight_glint",
            DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_GLINT_SHADER)
                    .setTextureState(new TextureStateShard(ITEM_TEXTURE, true, false))
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .setTransparencyState(GLINT_TRANSPARENCY)
                    .setTexturingState(GLINT_TEXTURING)
                    .createCompositeState(false));

    public static final RenderType GLINT_TRANSLUCENT = create("merlins_inferno_hell_sight_glint_translucent",
            DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_GLINT_TRANSLUCENT_SHADER)
                    .setTextureState(new TextureStateShard(ITEM_TEXTURE, true, false))
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .setTransparencyState(GLINT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setTexturingState(GLINT_TEXTURING)
                    .createCompositeState(false));

    public static final RenderType ENTITY_GLINT = create("merlins_inferno_hell_sight_glint_entity",
            DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
                    .setTextureState(new TextureStateShard(ENTITY_TEXTURE, true, false))
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .setTransparencyState(GLINT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setTexturingState(ENTITY_GLINT_TEXTURING)
                    .createCompositeState(false));

    /** The same glint, much stronger and hotter: used by an active Dark Coven from stage IV on. */
    private static final ResourceLocation STRONG_ITEM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/misc/coven_glint_item.png");
    private static final ResourceLocation STRONG_ENTITY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/misc/coven_glint_entity.png");

    public static final RenderType STRONG_GLINT = create("merlins_inferno_coven_glint",
            DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_GLINT_SHADER)
                    .setTextureState(new TextureStateShard(STRONG_ITEM_TEXTURE, true, false))
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .setTransparencyState(GLINT_TRANSPARENCY)
                    .setTexturingState(GLINT_TEXTURING)
                    .createCompositeState(false));

    public static final RenderType STRONG_GLINT_TRANSLUCENT = create("merlins_inferno_coven_glint_translucent",
            DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_GLINT_TRANSLUCENT_SHADER)
                    .setTextureState(new TextureStateShard(STRONG_ITEM_TEXTURE, true, false))
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .setTransparencyState(GLINT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setTexturingState(GLINT_TEXTURING)
                    .createCompositeState(false));

    public static final RenderType STRONG_ENTITY_GLINT = create("merlins_inferno_coven_glint_entity",
            DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
                    .setTextureState(new TextureStateShard(STRONG_ENTITY_TEXTURE, true, false))
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .setTransparencyState(GLINT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setTexturingState(ENTITY_GLINT_TEXTURING)
                    .createCompositeState(false));

    /**
     * Fixed buffers are flushed after the item model, which the glint's equal-depth test needs. A render type that is
     * not registered is flushed first, so in the inventory the glint was drawn before the item and never showed.
     */
    @SubscribeEvent
    static void registerBuffers(RegisterRenderBuffersEvent event) {
        event.registerRenderBuffer(GLINT);
        event.registerRenderBuffer(GLINT_TRANSLUCENT);
        event.registerRenderBuffer(ENTITY_GLINT);
        event.registerRenderBuffer(STRONG_GLINT);
        event.registerRenderBuffer(STRONG_GLINT_TRANSLUCENT);
        event.registerRenderBuffer(STRONG_ENTITY_GLINT);
    }

    private HellSightGlint() {
        super("merlins_inferno_hell_sight_glint_holder", DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS,
                1536, false, false, () -> {}, () -> {});
    }
}
