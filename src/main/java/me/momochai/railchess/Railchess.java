package me.momochai.railchess;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public final class Railchess extends JavaPlugin {

    public List<RailchessStand> stand = new ArrayList<>();
    public Map<String, Railmap> railmap = new HashMap<>();
    public Map<String, MapEditor> playerInEditor = new HashMap<>(); // key = playerName
    public Map<String, Game1> playerInGame = new HashMap<>(); // key = playerName
    public Map<String, RailchessStand> playerInStand = new HashMap<>(); // key = playerName
    public Map<String, Game1> playerSubGame = new HashMap<>();
    public File mapFolder;
    public File standFolder;
    public boolean loaded = false;

    public void saveAll() {
        stand.forEach(st -> st.save(new File(standFolder, st.fileName)));
        // railmap.forEach((name, map) -> map.save(new File(mapFolder, name + ".railmap")));
    }

    public void closeAll() {
        stand.forEach(st -> {
           if (st.editor != null)
               st.editor.close();
           if (st.game != null)
               st.game.close();
        });
    }

    public void loadMaps() {
        mapFolder.mkdirs();
        try {
            for (File f: Objects.requireNonNull(mapFolder.listFiles((File file) ->
                    file.getName().endsWith(".railmap")))) {
                Railmap rmp = new Railmap(f);
                if (rmp.valid) railmap.put(rmp.name, rmp);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void loadStands() {
        standFolder.mkdirs();
        try {
            for (File f: Objects.requireNonNull(standFolder.listFiles(file ->
                    file.getName().endsWith(".stand")))) {
                RailchessStand rcs = new RailchessStand(this, f);
                if (rcs.valid) stand.add(rcs);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new RailchessListener(this), this);
        getCommand("rc").setExecutor(new RailchessCommandHandler(this));
        getCommand("rcedit").setExecutor(new EditorCommandHandler(this));
        getCommand("rcgame").setExecutor(new GameCommandHandler(this));
        mapFolder = new File(getDataFolder(), "railmap");
        standFolder = new File(getDataFolder(), "stand");
    }

    @Override
    public void onDisable() {
        closeAll();
        if (loaded) saveAll();
    }
}
