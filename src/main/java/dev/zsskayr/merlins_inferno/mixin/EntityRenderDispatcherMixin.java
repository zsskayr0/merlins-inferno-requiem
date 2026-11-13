package dev.zsskayr.merlins_inferno.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.zsskayr.merlins_inferno.client.SoulFire;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Entities burning from Soul Lava show blue flames in the world. */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @ModifyExpressionValue(method = "renderFlame", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/Material;sprite()Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;", ordinal = 0))
    private TextureAtlasSprite merlins_inferno$soulFire0(TextureAtlasSprite original, @Local(argsOnly = true) Entity entity) {
        return SoulFire.burning(entity) ? SoulFire.sprite(0) : original;
    }

    @ModifyExpressionValue(method = "renderFlame", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/Material;sprite()Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;", ordinal = 1))
    private TextureAtlasSprite merlins_inferno$soulFire1(TextureAtlasSprite original, @Local(argsOnly = true) Entity entity) {
        return SoulFire.burning(entity) ? SoulFire.sprite(1) : original;
    }
}
