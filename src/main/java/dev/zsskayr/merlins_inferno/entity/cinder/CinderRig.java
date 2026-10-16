package dev.zsskayr.merlins_inferno.entity.cinder;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.google.gson.Gson;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.world.phys.Vec3;

/** One pose sampler shared by the dedicated server's parts and the client renderer. */
public final class CinderRig {
    public record Bone(String name, int parent, float[] pivot) {}
    private record Clip(float length, boolean loop, int[] bones, float[][] frames) {}
    private record File(float scale, Bone[] bones, int[] parts, Map<String, Clip> clips) {}
    private static final File DATA = load();
    private CinderRig() {}
    private static File load() {
        try (var reader = new InputStreamReader(Objects.requireNonNull(CinderRig.class.getResourceAsStream(
                "/assets/merlins_inferno/cinder/cinder.rig.json")), StandardCharsets.UTF_8)) {
            File file = new Gson().fromJson(reader, File.class);
            if (file.parts.length != 32 || file.clips.size() != 10) throw new IllegalStateException("Incomplete Cinder rig");
            return file;
        } catch (Exception e) { throw new IllegalStateException("Cannot load shared Cinder rig", e); }
    }
    public static Bone[] bones() { return DATA.bones; }
    public static float scale() { return DATA.scale; }
    public static int partCount() { return DATA.parts.length; }

    public static float[] sample(CinderAction action, double ticks) {
        Clip c = DATA.clips.get(action.clip);
        double frame = c.loop ? Math.max(0, ticks) % (c.frames.length - 1) : Math.max(0, Math.min(c.frames.length - 1, ticks));
        int a = (int) frame, b = Math.min(a + 1, c.frames.length - 1);
        float alpha = (float)(frame - a);
        float[] result = new float[DATA.bones.length * 6];
        for (int i = 0; i < c.bones.length; i++) {
            for (int axis = 0; axis < 6; axis++) {
                int index = i * 6 + axis;
                result[c.bones[i] * 6 + axis] = c.frames[a][index] + (c.frames[b][index] - c.frames[a][index]) * alpha;
            }
        }
        return result;
    }

    public static float[] pose(CinderAction action, double elapsed, int coldTicks, boolean onLand) {
        float[] pose = sample(action, action == CinderAction.COOLING ? coldTicks : elapsed);
        // A late land bite or recoil must not reset the cold clock or raise a dying head.
        if (onLand && (action == CinderAction.LAND_ATTACK || action == CinderAction.HURT)) {
            float[] baseline = sample(action == CinderAction.HURT ? CinderAction.SWIM : CinderAction.LAND_IDLE, 0);
            float[] cold = sample(CinderAction.COOLING, coldTicks);
            for (int i = 0; i < pose.length; i++) pose[i] += cold[i] - baseline[i];
        }
        return pose;
    }

    public static Vec3[] parts(float[] pose) {
        Matrix4f[] transforms = new Matrix4f[DATA.bones.length];
        for (int i = 0; i < DATA.bones.length; i++) {
            Bone bone = DATA.bones[i];
            float[] parentPivot = bone.parent < 0 ? new float[3] : DATA.bones[bone.parent].pivot;
            int k = i * 6;
            Matrix4f matrix = bone.parent < 0 ? new Matrix4f() : new Matrix4f(transforms[bone.parent]);
            matrix.translate(bone.pivot[0] - parentPivot[0] + pose[k],
                    bone.pivot[1] - parentPivot[1] + pose[k + 1], bone.pivot[2] - parentPivot[2] + pose[k + 2]);
            matrix.rotateZYX((float)Math.toRadians(pose[k + 5]), (float)Math.toRadians(pose[k + 4]), (float)Math.toRadians(pose[k + 3]));
            transforms[i] = matrix;
        }
        Vec3[] points = new Vec3[DATA.parts.length];
        for (int i = 0; i < points.length; i++) {
            // Snout rather than skull hinge is the biting point.
            Vector3f v = transforms[DATA.parts[i]].transformPosition(new Vector3f(0, i == 0 ? -1 : 0, i == 0 ? -4 : 0));
            points[i] = new Vec3(v.x, v.y, v.z).scale(DATA.scale / 16.0);
        }
        return points;
    }
}
