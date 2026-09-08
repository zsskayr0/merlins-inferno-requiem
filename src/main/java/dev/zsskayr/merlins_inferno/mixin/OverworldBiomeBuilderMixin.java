package dev.zsskayr.merlins_inferno.mixin;

import java.util.function.Consumer;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.zsskayr.merlins_inferno.worldgen.biome.ModBiomes;

/**
 * Adds Hallowed Grove to the Overworld's noise-based biome placement.
 * <p>
 * There's no data-driven way to do this in 1.21.1: which biome occupies which climate coordinate
 * in the Overworld is hardcoded Java in vanilla's (final) {@link OverworldBiomeBuilder}, and the
 * datapack-facing registry that wraps it ({@code MultiNoiseBiomeSourceParameterList}) only ever
 * serializes to a symbolic "use the vanilla overworld preset" pointer - never actual parameter
 * data - so overriding JSON can't add points either.
 * <p>
 * This Mixin injects at the tail of {@code addBiomes} (after every vanilla point has already been
 * added to the consumer) and adds a few of our own. That's the additive approach real biome mods
 * use: every mod's Mixin just contributes more points to the same shared list, so this coexists
 * with other biome mods instead of one overwriting the other's placement entirely.
 * <p>
 * <b>Datagen wrinkle:</b> {@code addBiomes} also runs inside {@code net.minecraft.data.registries
 * .VanillaRegistries}, which every mod's datapack-patch generation is built on top of as a
 * "pure vanilla" baseline (see {@code ModDataGenerators}). That baseline registry set has no
 * knowledge of {@code merlins_inferno:hallowed_grove}, so referencing it there fails registry
 * validation with an "unreferenced key" error - it's a datagen-only, no-mods build, never used by
 * actual gameplay. {@link #isVanillaOnlyRegistryBuild()} detects and skips that one case; the real
 * client/server registry build (and our own patch datagen) both go through a different path and
 * are unaffected.
 */
@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomeBuilderMixin {

    @Inject(method = "addBiomes", at = @At("TAIL"))
    private void merlinsInferno$addHallowedGrove(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer, CallbackInfo ci) {
        if (isVanillaOnlyRegistryBuild()) {
            return;
        }

        // Temperate + humid niche - the same band vanilla uses for Forest/Birch Forest/Dark
        // Forest - spread across near/mid/far inland terrain at moderate erosion (walkable hills,
        // not mountains/valleys). First-pass tuning: narrow/widen these spans after playtesting
        // if Hallowed Grove ends up too rare or too common.
        Climate.Parameter temperature = Climate.Parameter.span(-0.15F, 0.2F);
        Climate.Parameter humidity = Climate.Parameter.span(0.3F, 1.0F);
        Climate.Parameter erosion = Climate.Parameter.span(-0.2225F, 0.45F);
        Climate.Parameter depth = Climate.Parameter.point(0.0F);
        Climate.Parameter weirdness = Climate.Parameter.span(-1.0F, 1.0F);

        addPoint(consumer, temperature, humidity, Climate.Parameter.span(-0.11F, 0.03F), erosion, depth, weirdness); // near inland
        addPoint(consumer, temperature, humidity, Climate.Parameter.span(0.03F, 0.3F), erosion, depth, weirdness); // mid inland
        addPoint(consumer, temperature, humidity, Climate.Parameter.span(0.3F, 1.0F), erosion, depth, weirdness); // far inland
    }

    private static void addPoint(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer, Climate.Parameter temperature,
            Climate.Parameter humidity, Climate.Parameter continentalness, Climate.Parameter erosion, Climate.Parameter depth, Climate.Parameter weirdness) {
        consumer.accept(Pair.of(Climate.parameters(temperature, humidity, continentalness, erosion, depth, weirdness, 0.0F), ModBiomes.HALLOWED_GROVE));
    }

    private static boolean isVanillaOnlyRegistryBuild() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (frame.getClassName().equals("net.minecraft.data.registries.VanillaRegistries")) {
                return true;
            }
        }
        return false;
    }
}
