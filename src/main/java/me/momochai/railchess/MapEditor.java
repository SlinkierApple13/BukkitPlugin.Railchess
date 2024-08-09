package me.momochai.railchess;

import org.apache.commons.lang3.tuple.MutablePair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.server.BroadcastMessageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.BiFunction;

public class MapEditor {

    Railchess plugin;
    Location location;
    Vector hDir;
    double sizeH;
    double sizeV;
    public static final double STATION_RANGE = 0.2 * 0.2;
    List<Player> editingPlayer = new ArrayList<>();
    int nextId = 1;
    boolean available = true;
    Map<Integer, StationWrapper> stationList = new HashMap<>();
    List<MutablePair<Integer, Integer>> transferRepellence = new ArrayList<>();
    List<MutablePair<Integer, Integer>> spawnRepellence = new ArrayList<>();
    String name;
    int currentStation = -1;
    int previousStation = -1;
    int activeLine = -1;
    RailchessStand stand;

    MapEditor(@NotNull Railchess p, @NotNull RailchessStand st, String na, Location loc, Vector hD, double sH, double sV) {
        plugin = p;
        stand = st;
        for (Player pl: st.players) {
            plugin.playerInStand.remove(pl.getName());
            if (pl.hasPermission("railchess.edit")) {
                plugin.playerInEditor.put(pl.getName(), this);
                editingPlayer.add(pl);
            }
        }
        name = na;
        hDir = hD;
        location = loc;
        sizeH = sH;
        sizeV = sV;
        if (p.railmap.containsKey(na)) {
            broadcastMessage("Opening map " + na + ".railmap");
            loadFrom(p.railmap.get(na));
        }
        selectLine(1);
    }

    public Vector normal() {
        return hDir.getCrossProduct(new Vector(0.0, 1.0, 0.0)).normalize();
    }

    public int getStation(Player pl, boolean newOnFail) {
        if (!editingPlayer.contains(pl)) return -1;
        MutablePair<Boolean, MutablePair<Double, Double>> sight = getSight(pl);
        if (!sight.getLeft()) return -1;
        double x = sight.getRight().getLeft();
        double y = sight.getRight().getRight();
        AtomicReference<Double> minDist2 = new AtomicReference<>(STATION_RANGE);
        AtomicInteger sta = new AtomicInteger(-2);
        stationList.forEach((Integer id, StationWrapper stw) -> {
            if (dist2(x, y, stw.station.normPos) < minDist2.get()) {
                minDist2.set(dist2(x, y, stw.station.normPos));
                sta.set(id);
            }
        });
        if (newOnFail && sta.get() == -2)
            return newStation(sight.getRight(), s -> {});
        return sta.get();
    }

    public void broadcastMessage(String s) {
        for (Player p: editingPlayer)
            p.sendMessage(s);
    }

    public void addTrainForbid(int sta, int from, int to, int line) {
        if (!stationList.containsKey(sta) || !stationList.containsKey(to) || !stationList.containsKey(from))
            return;
        if (stationList.get(sta).station.forbid.contains(new ForbidTrain(from, line, to)))
            return;
        stationList.get(sta).station.forbid.add(new ForbidTrain(from, line, to));
        broadcastMessage("Forbid train from " + from + " to " + to + " via " + sta + " on Line " + line);
    }

    public void removeTrainForbid(int sta, int from, int to, int line) {
        if (!stationList.containsKey(sta) || !stationList.containsKey(to) || !stationList.containsKey(from))
            return;
        if (!stationList.get(sta).station.forbid.contains(new ForbidTrain(from, line, to)))
            return;
        stationList.get(sta).station.forbid.remove(new ForbidTrain(from, line, to));
        broadcastMessage("Allowed train from " + from + " to " + to + " via " + sta + " on Line " + line);
    }

    //  Right Click without Sneaking: Select station
    //  Right Click while Sneaking: Place or select station, and connect with previous station
    //  Left Click without sneaking: Place or select station
    //  Left Click while Sneaking: Remove station
    public void parse(Player pl, boolean leftClick, boolean sneaking) {
        if (!editingPlayer.contains(pl))
            return;
        if (!leftClick && sneaking) {
            int sta = getStation(pl, true);
            if (stationList.containsKey(sta)) {
                selectStation(sta);
                biConnect();
            }
        }
        if (leftClick && !sneaking)
            selectStation(getStation(pl, true));
        if (!leftClick && !sneaking)
            selectStation(getStation(pl, false));
        if (leftClick && sneaking) {
            int sta = getStation(pl, false);
            if (stationList.containsKey(sta))
                removeStation(sta);
        }
    }

    public double dist2(double a, double b, MutablePair<Double, Double> nPos) {
        double c = nPos.getLeft();
        double d = nPos.getRight();
        return (a - c) * (a - c) * sizeH * sizeH + (b - d) * (b - d) * sizeV * sizeV;
    }

