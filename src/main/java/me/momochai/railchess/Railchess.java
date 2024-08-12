package me.momochai.railchess;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public final class Railchess extends JavaPlugin {

    public List<RailchessStand> stand = new ArrayList<>();
    public Map<Long, Railmap> railmap = new HashMap<>();
    public Map<String, Long> railmapDict = new HashMap<>();
    public Map<String, MapEditor> playerInEditor = new HashMap<>(); // key = playerName
    public Map<String, Game1> playerInGame = new HashMap<>(); // key = playerName
    public Map<String, RailchessStand> playerInStand = new HashMap<>(); // key = playerName
    public Map<String, Game1> playerSubGame = new HashMap<>();
    // public File deadMapFolder;
    public Map<Long, Game1Logger> logList = new HashMap<>();
    public File mapFolder;
    public File standFolder;
    public File logFolder;
    public boolean loaded = false;

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

    public void saveAll() {
        stand.forEach(st -> st.save(new File(standFolder, st.fileName)));
        // railmapDict.forEach((name, map) -> map.save(new File(mapFolder, map.name + ".railmap")));
    }

    public void closeAll() {
        stand.forEach(st -> {
           if (st.editor != null)
               st.editor.close();
           if (st.game != null)
               st.game.close();
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
            System.out.println(e.getMessage());
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
            System.out.println(e.getMessage());
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
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new RailchessListener(this), this);
        Objects.requireNonNull(getCommand("rc")).setExecutor(new RailchessCommandHandler(this));
        Objects.requireNonNull(getCommand("rcedit")).setExecutor(new EditorCommandHandler(this));
        Objects.requireNonNull(getCommand("rcgame")).setExecutor(new GameCommandHandler(this));
        Objects.requireNonNull(getCommand("rcmap")).setExecutor(new MapCommandHandler(this));
        mapFolder = new File(getDataFolder(), "railmap");
        logFolder = new File(getDataFolder(), "log");
        standFolder = new File(getDataFolder(), "stand");
        loaded = loadMaps() && loadLogs() && loadStands();
    }

    @Override
    public void onDisable() {
        closeAll();
        if (loaded) saveAll();
    }

}
