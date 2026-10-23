package dev.zsskayr.merlins_inferno.compat.epicfight;

import dev.zsskayr.merlins_inferno.client.WingsLayer;
import dev.zsskayr.merlins_inferno.item.WingsItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EntityType;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

/**
 * Epic Fight draws the player with its own skinned model in battle mode and skips every vanilla layer it has no patched
 * counterpart for, so the wings vanished. This registers a patched {@link WingsLayer} that places the same elytra model
 * on the animated chest joint, exactly like Epic Fight's own patched elytra layer. Only loaded when Epic Fight is present.
 */
public final class EpicFightWingsCompat {
    /** The armature's chest joint in the pose array, as Epic Fight's patched elytra layer uses it. */
    private static final int CHEST_JOINT = 8;

    private EpicFightWingsCompat() {}

    public static void register() {
        EpicFightClientEventHooks.Registry.MODIFY_PATCHED_ENTITY.registerEvent(event -> {
            if (event.get(EntityType.PLAYER) instanceof PPlayerRenderer renderer) {
                renderer.addPatchedLayer(WingsLayer.class, new PatchedWingsLayer());
            }
        });
    }

    private static final class PatchedWingsLayer extends PatchedLayer<AbstractClientPlayer,
            AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, WingsLayer> {
        @Override
        protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> patch, AbstractClientPlayer player,
                                   WingsLayer layer, PoseStack pose, MultiBufferSource buffer, int light, OpenMatrix4f[] poses,
                                   float ageInTicks, float netHeadYaw, float headPitch, float partialTick) {
            if (!(WingsItem.findVisible(player).getItem() instanceof WingsItem)) return;
            OpenMatrix4f transform = new OpenMatrix4f()
                    .scale(new Vec3f(-0.9F, -0.9F, 0.9F))
                    .translate(new Vec3f(0.0F, -0.5F, -0.1F))
                    .mulFront(poses[CHEST_JOINT]);
            pose.pushPose();
            MathUtils.mulStack(pose, transform);
            layer.render(pose, buffer, light, player, player.walkAnimation.position(), player.walkAnimation.speed(),
                    partialTick, ageInTicks, netHeadYaw, headPitch);
            pose.popPose();
        }
    }
}
