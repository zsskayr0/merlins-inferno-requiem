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
        loadTints();
        trueOverview(w, new File(out, "overview_true_seed" + seed + ".png"), 0, 0, 8);
        ElysiumWorld.Island sea = all.stream().filter(is -> is.size() == ElysiumBiome.Size.COLOSSAL)
                .max(java.util.Comparator.comparingInt(ElysiumWorld.Island::width)).orElse(null);
        if (sea != null) {
            trueOverview(w, new File(out, "overview_true_sea_seed" + seed + ".png"), sea.cx(), sea.cz(), 40);
        }
        for (ElysiumWorld.Island is : pick.values()) {
            if (is.biome() != ElysiumBiome.CHAINFALL) {
                trueIsland(w, is, new File(out, "island_true_" + is.biome().id + "_seed" + seed + ".png"));
            }
        }
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
        // how far apart the colossal seas stand: distance from each to its nearest neighbour
        List<ElysiumWorld.Island> seas = w.islandsIn(-60000, -60000, 60000, 60000).stream()
                .filter(is -> is.size() == ElysiumBiome.Size.COLOSSAL).toList();
        double sum = 0, min = Double.POSITIVE_INFINITY, max = 0;
        for (ElysiumWorld.Island a : seas) {
            double near = Double.POSITIVE_INFINITY;
            for (ElysiumWorld.Island b : seas) {
                if (a != b) {
                    near = Math.min(near, Math.hypot(a.cx() - b.cx(), a.cz() - b.cz()));
                }
            }
            sum += near;
            min = Math.min(min, near);
            max = Math.max(max, near);
        }
        System.out.printf("colossal seas: %d, nearest neighbour mean %.0f min %.0f max %.0f%n", seas.size(), sum / seas.size(), min, max);
    }

    // ------------------------------------------------------------------------------------------------ true colours

    /** {grass, foliage, water} of each biome, read from its JSON (run from the repo root). */
    private static final Map<ElysiumBiome, int[]> TINT = new EnumMap<>(ElysiumBiome.class);

    private static void loadTints() throws Exception {
        for (ElysiumBiome b : ElysiumBiome.values()) {
            String json = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "src/main/resources/data/merlins_inferno/worldgen/biome/" + b.id + ".json"));
            TINT.put(b, new int[] {jsonInt(json, "grass_color"), jsonInt(json, "foliage_color"), jsonInt(json, "water_color")});
        }
    }

    private static int jsonInt(String json, String key) {
        var m = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0x7FB238;
    }

    /** Multiplies two colours, as the game tints a grey texture. {@code grey} is the texture's average level. */
    private static int tint(int colour, int grey) {
        return scale(colour, grey / 255.0);
    }

    private static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Average colour of each block's texture (vanilla 1.21.1 assets; Arganite and Quartzite from the mod). */
    private static int textureColour(Mat m, ElysiumBiome biome) {
        return switch (m) {
            case GRASS_BLOCK -> tint(TINT.get(biome)[0], 0x93);
            case ARGANITE -> 0xAEB1B7;
            case QUARTZITE -> 0xDBD2C4;
            case CALCITE -> 0xDFE0DC;
            case STONE -> 0x7D7D7D;
            case ANDESITE -> 0x888888;
            case DIORITE -> 0xBCBCBC;
            case TUFF -> 0x6C6D66;
            case SMOOTH_BASALT -> 0x48484E;
            case DIRT -> 0x866043;
            case COARSE_DIRT -> 0x77553B;
            case ROOTED_DIRT -> 0x90674C;
            case PODZOL -> 0x5B3F18;
            case MOSS_BLOCK, MOSS_CARPET -> 0x596D2D;
            case MUD -> 0x3C393C;
            case CLAY -> 0xA0A6B3;
            case GRAVEL -> 0x837F7E;
            case SAND -> 0xDBCFA3;
            case SANDSTONE -> 0xDFD6AA;
            case SNOW_BLOCK, SNOW_LAYER, POWDER_SNOW -> 0xF9FEFE;
            case ICE -> 0x91B7FD;
            case PACKED_ICE -> 0x8DB4FA;
            case BLUE_ICE -> 0x74A7FD;
            case MOSSY_COBBLESTONE -> 0x6E765E;
            case COBBLESTONE -> 0x7F7F7F;
            case QUARTZ_BLOCK -> 0xEBE5DE;
            case AMETHYST_BLOCK -> 0x8561BF;
            default -> 0x7D7D7D;
        };
    }

    /**
     * Approximate tree cover: the biome JSONs' tree features are not run here, so each biome gets a jittered grid of round
     * canopies at about its real density, in the leaf colour its trees really have. Returns -1 where no canopy covers the column.
     */
    private static int canopy(Column c, long seed) {
        int cell;
        double prob, radius;
        int leaf, leaf2;
        int foliage = tint(TINT.get(c.biome)[1], 0x90);
        switch (c.biome) {
            case HIGHPINE_FOREST -> { cell = 8; prob = 0.85; radius = 4; leaf = tint(0x619961, 0x7E); leaf2 = leaf; }
            case MOSSY_WEALD -> { cell = 9; prob = 0.8; radius = 5.5; leaf = 0x5A722C; leaf2 = 0x636F3C; } // azalea leaves are not tinted
            case MISTFALL_GORGE -> { cell = 10; prob = 0.75; radius = 4.5; leaf = 0x5A722C; leaf2 = foliage; }
            case CASCADE_VALE -> { cell = 9; prob = 0.7; radius = 3.5; leaf = tint(0x80A755, 0x82); leaf2 = foliage; }
            case LARK_MEADOWS, STONECROWN_CLIFFS, GLASS_SEA -> { cell = 34; prob = 0.35; radius = 3.5; leaf = foliage; leaf2 = foliage; }
            case ARGENT_PEAKS, FROSTCREST_RANGE, RIME_EXPANSE -> { cell = 20; prob = 0.3; radius = 3; leaf = 0xDDE6E0; leaf2 = tint(0x619961, 0x7E); } // snow on spruce
            default -> { return -1; }
        }
        int gx = Math.floorDiv(c.x, cell), gz = Math.floorDiv(c.z, cell);
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                java.util.SplittableRandom r = new java.util.SplittableRandom(seed ^ ((gx + ox) * 341873128712L + (gz + oz) * 132897987541L));
                if (r.nextDouble() >= prob) {
                    continue;
                }
                double tx = (gx + ox + r.nextDouble()) * cell, tz = (gz + oz + r.nextDouble()) * cell;
                double dx = c.x + 0.5 - tx, dz = c.z + 0.5 - tz, rr = radius * (0.8 + 0.4 * r.nextDouble());
                if (dx * dx + dz * dz < rr * rr) {
                    return scale(r.nextBoolean() ? leaf : leaf2, 1.05 - 0.3 * Math.sqrt(dx * dx + dz * dz) / rr);
                }
            }
        }
        return -1;
    }

    private static int trueColour(ElysiumWorld w, Column c) {
        if (!c.land) {
            return new int[] {0xCED0D4, 0xE2E3E6, 0xF4F5F8}[w.cloudTone(c.x, c.z, w.cloudTop(c.x, c.z))];
        }
        if (c.hasArch()) {
            return textureColour(w.material(c, c.archTop), c.biome);
        }
        if (c.hasWater()) {
            if (c.frozen) {
                return 0x91B7FD;
            }
            int water = tint(TINT.get(c.biome)[2], 0xB1);
            double clear = Math.max(0.15, 0.6 - 0.09 * (c.waterTop - c.top));
            return mix(water, textureColour(w.material(c, c.top), c.biome), clear);
        }
        Mat top = w.material(c, c.top);
        if (top == Mat.GRASS_BLOCK || top == Mat.PODZOL || top == Mat.DIRT || top == Mat.MOSS_BLOCK || top == Mat.COARSE_DIRT) {
            int leaves = canopy(c, w.seed);
            if (leaves != -1) {
                return leaves;
            }
        }
        return textureColour(top, c.biome);
    }

    /** A 1000x1000 map centred on (cx, cz), one pixel = {@code scale} blocks. */
    private static void trueOverview(ElysiumWorld w, File f, int cx, int cz, int scale) throws Exception {
        int size = 1000, half = size * scale / 2;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        int[] tops = new int[size * size], rgb = new int[size * size];
        IntStream.range(0, size).parallel().forEach(pz -> {
            Column c = new Column();
            for (int px = 0; px < size; px++) {
                w.sample(cx + px * scale - half, cz + pz * scale - half, c);
                tops[pz * size + px] = c.land ? c.top : Integer.MIN_VALUE;
                rgb[pz * size + px] = trueColour(w, c);
            }
        });
        shade(img, tops, rgb, size, size, 0.6);
        Graphics2D g = img.createGraphics();
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(new Color(0x6C6D66));
        for (ElysiumWorld.Span s : w.spansIn(cx - half, cz - half, cx + half, cz + half)) {
            g.drawLine((s.ax() - cx + half) / scale, (s.az() - cz + half) / scale, (s.bx() - cx + half) / scale, (s.bz() - cz + half) / scale);
        }
        g.dispose();
        ImageIO.write(img, "png", f);
        System.out.println("wrote " + f);
    }

    private static void trueIsland(ElysiumWorld w, ElysiumWorld.Island is, File f) throws Exception {
        int x0 = is.minX() - 10, z0 = is.minZ() - 10, ww = is.maxX() - is.minX() + 21, hh = is.maxZ() - is.minZ() + 21;
        int[] tops = new int[ww * hh], rgb = new int[ww * hh];
        IntStream.range(0, hh).parallel().forEach(z -> {
            Column c = new Column();
            for (int x = 0; x < ww; x++) {
                w.sample(x0 + x, z0 + z, c);
                tops[z * ww + x] = c.land ? c.top : Integer.MIN_VALUE;
                rgb[z * ww + x] = trueColour(w, c);
            }
        });
        BufferedImage img = new BufferedImage(ww, hh, BufferedImage.TYPE_INT_RGB);
        shade(img, tops, rgb, ww, hh, 1.0);
        ImageIO.write(img, "png", f);
        System.out.println("wrote " + f);
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
            case CALCITE, QUARTZ_BLOCK, QUARTZITE, DIORITE -> 0xE6E6E0;
            case SNOW_BLOCK, POWDER_SNOW, SNOW_LAYER -> 0xFAFAFF;
            case ICE, PACKED_ICE, BLUE_ICE -> 0x9CC8F5;
            case ARGANITE -> 0x5C4F6E;
            case TUFF, SMOOTH_BASALT -> 0x4A4A50;
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
