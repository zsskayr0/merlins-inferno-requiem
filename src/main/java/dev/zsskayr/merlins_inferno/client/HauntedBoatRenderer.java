package dev.zsskayr.merlins_inferno.client;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/** Vanilla oak boat model with the Haunted wood texture. */
public class HauntedBoatRenderer extends BoatRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "textures/entity/boat/haunted.png");

    public HauntedBoatRenderer(EntityRendererProvider.Context context) {
        super(context, false);
    }

    @Override
    public Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat boat) {
        return Pair.of(TEXTURE, super.getModelWithLocation(boat).getSecond());
    }
}