    public MutablePair<Boolean, MutablePair<Double, Double>> getSight(Player pl) {
        if (!editingPlayer.contains(pl))
            return MutablePair.of(false, new MutablePair<>());
        Location eye = pl.getEyeLocation();
        if (!eye.getWorld().equals(location.getWorld()))
            return MutablePair.of(false, new MutablePair<>());
        Vector eyeOffset = eye.toVector().subtract(location.toVector());
        Vector sight = eye.getDirection();
        Vector normal = normal();
        if (sight.dot(normal) >= -0.1)
            return MutablePair.of(false, new MutablePair<>());
        Vector pointing = sight.multiply(-(eyeOffset.dot(normal)) / (sight.dot(normal)));
        Vector onMap = pointing.add(eyeOffset);
        double normH = onMap.dot(hDir) / sizeH;
        double normV = onMap.dot(new Vector(0.0, 1.0, 0.0)) / sizeV;
        boolean valid = (0.0 <= normH && normH <= 1.0 && 0.0 <= normV && normV <= 1.0);
        return MutablePair.of(valid, MutablePair.of(normH, normV));
    }

    public int newStation(MutablePair<Double, Double> nPos, @NotNull Consumer<StationWrapper> consumer) {
        consumer.accept(stationList.put(nextId, new StationWrapper(nPos)));
        broadcastMessage("Successfully created Station " + nextId);
        ++nextId;
        return nextId - 1;
    }

    public void selectStation(int key) {
        if (!stationList.containsKey(key))
            return;
        broadcastMessage("Station " + key + " is selected");
        if (stationList.containsKey(previousStation))
            stationList.get(previousStation).autoMark();
        if (stationList.containsKey(currentStation))
            stationList.get(currentStation).mark(StationWrapper.PREVIOUS);
        stationList.get(key).mark(StationWrapper.SELECTED);
        previousStation = currentStation;
        currentStation = key;
    }

    public void selectLine(int line) {
        if (activeLine == line || line <= 0) return;
        broadcastMessage("Line " + line + " is selected");
        activeLine = line;
        currentStation = -1;
        previousStation = -1;
        for (StationWrapper stw: stationList.values())
            stw.autoMark();
    }

    public void connect(int from, int to, int line) {
        if (line < 0) return;
        broadcastMessage("Added connection " + from + " -> " + to + " on Line " + line);
        if (stationList.containsKey(from) && stationList.containsKey(to) && from != to)
            if (!stationList.get(from).station.neighbour.contains(MutablePair.of(line, to)))
                stationList.get(from).station.neighbour.add(MutablePair.of(line, to));
    }

    public void biConnect() {
        connect(previousStation, currentStation, activeLine);
        connect(currentStation, previousStation, activeLine);
    }

    public void singleConnect() {
        connect(previousStation, currentStation, activeLine);
    }

    public void disconnect(int from, BiFunction<Integer, Integer, Boolean> filter) {
        if (!stationList.containsKey(from)) return;
        stationList.get(from).station.neighbour.forEach(pair -> {
            if (filter.apply(pair.getLeft(), pair.getRight()))
                broadcastMessage("Removed connection " + from + " -> " + pair.getRight() + " on Line " + pair.getLeft());
        });
        stationList.get(from).station.neighbour.removeIf(nb -> filter.apply(nb.getLeft(), nb.getRight()));
    }

    public void loadFrom(@NotNull Railmap map) {
        AtomicInteger nid = new AtomicInteger(nextId);
        map.station.forEach((Integer id, Station sta) -> {
            // broadcastMessage("Loading station " + id);
            stationList.put(id, new StationWrapper(sta));
            // broadcastMessage("Loaded station " + id);
            if (id >= nid.get())
                nid.set(id + 1);
            // broadcastMessage("Updated nextId to " + nid.get());
        });
        nextId = nid.get();
        for (int i: map.spawn)
            stationList.get(i).isSpawn = true;
        spawnRepellence.addAll(map.spawnRepellence);
        transferRepellence.addAll(map.transferRepellence);
        name = map.name;
    }

    public void removeStation(int sta) {
        if (!stationList.containsKey(sta)) return;
        broadcastMessage("Removed Station " + sta);
        for (MutablePair<Integer, Integer> s: stationList.get(sta).station.neighbour)
            disconnect(s.getRight(), (Integer line, Integer st) -> (st == sta));
        stationList.get(sta).close();
        stationList.remove(sta);
    }

    public void removeLine(int line) {
        stationList.forEach((Integer key, StationWrapper value) -> {
            if (value.isOn(line))
                disconnect(key, (Integer li, Integer st) -> (li == line));
        });
    }

