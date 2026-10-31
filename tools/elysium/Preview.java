import dev.zsskayr.merlins_inferno.worldgen.elysium.core.Column;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumBiome;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.Mat;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.River;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

/**
 * Offline renders of the El'ysium core (headless AWT): an 8000x8000 overview at 1px = 8 blocks, a 1:1 map of one
 * island per biome, and x/y cross-sections through a few islands (underside cone, water, river terraces).
 *
 * <p>Usage: {@code java -Djava.awt.headless=true -cp corebuild:tools Preview [seed] [outDir]}.
 */
public final class Preview {
    private static final Map<ElysiumBiome, Color> BIOME = new EnumMap<>(ElysiumBiome.class);

    static {
        BIOME.put(ElysiumBiome.LARK_MEADOWS, new Color(110, 190, 70));
        BIOME.put(ElysiumBiome.WINDLESS_STEPPE, new Color(185, 190, 95));
        BIOME.put(ElysiumBiome.HIGHPINE_FOREST, new Color(40, 100, 55));
        BIOME.put(ElysiumBiome.MOSSY_WEALD, new Color(70, 130, 60));
        BIOME.put(ElysiumBiome.ARGENT_PEAKS, new Color(150, 150, 170));
        BIOME.put(ElysiumBiome.STONECROWN_CLIFFS, new Color(120, 120, 110));
        BIOME.put(ElysiumBiome.CASCADE_VALE, new Color(200, 120, 60));
        BIOME.put(ElysiumBiome.MISTFALL_GORGE, new Color(70, 150, 120));
        BIOME.put(ElysiumBiome.GLASS_SEA, new Color(220, 220, 200));
        BIOME.put(ElysiumBiome.AZURE_SHALLOWS, new Color(235, 215, 160));
        BIOME.put(ElysiumBiome.RIME_EXPANSE, new Color(235, 240, 250));
        BIOME.put(ElysiumBiome.FROSTCREST_RANGE, new Color(190, 210, 235));
        BIOME.put(ElysiumBiome.CHAINFALL, new Color(140, 90, 70));
        BIOME.put(ElysiumBiome.UNDERHANG, new Color(60, 40, 70));
    }

    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 1L;
        File out = new File(args.length > 1 ? args[1] : "/home/claude/work/renders");
        out.mkdirs();
        ElysiumWorld w = new ElysiumWorld(seed);
        overview(w, new File(out, "overview_seed" + seed + ".png"));
        Map<ElysiumBiome, ElysiumWorld.Island> pick = new EnumMap<>(ElysiumBiome.class);
        List<ElysiumWorld.Island> all = w.islandsIn(-30000, -30000, 30000, 30000);
        all.sort((a, b) -> Long.compare((long) a.cx() * a.cx() + (long) a.cz() * a.cz(), (long) b.cx() * b.cx() + (long) b.cz() * b.cz()));
        for (ElysiumWorld.Island is : all) {
            pick.putIfAbsent(is.biome(), is);
        }
        pick.values().parallelStream().forEach(is -> {
            try {
                detail(w, is, new File(out, "island_" + is.biome().id + "_seed" + seed + ".png"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        ElysiumBiome[] sec = {ElysiumBiome.CASCADE_VALE, ElysiumBiome.GLASS_SEA, ElysiumBiome.ARGENT_PEAKS,
                ElysiumBiome.STONECROWN_CLIFFS, ElysiumBiome.MISTFALL_GORGE};
        for (ElysiumBiome b : sec) {
            ElysiumWorld.Island is = pick.get(b);
            if (is != null) {
                section(w, is, is.cz(), new File(out, "section_" + b.id + "_seed" + seed + ".png"));
                List<River> rv = w.riversOf(is.index());
                if (!rv.isEmpty()) {
                    River r = rv.get(0);
                    int mid = r.size() * 3 / 4;
                    section(w, is, (int) r.zs[mid], new File(out, "section_" + b.id + "_river_seed" + seed + ".png"));
                }
            }
        }
        // stats: size & biome distribution
        Map<ElysiumBiome.Size, Integer> sizes = new EnumMap<>(ElysiumBiome.Size.class);
        Map<ElysiumBiome, Integer> biomes = new EnumMap<>(ElysiumBiome.class);
        List<ElysiumWorld.Island> area = w.islandsIn(-20000, -20000, 20000, 20000);
        int n = 0;
        for (ElysiumWorld.Island is : area) {
            sizes.merge(is.size(), 1, Integer::sum);
            biomes.merge(is.biome(), 1, Integer::sum);
            if (is.size() != ElysiumBiome.Size.PLATFORM) {
                n++;
            }
        }
        System.out.println("islands in 40k x 40k: " + n + " sizes " + sizes + "\nbiomes " + biomes);
    }

    private static void overview(ElysiumWorld w, File f) throws Exception {
        int size = 1000, scale = 8, half = size * scale / 2;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        int[] tops = new int[size * size];
        int[] rgb = new int[size * size];
        IntStream.range(0, size).parallel().forEach(pz -> {
            Column c = new Column();
            for (int px = 0; px < size; px++) {
                int x = px * scale - half, z = pz * scale - half;
                w.sample(x, z, c);
                tops[pz * size + px] = c.land ? c.top : Integer.MIN_VALUE;
                rgb[pz * size + px] = colour(c);
            }
        });
        shade(img, tops, rgb, size, size, 0.6);
        Graphics2D g = img.createGraphics();
        g.setStroke(new BasicStroke(1.2f));
        for (ElysiumWorld.Span s : w.spansIn(-half, -half, half, half)) {
            g.setColor(switch (s.kind()) {
                case STONE_ARCH -> new Color(90, 90, 90);
                case ROPE -> new Color(150, 100, 40);
                case VINE_CHAIN -> new Color(30, 120, 30);
                case GIANT_CHAIN -> new Color(60, 30, 30);
            });
            g.drawLine((s.ax() + half) / scale, (s.az() + half) / scale, (s.bx() + half) / scale, (s.bz() + half) / scale);
        }
        g.dispose();
        ImageIO.write(img, "png", f);
        System.out.println("wrote " + f);
    }

    private static int colour(Column c) {
        if (!c.land) {
            return 0xEEF2F7;
        }
        if (c.hasWater()) {
            if (c.frozen) {
                return 0xA8D0F0;
            }
            int d = c.waterTop - c.top;
            if (c.spill) {
                return 0xFF2020;
            }
            return d <= 2 ? 0x4FB8E0 : (d <= 5 ? 0x2C8BD0 : 0x1A5FA8);
        }
        Color b = BIOME.get(c.biome);
        if (c.shore) {
            b = c.biome == ElysiumBiome.GLASS_SEA ? new Color(240, 240, 235) : new Color(214, 200, 150);
        }
        if (c.top > 300) {
            b = new Color(245, 245, 250);
        }
        return b.getRGB() & 0xFFFFFF;
    }

    private static void shade(BufferedImage img, int[] tops, int[] rgb, int w, int h, double k) {
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                int i = z * w + x;
                int c = rgb[i];
                if (tops[i] != Integer.MIN_VALUE && x > 0 && z > 0 && tops[i - 1] != Integer.MIN_VALUE && tops[i - w] != Integer.MIN_VALUE) {
                    double d = (tops[i] - tops[i - 1]) + (tops[i] - tops[i - w]);
                    double f = Math.max(0.45, Math.min(1.4, 1 + d * 0.06 * k));
                    int r = Math.min(255, (int) (((c >> 16) & 255) * f));
                    int gg = Math.min(255, (int) (((c >> 8) & 255) * f));
                    int b = Math.min(255, (int) ((c & 255) * f));
                    c = (r << 16) | (gg << 8) | b;
                }
                img.setRGB(x, z, c);
            }
        }
    }

    private static void detail(ElysiumWorld w, ElysiumWorld.Island is, File f) throws Exception {
        int x0 = is.minX(), z0 = is.minZ(), ww = is.maxX() - x0 + 1, hh = is.maxZ() - z0 + 1;
        int[] tops = new int[ww * hh], rgb = new int[ww * hh];
        Column c = new Column();
        for (int z = 0; z < hh; z++) {
            for (int x = 0; x < ww; x++) {
                w.sample(x0 + x, z0 + z, c);
                tops[z * ww + x] = c.land ? c.top : Integer.MIN_VALUE;
                int col = colour(c);
                if (c.land && !c.hasWater()) {
                    // height tint: darker low, lighter high
                    double t = Math.max(0.75, Math.min(1.25, 1 + (c.top - c.baseY) / 120.0));
                    col = scale(col, t);
                }
                if (c.hasArch()) {
                    col = 0x8B5A2B;
                }
                rgb[z * ww + x] = col;
            }
        }
        BufferedImage img = new BufferedImage(ww, hh, BufferedImage.TYPE_INT_RGB);
        shade(img, tops, rgb, ww, hh, 1.0);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        for (ElysiumWorld.Spring s : w.springsIn(is.minX(), is.minZ(), is.maxX(), is.maxZ())) {
            g.fillOval(s.x() - x0 - 3, s.z() - z0 - 3, 7, 7);
        }
        g.setColor(Color.MAGENTA);
        for (ElysiumWorld.Site s : w.sitesIn(is.minX(), is.minZ(), is.maxX(), is.maxZ())) {
            g.drawRect(s.x() - x0 - 6, s.z() - z0 - 6, 12, 12);
            g.drawString(s.kind(), s.x() - x0 + 8, s.z() - z0);
        }
        g.setColor(Color.DARK_GRAY);
        for (ElysiumWorld.Span s : w.spansIn(is.minX(), is.minZ(), is.maxX(), is.maxZ())) {
            g.drawLine(s.ax() - x0, s.az() - z0, s.bx() - x0, s.bz() - z0);
        }
        g.setColor(Color.BLACK);
        g.drawString(is.biome().id + " w=" + is.width() + " " + is.size() + " baseY=" + is.baseY(), 6, 14);
        g.dispose();
        ImageIO.write(img, "png", f);
        System.out.println("wrote " + f);
    }

    private static int scale(int c, double t) {
        int r = Math.min(255, (int) (((c >> 16) & 255) * t));
        int g = Math.min(255, (int) (((c >> 8) & 255) * t));
        int b = Math.min(255, (int) ((c & 255) * t));
        return (r << 16) | (g << 8) | b;
    }

    private static int matColour(Mat m) {
        return switch (m) {
            case GRASS_BLOCK, MOSS_BLOCK -> 0x5DA83A;
            case DIRT, COARSE_DIRT, ROOTED_DIRT, PODZOL, MUD -> 0x7A5634;
            case SAND -> 0xE3D59A;
            case SANDSTONE -> 0xCDBB7A;
            case GRAVEL -> 0x8C8480;
            case CALCITE, QUARTZ_BLOCK, QUARTZITE, DIORITE, WHITE_TERRACOTTA -> 0xE6E6E0;
            case SNOW_BLOCK, POWDER_SNOW, SNOW_LAYER -> 0xFAFAFF;
            case ICE, PACKED_ICE, BLUE_ICE -> 0x9CC8F5;
            case ARGANITE -> 0x5C4F6E;
            case ORANGE_TERRACOTTA, TERRACOTTA, BROWN_TERRACOTTA -> 0xB4643A;
            case DEEPSLATE, TUFF, SMOOTH_BASALT -> 0x4A4A50;
            default -> 0x8A8A8A;
        };
    }

    private static void section(ElysiumWorld w, ElysiumWorld.Island is, int z, File f) throws Exception {
        int x0 = is.minX() - 20, x1 = is.maxX() + 20, ww = x1 - x0 + 1, hh = 384;
        BufferedImage img = new BufferedImage(ww, hh, BufferedImage.TYPE_INT_RGB);
        Column c = new Column();
        for (int x = 0; x < ww; x++) {
            w.sample(x0 + x, z, c);
            for (int y = 0; y < hh; y++) {
                int col = y <= ElysiumWorld.CLOUD_SEA_Y ? 0xF4F6FA : 0xBFDDF5;
                if (c.land) {
                    if (y >= c.bottom && y <= c.top) {
                        col = matColour(w.material(c, y));
                    } else if (c.hasWater() && y > c.top && y <= c.waterTop) {
                        col = c.spill ? 0xFF3030 : (c.frozen && y == c.waterTop ? 0x9CC8F5 : 0x2C70D0);
                    } else if (c.hasArch() && y >= c.archBottom && y <= c.archTop) {
                        col = 0x8B5A2B;
                    } else if (c.hangLength > 0 && y < c.bottom && y >= c.bottom - c.hangLength) {
                        col = 0x2E5E2E;
                    }
                }
                img.setRGB(x, hh - 1 - y, col);
            }
        }
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.drawString(is.biome().id + " section z=" + z + " (x " + x0 + ".." + x1 + ", y 0..383)", 6, 14);
        g.setColor(new Color(255, 0, 0, 90));
        g.drawLine(0, hh - 1 - ElysiumWorld.LOWEST_SOLID, ww, hh - 1 - ElysiumWorld.LOWEST_SOLID);
        g.dispose();
        ImageIO.write(img, "png", f);
        System.out.println("wrote " + f);
    }
}
