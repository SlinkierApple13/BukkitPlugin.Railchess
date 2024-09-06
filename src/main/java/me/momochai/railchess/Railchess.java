package me.momochai.railchess;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public final class Railchess extends JavaPlugin {

    public List<RailchessStand> stand = new ArrayList<>();
    public Map<Long, Railmap> railmap = new HashMap<>();
    public Map<String, Long> railmapDict = new HashMap<>();
    public Map<String, MapEditor> playerInEditor = new HashMap<>(); // key = playerName
    public Map<String, Game1> playerInGame = new HashMap<>(); // key = playerName
    public Map<String, RailchessStand> playerInStand = new HashMap<>(); // key = playerName
    public Map<String, Game1> playerSubGame = new HashMap<>(); // key = playerName
    public Map<String, Game1Replayer> playerInReplay = new HashMap<>(); // key = playerName
    public Map<Long, Game1Logger> logList = new HashMap<>();
    public File mapFolder;
    public File standFolder;
    public File logFolder;
    public final Railchess _thisPlugin = this;

    public boolean isAvailable(@NotNull Player player, boolean strict) {
        return !(playerInGame.containsKey(player.getName()) || playerInEditor.containsKey(player.getName()) || playerInReplay.containsKey(player.getName()) ||
                (strict && playerSubGame.containsKey(player.getName())) || playerInStand.containsKey(player.getName()));
    }

    public @Nullable RailchessStand nearbyStand(@NotNull Player player) {
        for (RailchessStand rcs: stand)
            if (rcs.isNearBy(player)) return rcs;
        return null;
    }

    public boolean isNearbyStand(@NotNull Player player) {
        for (RailchessStand rcs: stand)
            if (rcs.isNearBy(player)) return true;
        return false;
    }

    public void leaveAll(@NotNull Player pl) {
        if (playerInEditor.containsKey(pl.getName())) {
            playerInEditor.get(pl.getName()).editingPlayer.remove(pl);
        }
        if (playerInGame.containsKey(pl.getName())) {
            playerInGame.get(pl.getName()).subscriber.remove(pl);
            while (playerInGame.get(pl.getName()).getPlayerWrapper(pl) != null)
                playerInGame.get(pl.getName()).getPlayerWrapper(pl).quit(false, "", true, false);
        }
        if (playerSubGame.containsKey(pl.getName())) {
            playerSubGame.get(pl.getName()).desubscribe(pl);
        }
        if (playerInReplay.containsKey(pl.getName())) {
            playerInReplay.get(pl.getName()).playerLeave(pl);
        }
        playerInEditor.remove(pl.getName());
        playerInGame.remove(pl.getName());
        playerSubGame.remove(pl.getName());
        playerInReplay.remove(pl.getName());
    }

    public @Nullable Railmap getMap(String name) {
        if (!railmapDict.containsKey(name))
            return null;
        if (!railmap.containsKey(railmapDict.get(name)))
            return null;
        return railmap.get(railmapDict.get(name));
    }

    public void addMap(Railmap map) {
        railmap.put(map.mapId, map);
        railmapDict.put(map.name, map.mapId);
    }

//    public void saveAll() {
//        stand.forEach(st -> st.save0(new File(standFolder, st.fileName)));
//    }

    public void closeAll() {
        stand.forEach(st -> {
           if (st.editor != null)
               st.editor.close();
           if (st.game != null)
               st.game.close();
           if (st.replayer != null)
               st.replayer.close();
        });
    }

    public boolean loadMaps() {
        mapFolder.mkdirs();
        try {
            for (File f: Objects.requireNonNull(mapFolder.listFiles(file ->
                    file.getName().endsWith(".railmap")))) {
                Railmap rmp = new Railmap(f);
                if (rmp.valid) addMap(rmp);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, e.getMessage());
            return false;
        }
        return true;
    }

    public boolean loadLogs() {
        logFolder.mkdirs();
        try {
            for (File f: Objects.requireNonNull(logFolder.listFiles(file ->
                    file.getName().endsWith(".game1")))) {
                Game1Logger log = new Game1Logger(f);
                if (log.valid) logList.put(log.logId, log);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, e.getMessage());
            return false;
        }
        return true;
    }

    public boolean loadStands() {
        standFolder.mkdirs();
        try {
            for (File f: Objects.requireNonNull(standFolder.listFiles(file ->
                    file.getName().endsWith(".stand")))) {
                RailchessStand rcs = new RailchessStand(this, f);
                if (rcs.valid) stand.add(rcs);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, e.getMessage());
            return false;
        }
        return true;
    }

    public boolean loadLog(long logId) {
        logFolder.mkdirs();
        try {
            File f = new File(logFolder, logId + ".game1");
            Game1Logger log = new Game1Logger(f);
            if (log.valid) logList.put(log.logId, log);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, e.getMessage());
            return false;
        }
        return true;
    }

    public class LoadAll extends BukkitRunnable {
        @Override
        public void run() {
            loadStands();
            loadMaps();
            loadLogs();
        }

        LoadAll() {
            this.runTaskAsynchronously(_thisPlugin);
        }

    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new RailchessListener(this), this);
        Objects.requireNonNull(getCommand("rc")).setExecutor(new RailchessCommandHandler(this));
        Objects.requireNonNull(getCommand("rcedit")).setExecutor(new EditorCommandHandler(this));
        Objects.requireNonNull(getCommand("rcgame")).setExecutor(new GameCommandHandler(this));
        Objects.requireNonNull(getCommand("rcmap")).setExecutor(new MapCommandHandler(this));
        Objects.requireNonNull(getCommand("rcreplay")).setExecutor(new ReplayerCommandHandler(this));
        Objects.requireNonNull(getCommand("rclog")).setExecutor(new LogCommandHandler(this));
        mapFolder = new File(getDataFolder(), "railmap");
        logFolder = new File(getDataFolder(), "log");
        standFolder = new File(getDataFolder(), "stand");
        new LoadAll();
    }

    @Override
    public void onDisable() {
        closeAll();
    }

}