    public Railmap toRailmap(boolean autoSet) {
        Railmap rmp = new Railmap();
        if (autoSet) autoSet();
        rmp.name = name;
        rmp.spawnRepellence.addAll(spawnRepellence);
        rmp.transferRepellence.addAll(transferRepellence);
        stationList.forEach((id, sta) -> rmp.station.put(id, sta.station));
        stationList.forEach((id, sta) -> {
            if (sta.isSpawn)
                rmp.spawn.add(id);
        });
//      broadcastMessage("Map saved");
        return rmp;
    }

    public Railmap toRailmap(boolean autoSet, String newName) {
        Railmap rmp = new Railmap();
        if (autoSet) autoSet();
        rmp.name = newName.replaceAll("\\s+", "");
        rmp.spawnRepellence.addAll(spawnRepellence);
        rmp.transferRepellence.addAll(transferRepellence);
        stationList.forEach((id, sta) -> rmp.station.put(id, sta.station));
        stationList.forEach((id, sta) -> {
            if (sta.isSpawn)
                rmp.spawn.add(id);
        });
//      broadcastMessage("Map saved as " + newName);
        return rmp;
    }

    public void autoSet() {
        for (StationWrapper stw: stationList.values())
            stw.autoSet(true, true);
    }

    public Location mid() {
        Location res = location.clone();
        Vector vec = hDir.clone();
        return res.add(vec.multiply(sizeH * 0.5));
    }

    public boolean isNearBy(Player pl) {
        return mid().getNearbyLivingEntities(RailchessStand.RANGE).contains(pl);
    }

    public void close() {
        available = false;
        stationList.forEach((ignored, st) -> st.close());
        for (Player plw: editingPlayer)
            plugin.playerInEditor.remove(plw.getName());
        editingPlayer.clear();
        stand.editor = null;
    }

    public void refresh() {
        stationList.forEach((a, b) -> b.autoMark());
        if (stationList.containsKey(currentStation))
            stationList.get(currentStation).mark(StationWrapper.SELECTED);
        if (stationList.containsKey(previousStation))
            stationList.get(previousStation).mark(StationWrapper.SELECTED);
    }

    public class StationWrapper {

        Station station;
        ItemDisplay entity;
        boolean isSpawn;
        public static final ItemStack NORMAL = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS);
        public static final ItemStack HIGHLIGHT = new ItemStack(Material.LIME_STAINED_GLASS);
        public static final ItemStack SELECTED = new ItemStack(Material.ORANGE_CONCRETE);
        public static final ItemStack PREVIOUS = new ItemStack(Material.ORANGE_STAINED_GLASS);
        public static final ItemStack SPAWN = new ItemStack(Material.YELLOW_STAINED_GLASS);
    //  public static final ItemStack NEIGHBOUR = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS);

        public void mark(ItemStack item) {
            Location loc = getLocation();
            if (entity == null || !entity.isValid()) {
                entity = (ItemDisplay) location.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
                entity.setBrightness(new Display.Brightness(15, 15));
                entity.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f),
                        new Quaternionf(), new Vector3f(0.15f, 0.15f, 0.15f), new Quaternionf()));
            }
            entity.setItemStack(item);
            entity.setInvulnerable(true);
        }

        public void autoSet(boolean autoSpawn, boolean autoValue) {
            List<Integer> instances = new ArrayList<>();
            for (MutablePair<Integer, Integer> nb: station.neighbour)
                if (!instances.contains(nb.getRight()))
                    instances.add(nb.getRight());
            if (autoValue) station.value = instances.size();
            if (autoSpawn) {
                isSpawn = (instances.size() == 1);
                autoMark();
            }
        }

        public void autoMark() {
            if (isSpawn) mark(SPAWN);
            else if (isOn(activeLine)) mark(HIGHLIGHT);
            else mark(NORMAL);
        }

        public boolean isOn(int line) {
            for (MutablePair<Integer, Integer> nb: station.neighbour)
                if (nb.getLeft() == line)
                    return true;
            return false;
        }

        public void close() {
            entity.remove();
        }

        public Location getLocation() {
            World world = location.getWorld();
            Vector coord = new Vector(location.getX(), location.getY(), location.getZ());
            coord.add(hDir.clone().normalize().multiply(station.normPos.getLeft() * sizeH));
            coord.add(new Vector(0.0, sizeV, 0.0).multiply(station.normPos.getRight()));
            return new Location(world, coord.getX(), coord.getY(), coord.getZ());
        }

        StationWrapper(Station sta) {
            isSpawn = false;
            station = sta.clone();
//          broadcastMessage("Attempting to create StationWrapper at normPos " +
//                  station.normPos.getLeft() + ", " + station.normPos.getRight());
            mark(NORMAL);
//          broadcastMessage("Created StationWrapper from station");
        }

        StationWrapper(MutablePair<Double, Double> nPos) {
            isSpawn = false;
            station = new Station();
            station.normPos = nPos;
            mark(SELECTED);
        }

    }

}
