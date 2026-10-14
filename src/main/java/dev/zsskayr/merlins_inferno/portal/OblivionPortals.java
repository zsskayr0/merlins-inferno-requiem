package dev.zsskayr.merlins_inferno.portal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** The arrival portals built in Oblivion (the lower-left interior block of each), so later arrivals reuse the nearest. */
public class OblivionPortals extends SavedData {
    private static final String KEY = "merlins_inferno_oblivion_portals";
    private static final SavedData.Factory<OblivionPortals> FACTORY = new SavedData.Factory<>(OblivionPortals::new, OblivionPortals::load, null);

    private final List<BlockPos> portals = new ArrayList<>();

    public static OblivionPortals get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    private static OblivionPortals load(CompoundTag tag, HolderLookup.Provider registries) {
        OblivionPortals data = new OblivionPortals();
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
        portals.add(pos);
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (portals.remove(pos)) {
            setDirty();
        }
    }
}
