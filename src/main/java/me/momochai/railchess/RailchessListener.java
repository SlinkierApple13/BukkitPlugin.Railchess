package me.momochai.railchess;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RailchessListener implements Listener {

    Railchess plugin;
    public static final Material ITEM = Material.BLAZE_ROD;

    RailchessListener(Railchess p) {
        plugin = p;
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onPlayerInteract(@NotNull PlayerInteractEvent e) {
        try {
            if (!Objects.requireNonNull(e.getItem()).getType().equals(ITEM))
                return;
        } catch (NullPointerException exception) { return; }
        if (!e.getPlayer().hasPermission("railchess.edit") && !e.getPlayer().hasPermission("railchess.play"))
            return;
//      Bukkit.broadcastMessage("Event Detected: " + e.getPlayer().getName());
        if (plugin.playerInGame.containsKey(e.getPlayer().getName())) {
            Game1 g = plugin.playerInGame.get(e.getPlayer().getName());
            if (g.available && (e.getAction().equals(Action.RIGHT_CLICK_AIR) ||
                    e.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
                g.play(e.getPlayer());
            }
            e.setCancelled(true);
        } else if (plugin.playerInEditor.containsKey(e.getPlayer().getName())) {
            MapEditor ed = plugin.playerInEditor.get(e.getPlayer().getName());
            if (ed.available) {
                if (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
                    ed.parse(e.getPlayer(), false, e.getPlayer().isSneaking());
                if (e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK))
                    ed.parse(e.getPlayer(), true, e.getPlayer().isSneaking());
            }
            e.setCancelled(true);
        }
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onPlayerJoin(@NotNull PlayerJoinEvent e) {
        if (!plugin.loaded) {
            plugin.loadMaps();
            plugin.loadStands();
        }
        Player pl = e.getPlayer();
        if (plugin.playerInEditor.containsKey(pl.getName())) {
            plugin.playerInEditor.get(pl.getName()).editingPlayer.remove(pl);
        }
        if (plugin.playerInGame.containsKey(pl.getName())) {
            plugin.playerInGame.get(pl.getName()).subscriber.remove(pl);
            plugin.playerInGame.get(pl.getName()).getPlayerWrapper(pl).quit(false, "");
        }
        plugin.playerInEditor.remove(pl.getName());
        plugin.playerInGame.remove(pl.getName());
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onPlayerLeave(@NotNull PlayerQuitEvent e) {
        Player pl = e.getPlayer();
        if (plugin.playerInEditor.containsKey(pl.getName())) {
            plugin.playerInEditor.get(pl.getName()).editingPlayer.remove(pl);
        }
        if (plugin.playerInGame.containsKey(pl.getName())) {
            plugin.playerInGame.get(pl.getName()).subscriber.remove(pl);
            plugin.playerInGame.get(pl.getName()).getPlayerWrapper(pl).quit(false, "");
        }
        plugin.playerInEditor.remove(pl.getName());
        plugin.playerInGame.remove(pl.getName());
    }

}
