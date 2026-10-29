package dev.zsskayr.merlins_inferno.attachment;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** The world's Circle, stored once in the Overworld's data storage so it holds for every dimension and every player. */
public class WorldCircle extends SavedData {
    private static final String KEY = "merlins_inferno_world_circle";
    private static final SavedData.Factory<WorldCircle> FACTORY = new SavedData.Factory<>(WorldCircle::new, WorldCircle::load, null);

    private int circle = ProgressionHelper.FIRST_CIRCLE;

    public static WorldCircle get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    private static WorldCircle load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldCircle data = new WorldCircle();
        data.circle = ProgressionHelper.clamp(tag.getInt("circle"));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("circle", circle);
        return tag;
    }

    public int circle() {
        return circle;
    }

    void set(int circle) {
        this.circle = circle;
        setDirty();
    }
}
