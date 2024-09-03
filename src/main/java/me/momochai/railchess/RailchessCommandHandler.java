package me.momochai.railchess;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.Objects;

public class RailchessCommandHandler implements CommandExecutor {

    Railchess plugin;

    RailchessCommandHandler(Railchess p) {
        plugin = p;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args[0].equals("list")) {
                for (RailchessStand stand: plugin.stand)
                    sender.sendMessage(stand.fileName + " in " + stand.location.getWorld().getName() + " at (" +
                            stand.location.getX() + ", " + stand.location.getY() + ", " + stand.location.getZ() + ")");
            }
            Player player = sender.getServer().getPlayer(sender.getName());
            if (player == null) return false;
            if (!player.hasPermission("railchess.edit") && !player.hasPermission("railchess.play"))
                return false;
            Location location = player.getLocation().toBlockLocation();
            if (args[0].equals("create") && player.hasPermission("railchess.edit")) {
                double x = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                Vector hDir = new Vector(x, 0.0, z);
                double sH = Double.parseDouble(args[3]);
                double sV = Double.parseDouble(args[4]);
                hDir.normalize();
                RailchessStand stand = new RailchessStand(plugin, location, hDir, sH, sV);
                plugin.stand.add(stand);
                stand.fileName = System.currentTimeMillis() + ".stand";
                player.sendMessage("Successfully created " + stand.fileName);
                stand.save(new File(plugin.standFolder, stand.fileName));
            } else if (args[0].equals("join")) {
                if (plugin.playerInGame.containsKey(player.getName()) ||
                        plugin.playerInEditor.containsKey(player.getName()) ||
                        plugin.playerInStand.containsKey(player.getName()))
                    return false;
                if (plugin.playerSubGame.containsKey(player.getName()))
                    plugin.playerSubGame.get(player.getName()).desubscribe(player);
                for (RailchessStand stand: plugin.stand)
                    if (!stand.occupied() && stand.isNearBy(player)) {
                        stand.playerJoin(player);
                        plugin.playerInStand.put(player.getName(), stand);
                        break;
                    }
            } else if (args[0].equals("leave")) {
                if (!plugin.playerInStand.containsKey(player.getName()))
                    return false;
                RailchessStand stand = plugin.playerInStand.get(player.getName());
                stand.playerLeave(player);
                plugin.playerInStand.remove(player.getName());
            } else if (args[0].equals("edit")) {
                if (!player.hasPermission("railchess.edit"))
                    return false;
                if (!plugin.playerInStand.containsKey(player.getName()))
                    return false;
                RailchessStand stand = plugin.playerInStand.get(player.getName());
                if (args[1].isBlank()) return false;
                stand.broadcast("Editing map " + args[1]);
                stand.newEditor(args[1].replaceAll("\\s+", ""));
            } else if (args[0].equals("game") || args[0].equals("play")) {
                if (!player.hasPermission("railchess.play"))
                    return false;
                if (!plugin.playerInStand.containsKey(player.getName()))
                    return false;
                if (args[1] == null || args[2] == null || args[1].isBlank() || args[2].isBlank() ||
                        args[3] == null || args[3].isBlank() || args[4] == null || args[4].isBlank())
                    return false;
                if (Integer.parseInt(args[2]) <= 0 || Integer.parseInt(args[2]) >= 13)
                    return false;
                RailchessStand stand = plugin.playerInStand.get(player.getName());
                int maxSteps = Integer.parseInt(args[2]);
                int maxHurt = Integer.parseInt(args[3]);
                boolean showChoices = Boolean.parseBoolean(args[4]);
                return stand.newGame(args[1], maxSteps, maxHurt, showChoices);
            } else if (args[0].equals("remove")) {
                if (!player.hasPermission("railchess.edit"))
                    return false;
                for (RailchessStand stand: plugin.stand) {
                    if (Objects.equals(stand.fileName, args[1])) {
                        if (stand.occupied()) break;
                        for (Player pl: stand.players)
                            stand.playerLeave(pl);
                        plugin.stand.remove(stand);
                        player.sendMessage("Successfully removed " + args[1]);
                        return new File(plugin.standFolder, stand.fileName).delete();
                    }
                }
            } else if (args[0].equals("duplicate")) {
                if (!player.hasPermission("railchess.edit"))
                    return false;
                plugin.playerInStand.get(player.getName()).playerForceJoin(player);
            } else if (args[0].equals("replay")) {
                long logId = Long.parseLong(args[1]);
                if (plugin.logList.containsKey(logId)) {
                    Game1Logger log = plugin.logList.get(logId);
                    if (plugin.railmap.containsKey(log.mapId)) {
                        plugin.playerInStand.get(player.getName()).newReplayer(log.mapId, logId);
                        return true;
                    }
                }
                return false;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
