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

import java.util.*;

public class Game1Replayer {

    Railchess plugin;
    RailchessStand stand;
    Railmap map;
    Game1Logger logger;
    List<Player> subscriberList = new ArrayList<>();
    int currentStep;
    int n;
    public static final double RANGE = 10.0d;
    boolean closed = false;

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
        closed = true;
        ArrayList<Player> pls = new ArrayList<>(subscriberList);
        broadcast("回放已关闭.");
        pls.forEach(this::playerLeave);
        stationList.forEach((id, st) -> st.close());
        stand.replayer = null;
    }

    public class StationWrapper {

        Station station;
        ItemDisplay entity;
        // ItemDisplay entity2;
        public static final ItemStack NORMAL = new ItemStack(Material.AIR);
        public static final ItemStack DEAD = new ItemStack(Material.GRAY_STAINED_GLASS);

        public void mark(ItemStack item) {
            Location loc = getLocation();
            if (entity == null || !entity.isValid()) {
                entity = (ItemDisplay) stand.location.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
                entity.setBrightness(new Display.Brightness(15, 0));
                entity.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f),
                        new Quaternionf(), new Vector3f(Railchess.BUTTON_SIZE, Railchess.BUTTON_SIZE, Railchess.BUTTON_SIZE), new Quaternionf()));
            }
            entity.setItemStack(item);
            entity.setInvulnerable(true);
            entity.addScoreboardTag("railchess");
//            if (entity2 == null || !entity2.isValid()) {
//                entity2 = (ItemDisplay) stand.location.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
//                entity2.setBrightness(new Display.Brightness(15, 0));
//                entity2.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f),
//                        new Quaternionf(), new Vector3f(Railchess.BUTTON_SIZE, Railchess.BUTTON_SIZE, Railchess.BUTTON_SIZE), new Quaternionf()));
//            }
//            entity2.setItemStack(item);
//            entity2.setInvulnerable(true);
//            entity2.addScoreboardTag("railchess");
        }

        public void close() {
            entity.remove();
            // entity2.remove();
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
//          broadcast("Attempting to create StationWrapper at normPos " +
//                  station.normPos.getLeft() + ", " + station.normPos.getRight());
            mark(NORMAL);
//          broadcast("Created StationWrapper from station");
        }

//      StationWrapper(MutablePair<Double, Double> nPos) {
//          station = new Station();
//          station.normPos = nPos;
//          mark(NORMAL);
//      }

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
        // broadcast("Current Step: " + currentStep);
        display();
    }

    public void display() {
        try {
            if (logger.getMove(currentStep) == null) return;
            broadcast("第 " + currentStep + " / " + (logger.totalMoves - 1) + " 手");
            for (final String str : logger.getMove(currentStep).buffer)
                plainBroadcast(str);
            stationList.forEach((id, stw) -> stw.mark(getItem(id)));
        } catch (ConcurrentModificationException ignored) {}
    }

    @Contract("_ -> new")
    public final @NotNull ItemStack getItem(int stationId) {
        Game1Logger.Move move = logger.getMove(currentStep);
        if (move == null) return new ItemStack(Material.BLACK_CONCRETE);
        for (int i = 0; i < n; ++i) {
            if (stationId == move.positions.get(i))
                return displayTiles(logger.playerColour.get(move.occupier.get(stationId))).getRight();
        }
        if (move.occupier.get(stationId) == -1) return StationWrapper.NORMAL;
        if (move.occupier.get(stationId) == -2) return StationWrapper.DEAD;
        return displayTiles(logger.playerColour.get(move.occupier.get(stationId))).getLeft();
    }

    public void broadcast(String s) {
        // Bukkit.getLogger().log(Level.INFO, s);
        for (Player pl: subscriberList)
            if (pl.isValid())
                Railchess.sendMessage(pl, s, "6对局回放");
        for (Player pl: stand.mid().getNearbyPlayers(RANGE))
            if (!subscriberList.contains(pl))
                Railchess.sendMessage(pl, s, "6对局回放");
    }

    public void plainBroadcast(String s) {
        // Bukkit.getLogger().log(Level.INFO, s);
        for (Player pl: subscriberList)
            if (pl.isValid())
                Railchess.sendMessage(pl, s, "");
        for (Player pl: stand.mid().getNearbyPlayers(RANGE))
            if (!subscriberList.contains(pl))
                Railchess.sendMessage(pl, s, "");
    }

    public void playerJoin(@NotNull Player pl, boolean info) {
        if (subscriberList.contains(pl) || !plugin.isAvailable(pl, true))
            return;
        // if (info) broadcast(pl.getName() + " 加入了回放.");
        subscriberList.add(pl);
        plugin.playerInReplay.put(pl.getName(), this);
        if (info) Railchess.sendMessage(pl, "已加入当前回放.", "6对局回放");
    }

    public void playerLeave(@NotNull Player pl) {
        if (!subscriberList.contains(pl)) return;
        if (!closed) { Railchess.sendMessage(pl, "已离开当前回放.", "6对局回放"); }
        plugin.playerInReplay.remove(pl.getName());
        subscriberList.remove(pl);
        // broadcast(pl.getName() + " 离开了回放.");
    }

    Game1Replayer(Railchess p, @NotNull RailchessStand s, Railmap m, @NotNull Game1Logger l) {
        plugin = p;
        stand = s;
        map = m;
        logger = l;
        s.replayer = this;
        n = logger.playerColour.size();
        stand.players.forEach(pl -> {
            plugin.playerInStand.remove(pl.getName());
            playerJoin(pl, false);
        });
        broadcast("开始回放对局 " + l.logId + ".");
        map.station.forEach((id, sta) -> stationList.put(id, new StationWrapper(sta)));
        jumpTo(0);
    }

}
