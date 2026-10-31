package dev.zsskayr.merlins_inferno.portal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** The El'ysium portals of one level, lit or built (one film block of each), so later travel reuses the nearest. */
public class ElysiumPortals extends SavedData {
    private static final String KEY = "merlins_inferno_elysium_portals";
    private static final SavedData.Factory<ElysiumPortals> FACTORY = new SavedData.Factory<>(ElysiumPortals::new, ElysiumPortals::load, null);

    private final List<BlockPos> portals = new ArrayList<>();

    public static ElysiumPortals get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    private static ElysiumPortals load(CompoundTag tag, HolderLookup.Provider registries) {
        ElysiumPortals data = new ElysiumPortals();
        for (long packed : tag.getLongArray("portals")) {
            data.portals.add(BlockPos.of(packed));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("portals", portals.stream().mapToLong(BlockPos::asLong).toArray());
        return tag;
    }

    public List<BlockPos> all() {
        return portals;
    }

    public void add(BlockPos pos) {
        if (!portals.contains(pos)) {
            portals.add(pos);
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (portals.remove(pos)) {
            setDirty();
        }
    }
}
