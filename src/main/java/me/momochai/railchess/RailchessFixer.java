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
            if (!player.hasPermission("railchess.edit")) return false;
            Location loc = player.getLocation();
            Collection<Entity> itds = loc.getNearbyEntitiesByType(ItemDisplay.class,15.0f);
            for (Entity e: itds) {
                if (e.getScoreboardTags().contains("railchess"))
                    e.remove();
            }
            Railchess.sendMessage(player, "已清除 15m 内的轨交棋展示实体.");
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
