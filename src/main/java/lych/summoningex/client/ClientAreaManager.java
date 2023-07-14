package lych.summoningex.client;

import com.google.common.collect.ImmutableSortedMap;
import lych.summoningex.network.AreaNetwork;
import lych.summoningex.world.Area;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@OnlyIn(Dist.CLIENT)
public class ClientAreaManager {
    private static Map<ResourceKey<Level>, ClientAreaManager> instances;
    private final SortedMap<String, Area> areas = new TreeMap<>();
    private boolean showsAreas;

    public static ClientAreaManager getInstanceIn(ResourceKey<Level> key) {
        if (instances == null) {
            instances = new HashMap<>();
        }
        return instances.computeIfAbsent(key, l -> new ClientAreaManager());
    }

    public void accept(@Nullable Area area, AreaNetwork.Operation operation) {
        operation.operate(this, areas, area);
    }

    public ImmutableSortedMap<String, Area> getAreas() {
        return ImmutableSortedMap.copyOf(areas);
    }

    @Nullable
    public Area get(String name) {
        return areas.get(name);
    }

    public boolean showsAreas() {
        return showsAreas;
    }

    public void setShowsAreas(boolean showsAreas) {
        this.showsAreas = showsAreas;
    }
}
