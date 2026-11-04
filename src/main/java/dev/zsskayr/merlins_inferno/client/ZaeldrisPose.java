package dev.zsskayr.merlins_inferno.client;

import java.util.HashSet;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * While Zaeldris' barrage is held the wielder holds the lance back over the shoulder, like a trident about to be thrown: the
 * vanilla throw pose in third person; in first person the lance is drawn up and back over {@value #CHARGE_TICKS} ticks
 * from the press. The local player's own state comes from the key ({@link ZaeldrisKeys}); everyone else's from the
 * server ({@code network.ZaeldrisPosePayload}). Purely visual: the lance is never "in use", so nobody is slowed.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class ZaeldrisPose implements IClientItemExtensions {
    public static final ZaeldrisPose INSTANCE = new ZaeldrisPose();
    private static final float CHARGE_TICKS = 10.0F;
    /** How far the first-person lance is drawn up and back at full charge (screen units). */
    private static final float DRAW_UP = 0.02F;
    private static final float DRAW_BACK = 0.18F;
    private static final Set<Integer> BARRAGING = new HashSet<>();

    private ZaeldrisPose() {}

    public static void set(int entityId, boolean barraging) {
        if (barraging) {
            BARRAGING.add(entityId);
        } else {
            BARRAGING.remove(entityId);
        }
    }

    public static boolean isBarraging(LivingEntity entity) {
        return entity == Minecraft.getInstance().player ? ZaeldrisKeys.isBarraging() : BARRAGING.contains(entity.getId());
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        BARRAGING.clear();
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return hand == InteractionHand.MAIN_HAND && isBarraging(entity) ? HumanoidModel.ArmPose.THROW_SPEAR : null;
    }

    /**
     * First person: the throw model raises the lance over the shoulder, pointing ahead past the crosshair, and this draws it
     * back as the barrage builds. The trident's own charge transform is made for its 3D model and buried this flat lance in
     * the camera, so it is not used here.
     */
    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack,
            float partialTick, float equipProcess, float swingProcess) {
        if (!ZaeldrisKeys.isBarraging() || player.getMainArm() != arm) {
            return false;
        }
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        // Vanilla's resting hand (ItemInHandRenderer#applyItemArmTransform), then the draw-back.
        poseStack.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
        // The raised overhead pose itself is the throw model's first-person display (models/item/zaeldris_hand_throw.json);
        // here the lance is only drawn back along its line, then trembles once fully charged, like the trident.
        float held = ZaeldrisKeys.heldTicks(partialTick);
        float charge = Math.min(1.0F, held / CHARGE_TICKS);
        poseStack.translate(0.0F, DRAW_UP * charge, DRAW_BACK * charge);
        if (charge >= 1.0F) {
            poseStack.translate(0.0F, Mth.sin((held - CHARGE_TICKS) * 1.3F) * 0.004F, 0.0F);
        }
        return true;
    }
}
