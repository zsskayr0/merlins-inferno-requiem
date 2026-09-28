package dev.zsskayr.merlins_inferno.client;

import dev.zsskayr.merlins_inferno.menu.PandoraLayout;

/** Frame-time interpolation of server snapshots. Never predicts beyond the last received progress. */
public final class PandoraAnimation {
    private long lastFrame = -1, snapshotTime, completionTime = -1;
    private int completionSerial;
    private boolean crafting;
    private float fromProgress, targetProgress, progress;
    private double angle = PandoraLayout.INITIAL_ANGLE;
    private float flash, contraction;

    public void update(long now, boolean active, float serverProgress, int serial, boolean pauseIdle, boolean touchscreen) {
        double dt = lastFrame < 0 ? 0 : Math.max(0, Math.min(0.1, (now - lastFrame) / 1000.0));
        lastFrame = now;
        if (serial != completionSerial) {
            completionSerial = serial;
            completionTime = now;
        }
        if (active) {
            if (!crafting) {
                fromProgress = 0;
                targetProgress = serverProgress;
                snapshotTime = now;
                completionTime = -1;
            } else if (serverProgress != targetProgress) {
                fromProgress = sample(now);
                targetProgress = serverProgress;
                snapshotTime = now;
            }
            progress = sample(now);
        } else progress = 0;
        crafting = active;
        float age = completionTime < 0 ? 1 : (now - completionTime) / 650.0F;
        flash = 1 - smooth(age);
        contraction = active ? smooth((progress - 0.08F) / 0.86F) : 1 - smooth(age * 1.5F);
        // Hover pauses only the idle orbit. Forging always keeps rotating, even with the pointer over it.
        if (active || flash > 0 || (!pauseIdle && !touchscreen)) {
            double speed = active ? 1.0 + progress * 5.0 : Math.PI * 2 / (PandoraLayout.ORBIT_PERIOD_MS / 1000.0);
            angle = (angle + dt * speed) % (Math.PI * 2);
        } else if (touchscreen) angle = PandoraLayout.INITIAL_ANGLE;
    }

    private float sample(long now) {
        float t = Math.max(0, Math.min(1, (now - snapshotTime) / 50.0F));
        return fromProgress + (targetProgress - fromProgress) * t;
    }

    public static float smooth(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    public float progress() { return progress; }
    public float flash() { return flash; }
    public double angle() { return angle; }
    public float radius() { return PandoraLayout.ORBIT_RADIUS * (1 - contraction); }
    public float slotX(int index) {
        return PandoraLayout.CENTER_X - 8 + (float) (radius() * Math.cos(angle + index * Math.PI * 2 / 3));
    }
    public float slotY(int index) {
        return PandoraLayout.CENTER_Y - 8 + (float) (radius() * Math.sin(angle + index * Math.PI * 2 / 3));
    }
}
