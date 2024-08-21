package me.momochai.railchess;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReplayerCommandHandler implements CommandExecutor {

    Railchess plugin;

    ReplayerCommandHandler(Railchess p) {
        plugin = p;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        Player player = sender.getServer().getPlayer(sender.getName());
        if (player == null || !player.hasPermission("railchess.subscribe"))
            return false;
        try {
            if (args[0].equals("join") && plugin.isAvailable(player, true)) {
                RailchessStand stand = plugin.nearbyStand(player);
                if (stand == null) return false;
                stand.replayer.playerJoin(player);
                return true;
            }
            if (args[0].equals("leave") && plugin.playerInReplay.containsKey(player.getName())) {
                plugin.playerInReplay.get(player.getName()).playerLeave(player);
                return true;
            }
            if (args[0].equals("goto") && plugin.playerInReplay.containsKey(player.getName())) {
                int step = Integer.parseInt(args[1]);
                plugin.playerInReplay.get(player.getName()).jumpTo(step);
                return true;
            }
            if (args[0].equals("close") && plugin.playerInReplay.containsKey(player.getName())) {
                plugin.playerInReplay.get(player.getName()).close();
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

}
