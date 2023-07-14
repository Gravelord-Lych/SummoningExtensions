package lych.summoningex.world;

import com.google.common.collect.ImmutableSortedMap;
import lych.summoningex.SummoningExtensions;
import lych.summoningex.network.AreaNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;

import java.util.SortedMap;
import java.util.TreeMap;

public class AreaManager extends SavedData {
    private static final String NAME = SummoningExtensions.MOD_ID + "_areas";
    private static final String TAG_KEY = "Areas";
    private static final String SHOWS_AREAS = "ShowsAreas";
    private final SortedMap<String, Area> areas = new TreeMap<>();
    private final ServerLevel level;
    private boolean showsAreas;

    public AreaManager(ServerLevel level) {
        this.level = level;
        AreaNetwork.sync(level, AreaNetwork.Operation.CLEAR);
        AreaNetwork.sync(level, AreaNetwork.Operation.HIDE);
    }

    public static AreaManager get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(tag -> load(tag, level), () -> new AreaManager(level), NAME);
    }

    /**
     * Adds the area to the AreaMap.
     * @param area The area
     * @return The old area with the same name (nullable)
     */
    @Nullable
    public Area add(Area area) {
        setDirty();
        Area old = areas.put(area.getName(), area);
        AreaNetwork.sync(level, area, AreaNetwork.Operation.ADD);
        return old;
    }

    /**
     * Removes an area from the AreaMap.
     * @param name The name of the area which will be removed
     * @return True if successfully removed the area. False if the area was not found.
     */
    @Nullable
    public Area remove(String name) {
        Area area = areas.remove(name);
        if (area != null) {
            AreaNetwork.sync(level, area, AreaNetwork.Operation.REMOVE);
        }
        return area;
    }

    /**
     * Clears all the areas
     * @return True if some areas have been cleared
     */
    public boolean clear() {
        if (areas.isEmpty()) {
            return false;
        }
        areas.clear();
        AreaNetwork.sync(level, AreaNetwork.Operation.CLEAR);
        return true;
    }

    public int setShowsAreas(boolean showsAreas) {
        if (this.showsAreas == showsAreas) {
            return 0;
        }
        this.showsAreas = showsAreas;
        AreaNetwork.sync(level, showsAreas ? AreaNetwork.Operation.SHOW : AreaNetwork.Operation.HIDE);
        return 1;
    }

    @Nullable
    public Area byName(String name) {
        return areas.get(name);
    }

    public ImmutableSortedMap<String, Area> getAreas() {
        return ImmutableSortedMap.copyOf(areas);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean(SHOWS_AREAS, showsAreas);
        ListTag listTag = new ListTag();
        for (Area area : areas.values()) {
            listTag.add(area.save());
        }
        tag.put(TAG_KEY, listTag);
        return tag;
    }

    public static AreaManager load(CompoundTag tag, ServerLevel level) {
        if (!tag.contains(TAG_KEY, CompoundTag.TAG_LIST)) {
            throw new IllegalStateException("No tag of AreaManager found");
        }
        AreaManager manager = new AreaManager(level);
        manager.setShowsAreas(tag.getBoolean(SHOWS_AREAS));
        AreaNetwork.sync(level, AreaNetwork.Operation.CLEAR);
        ListTag listTag = tag.getList(TAG_KEY, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            manager.add(Area.load(listTag.getCompound(i)));
        }
        return manager;
    }
}
