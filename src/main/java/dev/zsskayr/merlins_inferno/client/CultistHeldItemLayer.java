package dev.zsskayr.merlins_inferno.client;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import dev.zsskayr.merlins_inferno.entity.SacredCultistEntity;

/**
 * Shows the sword some cultists spawn with (30% iron, 8% Seraphium; see {@code SacredCultistEntity#finalizeSpawn})
 * in its right hand - the model's terminal right-arm bone, one level under the elbow. GeckoLib's default item
 * placement is used as-is; the offset has not been checked against an in-game screenshot yet.
 */
public class CultistHeldItemLayer extends BlockAndItemGeoLayer<SacredCultistEntity> {
    private static final String RIGHT_HAND = "Right Arm";

    public CultistHeldItemLayer(GeoRenderer<SacredCultistEntity> renderer) {
        super(renderer);
    }

    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, SacredCultistEntity cultist) {
        return RIGHT_HAND.equals(bone.getName()) ? cultist.getMainHandItem() : null;
    }
}
