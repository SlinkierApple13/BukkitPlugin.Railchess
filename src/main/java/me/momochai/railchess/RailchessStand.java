package me.momochai.railchess;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RailchessStand {

    Railchess plugin;
    String fileName;
    Location location;
    Vector hDir;
    double sizeH;
    double sizeV;
    List<Player> players = new ArrayList<>();
    public static final double RANGE = 8.0;
    MapEditor editor = null;
    Game1 game = null;
    boolean valid = false;

    public void broadcast(String s) {
        for (Player pl: players)
            pl.sendMessage(s);
    }

    public boolean occupied() {
        return !(editor == null && game == null);
    }

    RailchessStand(Railchess p, Location loc, Vector h, double sH, double sV) {
        plugin = p;
        location = loc;
        hDir = h;
        sizeH = sH;
        sizeV = sV;
    }

    RailchessStand(Railchess p, File file) {
        plugin = p;
        load(file);
    }

    public void load(@NotNull File file) {
        try {
            fileName = file.getName();
            Scanner scanner = new Scanner(file, StandardCharsets.US_ASCII);
            int version = scanner.nextInt();
            if (version != 0) {
                System.out.println(fileName + " failed to load: version = " + version);
                scanner.close();
                return;
            }
            scanner.nextLine();
            String world = scanner.nextLine().replaceAll("\\s+","");
            double x = scanner.nextDouble();
            double y = scanner.nextDouble();
            double z = scanner.nextDouble();
            location = new Location(Bukkit.getWorld(world), x, y, z);
            double h = scanner.nextDouble();
            double v = scanner.nextDouble();
            hDir = new Vector(h, 0.0, v);
            sizeH = scanner.nextDouble();
            sizeV = scanner.nextDouble();
            scanner.close();
            System.out.println(fileName + " loaded successfully");
            valid = true;
        } catch (Exception ignored) {}
    }

    public void save(@NotNull File file) {
        try {
            if (!file.exists()) file.createNewFile();
            PrintWriter writer = new PrintWriter(file, StandardCharsets.US_ASCII);
            writer.println(0);
            writer.println(location.getWorld().getName());
            writer.println(location.getX());
            writer.println(location.getY());
            writer.println(location.getZ());
            writer.println(hDir.getX());
            writer.println(hDir.getZ());
            writer.println(sizeH);
            writer.println(sizeV);
            writer.close();
            System.out.println(fileName + " saved successfully");
        } catch (Exception ignored) {}
    }

    public Location mid() {
        Location res = location.clone();
        Vector vec = hDir.clone();
        return res.add(vec.multiply(sizeH * 0.5));
    }

    public boolean isNearBy(Player pl) {
        return mid().getNearbyLivingEntities(RANGE).contains(pl);
    }

    public int playables() {
        int res = 0;
        for (Player pl: players)
            if (pl.hasPermission("railchess.play"))
                ++res;
        return res;
    }

    public boolean newGame(String mapName, int maxStep) {
        if (occupied() || !plugin.railmap.containsKey(mapName) || playables() <= 1 ||
            maxStep > 16 || maxStep < 1) return false;
        if (!plugin.railmap.containsKey(mapName))
            return false;
        Game1 game1 = new Game1(plugin, this, plugin.railmap.get(mapName), players, location, sizeH, sizeV, maxStep, hDir);
        for (Player pl: players)
            plugin.playerInStand.remove(pl.getName());
        editor = null;
        game = game1;
        broadcast("Game started: Map " + mapName + ", Maximum Steps " + maxStep);
        players.removeIf(__ -> true);
        return true;
    }

    public boolean newEditor(String mapName) {
        if (occupied()) return false;
        MapEditor editor1 = new MapEditor(plugin, this, mapName, location, hDir, sizeH, sizeV);
        game = null;
        editor = editor1;
        for (Player pl: players)
            plugin.playerInStand.remove(pl.getName());
        players.removeIf(__ -> true);
        return true;
    }

    public boolean playerJoin(Player pl) {
        if (!occupied() && !players.contains(pl) && players.size() < 4) {
            broadcast(pl.getName() + " joined");
            pl.sendMessage("Joined stand " + fileName);
            players.add(pl);
            plugin.playerInStand.put(pl.getName(), this);
            return true;
        }
        return false;
    }

    public boolean playerForceJoin(Player pl) {
        if (!occupied() && players.size() < 4) {
            broadcast(pl.getName() + " joined");
            pl.sendMessage("Joined stand " + fileName);
            players.add(pl);
            plugin.playerInStand.put(pl.getName(), this);
            return true;
        }
        return false;
    }

    public boolean playerLeave(Player pl) {
        if (!players.contains(pl))
            return false;
        broadcast(pl.getName() + " left");
        players.remove(pl);
        plugin.playerInStand.remove(pl.getName());
        return true;
    }

}
