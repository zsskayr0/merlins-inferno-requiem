package dev.zsskayr.merlins_inferno.menu;

/** Shared logical GUI coordinates. Slot identities stay fixed while the client animates their positions. */
public final class PandoraLayout {
    public static final int WIDTH = 232, HEIGHT = 256;
    public static final int CENTER_X = 116, CENTER_Y = 78, ORBIT_RADIUS = 40;
    public static final int INVENTORY_X = 36, INVENTORY_Y = 171, HOTBAR_Y = 229;
    public static final int BUTTON_X = 48, BUTTON_Y = 135, BUTTON_WIDTH = 136, BUTTON_HEIGHT = 19;
    public static final double INITIAL_ANGLE = -Math.PI / 2;
    public static final double ORBIT_PERIOD_MS = 18000;

    private PandoraLayout() {}

    public static int slotX(int index, double angle) {
        return CENTER_X - 8 + (index == 3 ? 0 : (int) Math.round(ORBIT_RADIUS * Math.cos(angle + index * 2 * Math.PI / 3)));
    }

    public static int slotY(int index, double angle) {
        return CENTER_Y - 8 + (index == 3 ? 0 : (int) Math.round(ORBIT_RADIUS * Math.sin(angle + index * 2 * Math.PI / 3)));
    }

    public static boolean offeringHovered(float x, float y, double mouseX, double mouseY, boolean resultPresent) {
        // The finished item wins over returning surplus offerings while their paths still cross the center.
        boolean overCenter = mouseX >= CENTER_X - 9 && mouseX < CENTER_X + 9
                && mouseY >= CENTER_Y - 9 && mouseY < CENTER_Y + 9;
        return !(resultPresent && overCenter) && mouseX >= x - 1 && mouseX < x + 17
                && mouseY >= y - 1 && mouseY < y + 17;
    }

    public static float scale(int screenWidth, int screenHeight) {
        return Math.min(1.0F, Math.min((screenWidth - 8.0F) / WIDTH, (screenHeight - 8.0F) / HEIGHT));
    }

    public static double unscale(double coordinate, int screenSize, float scale) {
        return screenSize / 2.0 + (coordinate - screenSize / 2.0) / scale;
    }
}
