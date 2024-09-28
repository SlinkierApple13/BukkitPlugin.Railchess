package me.momochai.railchess;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

import java.util.Collection;

public class RailchessFixer implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            Player player = sender.getServer().getPlayer(sender.getName());
            if (player == null) return false;
            if (!player.hasPermission("railchess.op")) return false;
            boolean safe = Boolean.parseBoolean(args[0]);
            Location loc = player.getLocation();
            Collection<Entity> itds = loc.getNearbyEntitiesByType(ItemDisplay.class,15.0f);
            if (!safe) {
                for (Entity e: itds)
                    e.remove();
            }
            else {
                for (Entity e: itds) {
                    if (e.getScoreboardTags().contains("railchess"))
                        e.remove();
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
