package me.momochai.railchess;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class LogCommandHandler implements CommandExecutor {

    Railchess plugin;

    LogCommandHandler(Railchess p) {
        plugin = p;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (args[0].equals("list")) {
            plugin.logList.forEach((id, log) -> {
               String str = log.brief(plugin);
               if (str != null) sender.sendMessage(str);
            });
            return true;
        }
        return false;
    }

}
