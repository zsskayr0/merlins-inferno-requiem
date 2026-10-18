package dev.zsskayr.merlins_inferno.client;

import com.mojang.logging.LogUtils;
import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModEffects;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Shader packs compute their own fog, so the vanilla fog events do nothing under Iris. While the Hell Sight is on this
 * lowers the active pack's own Nether fog option the way Iris' settings screen does (queue the value, reload), and puts
 * the player's value back afterwards. Everything is reflective and optional: without Iris, or with a pack that is not
 * listed, nothing happens. The original value is kept in a backup file so a crash cannot leave the option lowered.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID, value = Dist.CLIENT)
public final class IrisFogCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String IRIS = "net.irisshaders.iris.Iris";
    /** Pack name prefix -> {option, lowered value}. Values must be in the pack's own option list. */
    private static final Map<String, String[]> PACKS = Map.of("photon", new String[]{"NETHER_FOG_INTENSITY", "0.22"});
    private static final int SETTLE_TICKS = 7;

    private static Class<?> iris;
    private static boolean missing;
    private static boolean applied;
    private static String appliedPack;
    private static String appliedKey;
    private static String original;
    private static boolean recovered;
    /** Set after a failed or unsupported attempt so a reload is never retried until the Hell Sight turns off. */
    private static boolean gaveUp;
    private static int settle;

    private IrisFogCompat() {}

    private static Path backupFile() {
        return FMLPaths.CONFIGDIR.get().resolve("merlins_inferno_hell_sight_fog.properties");
    }

    private static Object call(String name, Class<?>... types) throws ReflectiveOperationException {
        Method m = iris.getMethod(name, types);
        return m.invoke(null);
    }

    private static boolean irisReady() {
        if (missing) return false;
        try {
            if (iris == null) iris = Class.forName(IRIS);
            return (boolean) call("isPackInUseQuick");
        } catch (ClassNotFoundException e) {
            missing = true;
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.warn("Hell Sight: Iris fog compat disabled ({})", e.toString());
            missing = true;
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void setOption(String pack, String key, String value) throws ReflectiveOperationException, IOException {
        ((Map<String, String>) call("getShaderPackOptionQueue")).put(key, value);
        call("reload");
        LOGGER.info("Hell Sight: {} {} = {}", pack, key, value);
    }

    private static String readOption(String pack, String key) {
        try {
            Path dir = (Path) call("getShaderpacksDirectory");
            Path file = dir.resolve(pack + ".txt");
            if (Files.isRegularFile(file)) {
                Properties p = new Properties();
                try (Reader r = Files.newBufferedReader(file)) { p.load(r); }
                return p.getProperty(key);
            }
        } catch (ReflectiveOperationException | IOException | RuntimeException e) {
            LOGGER.debug("Hell Sight: could not read pack options", e);
        }
        return null;
    }

    private static void apply(String pack, String[] spec) {
        try {
            original = readOption(pack, spec[0]);
            Properties backup = new Properties();
            backup.setProperty("pack", pack);
            backup.setProperty("key", spec[0]);
            backup.setProperty("value", original == null ? "" : original);
            try (var w = Files.newBufferedWriter(backupFile())) { backup.store(w, "Restored when the Hell Sight turns off"); }
            setOption(pack, spec[0], spec[1]);
            applied = true;
            appliedPack = pack;
            appliedKey = spec[0];
        } catch (ReflectiveOperationException | IOException | RuntimeException e) {
            LOGGER.warn("Hell Sight: could not lower the shader fog option", e);
        }
    }

    private static void restore(String pack, String key, String value) {
        try {
            if (pack.equals(call("getCurrentPackName"))) {
                setOption(pack, key, value == null || value.isEmpty() ? "1.00" : value);
            }
        } catch (ReflectiveOperationException | IOException | RuntimeException e) {
            LOGGER.warn("Hell Sight: could not restore the shader fog option", e);
        } finally {
            applied = false;
            try { Files.deleteIfExists(backupFile()); } catch (IOException ignored) {}
        }
    }

    /** A leftover backup means the game ended while the option was still lowered. */
    private static void recoverAfterCrash() {
        recovered = true;
        Path file = backupFile();
        if (!Files.isRegularFile(file) || !irisReady()) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file)) { p.load(r); } catch (IOException e) { return; }
        String pack = p.getProperty("pack"), key = p.getProperty("key");
        if (pack != null && key != null) restore(pack, key, p.getProperty("value"));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!recovered) {
            if (mc.player != null) return;
            recoverAfterCrash();
        }
        boolean want = mc.player != null && mc.player.hasEffect(ModEffects.LAVA_SIGHT);
        if (!want) gaveUp = false;
        if (want == applied || gaveUp) {
            settle = 0;
            return;
        }
        if (++settle < SETTLE_TICKS || !irisReady()) return;
        settle = 0;
        if (want) {
            String name;
            try { name = String.valueOf(call("getCurrentPackName")); } catch (ReflectiveOperationException | RuntimeException e) { return; }
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            for (var entry : PACKS.entrySet()) {
                if (lower.startsWith(entry.getKey())) {
                    apply(name, entry.getValue());
                    gaveUp = !applied;
                    return;
                }
            }
            gaveUp = true;
        } else {
            restore(appliedPack, appliedKey, original);
        }
    }
}
