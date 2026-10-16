package dev.zsskayr.merlins_inferno.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import dev.zsskayr.merlins_inferno.Merlins_inferno;

/** Mesh triangles are absolute model coordinates; each bone's pose rotates them about its pivot. */
public final class CinderMeshes {
    private static Map<String, float[]> cache;
    private CinderMeshes() {}
    public static void invalidate() { cache = null; }
    private static Map<String, float[]> load() {
        if (cache != null) return cache;
        Map<String, float[]> result = new HashMap<>();
        var file = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "cinder/cinder.meshes.json");
        try (var reader = Minecraft.getInstance().getResourceManager().openAsReader(file)) {
            var root = new Gson().fromJson(reader, JsonObject.class);
            for (var entry : root.entrySet()) {
                var triangles = entry.getValue().getAsJsonArray();
                float[] data = new float[triangles.size() * 18];
                for (int i = 0; i < triangles.size(); i++) for (int k = 0; k < 18; k++) data[i*18+k] = triangles.get(i).getAsJsonArray().get(k).getAsFloat();
                result.put(entry.getKey(), data);
            }
        } catch (IOException e) { throw new IllegalStateException("Cannot load Cinder meshes", e); }
        return cache = result;
    }
    public static void render(String bone, PoseStack stack, VertexConsumer buffer, int light, int overlay, int colour) {
        float[] triangles = load().get(bone);
        if (triangles == null) return;
        var pose = stack.last();
        for (int i = 0; i < triangles.length; i += 18) for (int corner = 0; corner < 4; corner++) {
            int c = Math.min(corner, 2);
            buffer.addVertex(pose, triangles[i+3+c*3], triangles[i+4+c*3], triangles[i+5+c*3]).setColor(colour)
                    .setUv(triangles[i+12+c*2], triangles[i+13+c*2]).setOverlay(overlay).setLight(light)
                    .setNormal(pose, triangles[i], triangles[i+1], triangles[i+2]);
        }
    }
}
