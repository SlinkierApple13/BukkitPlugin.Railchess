package me.momochai.railchess;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

public class MapCommandHandler implements CommandExecutor {

    public class RenameTask extends BukkitRunnable {

        long id;
        Player player;

        RenameTask(long _id, @NotNull Player pl) {
            player = pl;
            id = _id;
            runTaskAsynchronously(plugin);
        }

        @Override
        public void run() {
            plugin.railmap.get(id).save(new File(plugin.mapFolder, id + ".railmap"));
            player.sendMessage("已将地图重命名为 " + plugin.railmap.get(id).name + ".");
        }

    }

    Railchess plugin;

    MapCommandHandler(Railchess p) {
        plugin = p;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            Player player = sender.getServer().getPlayer(sender.getName());
            if (!player.hasPermission("railchess.edit")) return false;
            if (Objects.equals(args[0], "list")) {
                Railchess.sendMessage(player, "共有 " + plugin.railmap.size() + " 张地图:");
                plugin.railmap.forEach((id, rmp) ->
                    player.sendMessage("地图 " + rmp.name + ((rmp.readOnly) ? ": 只读." : ".")));
                return true;
            }
            if (Objects.equals(args[0], "rename")) {
                if (args[1] == null || args[2] == null)
                    return false;
                String from = args[1].replaceAll("\\s+", "");
                String to = args[2].replaceAll("\\s+", "");
                if (!plugin.railmapDict.containsKey(from) || plugin.railmapDict.containsKey(to))
                    return false;
                plugin.getMap(from).name = to;
                new RenameTask(plugin.railmapDict.get(from), player);
                plugin.railmapDict.put(to, plugin.railmapDict.get(from));
                plugin.railmapDict.remove(from);
            }
        } catch (Exception ignored) {}
        return false;
    }

}
