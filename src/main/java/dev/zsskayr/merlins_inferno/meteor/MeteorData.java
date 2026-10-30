package dev.zsskayr.merlins_inferno.meteor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The world's fallen stars, kept in the Overworld's data storage. A meteor is only a coordinate until a player comes near enough for
 * its chunks to load: it is then built ({@link MeteorSite}) and, once somebody reaches it (or days pass), forgotten.
 */
public class MeteorData extends SavedData {
    private static final String KEY = "merlins_inferno_meteors";
    private static final SavedData.Factory<MeteorData> FACTORY = new SavedData.Factory<>(MeteorData::new, MeteorData::load, null);

    /** A fallen star: where it is, when it fell, and whether its crater has been built yet. */
    public static final class Meteor {
        public final int x;
        public final int z;
        public final long fell;
        public int y;
        public boolean built;

        public Meteor(int x, int z, long fell, int y, boolean built) {
            this.x = x;
            this.z = z;
            this.fell = fell;
            this.y = y;
            this.built = built;
        }
    }

    private final List<Meteor> meteors = new ArrayList<>();
    private long lastNight = -1;

    public static MeteorData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    private static MeteorData load(CompoundTag tag, HolderLookup.Provider registries) {
        MeteorData data = new MeteorData();
        data.lastNight = tag.contains("last_night") ? tag.getLong("last_night") : -1;
        for (Tag entry : tag.getList("meteors", Tag.TAG_COMPOUND)) {
            CompoundTag meteor = (CompoundTag) entry;
            data.meteors.add(new Meteor(meteor.getInt("x"), meteor.getInt("z"), meteor.getLong("fell"), meteor.getInt("y"), meteor.getBoolean("built")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("last_night", lastNight);
        ListTag list = new ListTag();
        for (Meteor meteor : meteors) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", meteor.x);
            entry.putInt("z", meteor.z);
            entry.putLong("fell", meteor.fell);
            entry.putInt("y", meteor.y);
            entry.putBoolean("built", meteor.built);
            list.add(entry);
        }
        tag.put("meteors", list);
        return tag;
    }

    public List<Meteor> all() {
        return meteors;
    }

    public int active() {
        return meteors.size();
    }

    public Meteor add(int x, int z, long gameTime, int y) {
        Meteor meteor = new Meteor(x, z, gameTime, y, false);
        meteors.add(meteor);
        setDirty();
        return meteor;
    }

    public void remove(Meteor meteor) {
        if (meteors.remove(meteor)) {
            setDirty();
        }
    }

    /** The night (day number) whose roll has already been made, so a night is rolled once. */
    public long lastNight() {
        return lastNight;
    }

    public void rolledNight(long day) {
        lastNight = day;
        setDirty();
    }

    public void touch() {
        setDirty();
    }

    /** The exact spot of the meteor nearest to {@code from} in this level (only the Overworld has them), or {@code null}. */
    public BlockPos nearest(ServerLevel level, BlockPos from) {
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return null;
        }
        Meteor best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Meteor meteor : meteors) {
            double dx = meteor.x - from.getX();
            double dz = meteor.z - from.getZ();
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = meteor;
            }
        }
        return best == null ? null : new BlockPos(best.x, best.y, best.z);
    }
}
