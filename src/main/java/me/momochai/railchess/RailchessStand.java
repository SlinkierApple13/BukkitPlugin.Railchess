package me.momochai.railchess;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class RailchessStand {

    Railchess plugin;
    String fileName;
    Location location;
    Vector hDir;
    double sizeH;
    double sizeV;
    List<Player> players = new ArrayList<>();
    public static final double RANGE = 10.0d;
    MapEditor editor = null;
    Game1 game = null;
    Game1Replayer replayer = null;
    boolean valid = false;

    public void broadcast(String s) {
        List<Player> subscriber = new ArrayList<>();
        for (Player pl: players) {
            if (subscriber.contains(pl))
                continue;
            pl.sendMessage(s);
            subscriber.add(pl);
        }
    }

    public boolean occupied() {
        return !(editor == null && game == null && replayer == null);
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
        try {
            if (!file.exists()) file.createNewFile();
        } catch (IOException ignored) {}
    }

    public void load(@NotNull File file) {
        try {
            fileName = file.getName();
            Scanner scanner = new Scanner(file, StandardCharsets.US_ASCII);
            int version = scanner.nextInt();
            if (version != 0) {
                Bukkit.getLogger().log(Level.INFO, fileName + " failed to load: version = " + version);
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
            Bukkit.getLogger().log(Level.INFO, "Successfully loaded " + fileName);
            valid = true;
        } catch (Exception ignored) {}
    }

    class SaveTask extends BukkitRunnable {

        File file;

        @Override
        public void run() {
            save0(file);
        }

        SaveTask(@NotNull File f) {
            file = f;
            this.runTaskAsynchronously(plugin);
        }

    }

    public void save(@NotNull File file) {
        new SaveTask(file);
    }

    public void save0(@NotNull File file) {
        try {
            file.createNewFile();
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
            Bukkit.getLogger().log(Level.INFO, "Successfully saved " + fileName);
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

    public boolean newGame(String mapName, int maxStep, int maxHurt, boolean showChoices) {
        if (occupied() || !plugin.railmapDict.containsKey(mapName) || playables() <= 1 ||
            maxStep > 16 || maxStep < 1) return false;
        new Game1(plugin, this, Objects.requireNonNull(plugin.getMap(mapName)), players,
                location, sizeH, sizeV, maxStep, hDir, maxHurt, showChoices);
        for (Player pl: players)
            plugin.playerInStand.remove(pl.getName());
        if (editor != null) editor.close();
        if (replayer != null) replayer.close();
        editor = null;
        replayer = null;
        players = new ArrayList<>();
        return true;
    }

    public boolean newEditor(String mapName) {
        if (occupied()) return false;
        new MapEditor(plugin, this, mapName, location, hDir, sizeH, sizeV);
        if (game != null) game.end();
        if (replayer != null) replayer.close();
        game = null;
        replayer = null;
        for (Player pl: players)
            plugin.playerInStand.remove(pl.getName());
        players = new ArrayList<>();
        return occupied();
    }

    public boolean newReplayer(long mapId, long replayId) {
        if (occupied()) return false;
        if (!plugin.railmap.containsKey(mapId) || !plugin.logList.containsKey(replayId))
            return false;
        Railmap m = plugin.railmap.get(mapId);
        Game1Logger log = plugin.logList.get(replayId);
        new Game1Replayer(plugin, this, m, log);
//      for (Player pl: players)
//          plugin.playerInStand.remove(pl.getName());
        players = new ArrayList<>();
        return occupied();
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
            if (!players.contains(pl)) {
                broadcast(pl.getName() + " joined");
                pl.sendMessage("Joined stand " + fileName);
            } else {
                broadcast("Duplicated player " + pl.getName());
            }
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
        while (players.contains(pl))
            players.remove(pl);
        plugin.playerInStand.remove(pl.getName());
        return true;
    }

}
