package dev.zsskayr.merlins_inferno.client;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * The triangles of Ostara's bones ({@code ostara/ostara.meshes.json}, baked by {@code tools/bake_ostara.py}), in the
 * model's own space (blocks, +Y up, front towards -Z) so they can be drawn with the pose of the matching GeckoLib bone.
 */
public final class OstaraMeshes {
    /** Floats per triangle: normal (3), three corners (9), three UVs (6). */
    private static final int STRIDE = 18;
    private static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "ostara/ostara.meshes.json");
    private static Map<String, float[]> byBone;

    private OstaraMeshes() {
    }

    private static Map<String, float[]> load() {
        if (byBone == null) {
            Map<String, float[]> loaded = new HashMap<>();
            try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(FILE)) {
                JsonObject root = new Gson().fromJson(reader, JsonObject.class);
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    JsonArray tris = entry.getValue().getAsJsonArray();
                    float[] data = new float[tris.size() * STRIDE];
                    for (int t = 0; t < tris.size(); t++) {
                        JsonArray tri = tris.get(t).getAsJsonArray();
                        for (int k = 0; k < STRIDE; k++) {
                            data[t * STRIDE + k] = tri.get(k).getAsFloat();
                        }
                    }
                    loaded.put(entry.getKey(), data);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + FILE, e);
            }
            byBone = loaded;
        }
        return byBone;
    }

    /** Draws the triangles of the named bone with the pose stack's current pose. */
    public static void render(String bone, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int colour) {
        float[] tris = load().get(bone);
        if (tris == null) {
            return;
        }
        PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i < tris.length; i += STRIDE) {
            // Entity render types draw quads: the last corner is repeated to make a degenerate one.
            for (int corner = 0; corner < 4; corner++) {
                int c = Math.min(corner, 2);
                consumer.addVertex(pose, tris[i + 3 + c * 3], tris[i + 4 + c * 3], tris[i + 5 + c * 3])
                        .setColor(colour)
                        .setUv(tris[i + 12 + c * 2], tris[i + 13 + c * 2])
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(pose, tris[i], tris[i + 1], tris[i + 2]);
            }
        }
    }
}
