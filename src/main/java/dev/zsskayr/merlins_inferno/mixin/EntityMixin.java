package dev.zsskayr.merlins_inferno.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.zsskayr.merlins_inferno.client.EdenweedSight;

/**
 * Colours the glow outline The Sight puts on creatures: the outline takes its colour from the entity's team, and an entity
 * The Sight reveals for the local player gets the Sight's colour instead. Only asked on the client, when rendering the outline.
 */
@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void merlins_inferno$sightColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide && EdenweedSight.revealsEntity(self)) {
            cir.setReturnValue(EdenweedSight.outlineColor(self));
        }
    }
}
