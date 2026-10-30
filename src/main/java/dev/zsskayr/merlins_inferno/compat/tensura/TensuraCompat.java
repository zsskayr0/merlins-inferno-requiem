package dev.zsskayr.merlins_inferno.compat.tensura;

import java.lang.reflect.Method;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * Soft bridge to Tensura (reflection, no compile dependency): the faithful of this mod hunt the Majin-aligned
 * (Tensura's {@code Alignment.MAJIN}, read from {@code TensuraStorages.getExistenceFrom(entity).getAlignment()}).
 * Without Tensura, or if its API changed, nobody counts as Majin.
 */
public final class TensuraCompat {
    private static final String MOD_ID = "tensura";

    private static boolean resolved;
    private static boolean available;
    private static Method getExistence;
    private static Method getAlignment;

    private TensuraCompat() {
    }

    public static boolean isMajin(LivingEntity living) {
        if (!ModList.get().isLoaded(MOD_ID) || !living.isAlive() || !resolve()) {
            return false;
        }
        try {
            Object alignment = getAlignment.invoke(getExistence.invoke(null, living));
            return alignment instanceof Enum<?> value && value.name().equals("MAJIN");
        } catch (ReflectiveOperationException | LinkageError | ClassCastException | NullPointerException exception) {
            return false;
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return available;
        }
        resolved = true;
        try {
            getExistence = Class.forName("io.github.manasmods.tensura.storage.TensuraStorages").getMethod("getExistenceFrom", LivingEntity.class);
            getAlignment = Class.forName("io.github.manasmods.tensura.storage.ep.IExistence").getMethod("getAlignment");
            available = true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            org.slf4j.LoggerFactory.getLogger(Merlins_inferno.class).warn("Tensura bridge unavailable", exception);
        }
        return available;
    }
}
