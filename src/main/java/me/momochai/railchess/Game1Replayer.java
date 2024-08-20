package me.momochai.railchess;

import org.apache.commons.lang3.tuple.MutablePair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game1Replayer {

    Railchess plugin;
    RailchessStand stand;
    Railmap map;
    Game1Logger logger;
    List<Player> subscriberList = new ArrayList<>();
    int currentStep;
    int n;

    MutablePair<ItemStack, ItemStack> displayTiles(int colour) {
        return switch (colour) {
            case 1 ->
                    MutablePair.of(new ItemStack(Material.YELLOW_STAINED_GLASS), new ItemStack(Material.YELLOW_CONCRETE));
            case 2 ->
                    MutablePair.of(new ItemStack(Material.PINK_STAINED_GLASS), new ItemStack(Material.PINK_CONCRETE));
            case 3 ->
                    MutablePair.of(new ItemStack(Material.LIME_STAINED_GLASS), new ItemStack(Material.LIME_CONCRETE));
            case 4 ->
                    MutablePair.of(new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS), new ItemStack(Material.LIGHT_BLUE_CONCRETE));
            default ->
                    MutablePair.of(new ItemStack(Material.BLACK_STAINED_GLASS), new ItemStack(Material.BLACK_CONCRETE));
        };
    }

    public void close() {
        stationList.forEach((id, st) -> st.close());
    }

    public class StationWrapper {

        Station station;
        ItemDisplay entity, entity2;
        public static final ItemStack NORMAL = new ItemStack(Material.AIR);
        public static final ItemStack DEAD = new ItemStack(Material.GRAY_STAINED_GLASS);

        public void mark(ItemStack item) {
            Location loc = getLocation();
            if (entity == null || !entity.isValid()) {
                entity = (ItemDisplay) stand.location.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
                entity.setBrightness(new Display.Brightness(15, 0));
                entity.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f),
                        new Quaternionf(), new Vector3f(0.1f, 0.1f, 0.1f), new Quaternionf()));
            }
            entity.setItemStack(item);
            entity.setInvulnerable(true);
            if (entity2 == null || !entity2.isValid()) {
                entity2 = (ItemDisplay) stand.location.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
                entity2.setBrightness(new Display.Brightness(15, 0));
                entity2.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f),
                        new Quaternionf(), new Vector3f(0.1f, 0.1f, 0.1f), new Quaternionf()));
            }
            entity2.setItemStack(item);
            entity2.setInvulnerable(true);
        }

        public void close() {
            entity.remove();
            entity2.remove();
        }

        public Location getLocation() {
            World world = stand.location.getWorld();
            Vector coord = new Vector(stand.location.getX(), stand.location.getY(), stand.location.getZ());
            coord.add(stand.hDir.clone().normalize().multiply(station.normPos.getLeft() * stand.sizeH));
            coord.add(new Vector(0.0, stand.sizeV, 0.0).multiply(station.normPos.getRight()));
            return new Location(world, coord.getX(), coord.getY(), coord.getZ());
        }

        StationWrapper(@NotNull Station sta) {
            station = sta.clone();
//          broadcastMessage("Attempting to create StationWrapper at normPos " +
//                  station.normPos.getLeft() + ", " + station.normPos.getRight());
            mark(NORMAL);
//          broadcastMessage("Created StationWrapper from station");
        }

//        StationWrapper(MutablePair<Double, Double> nPos) {
//            station = new Station();
//            station.normPos = nPos;
//            mark(NORMAL);
//        }

    }

    Map<Integer, StationWrapper> stationList = new HashMap<>();

    public void advance(int increment) {
        currentStep += increment;
        if (currentStep < 0 || currentStep >= logger.totalMoves) {
            currentStep -= increment;
            return;
        }
        display();
    }

    public void jumpTo(int to) {
        if (to < 0 || to >= logger.totalMoves)
            return;
        currentStep = to;
        display();
    }

    public void display() {
        stationList.forEach((id, stw) -> stw.mark(getItem(id)));
        broadcast("Step " + currentStep + " / " + logger.totalMoves);
        if (logger.getMove(currentStep) == null) return;
        for (String str: logger.getMove(currentStep).buffer)
            broadcast(str);
    }

    @Contract("_ -> new")
    public final @NotNull ItemStack getItem(int id) {
        Game1Logger.Move move = logger.getMove(id);
        if (move == null) return new ItemStack(Material.BLACK_CONCRETE);
        for (int i = 0; i < n; ++i) {
            if (id == move.positions.get(i))
                return displayTiles(logger.playerColour.get(id)).getRight();
        }
        if (move.occupier.get(id) == -1) return StationWrapper.NORMAL;
        if (move.occupier.get(id) == -2) return StationWrapper.DEAD;
        return displayTiles(move.occupier.get(id)).getLeft();
    }

    public void broadcast(final String str) {
        for (Player pl: subscriberList)
            pl.sendMessage(str);
    }

    public void playerJoin(@NotNull Player pl) {
        if (subscriberList.contains(pl) || !plugin.isAvailable(pl, true))
            return;
        subscriberList.add(pl);
        plugin.playerInReplay.put(pl.getName(), this);
        broadcast(pl.getName() + "joined the replay");
    }

    public void playerLeave(@NotNull Player pl) {
        if (!subscriberList.contains(pl)) return;
        broadcast(pl.getName() + "left");
        plugin.playerInReplay.remove(pl.getName());
        subscriberList.remove(pl);
    }

    Game1Replayer(Railchess p, RailchessStand s, Railmap m, Game1Logger l) {
        plugin = p;
        stand = s;
        map = m;
        logger = l;
        s.replayer = this;
        n = logger.playerColour.size();
        map.station.forEach((id, sta) -> stationList.put(id, new StationWrapper(sta)));
        jumpTo(0);
    }

}
