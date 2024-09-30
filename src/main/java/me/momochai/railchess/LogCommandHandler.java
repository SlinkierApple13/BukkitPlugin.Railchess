package me.momochai.railchess;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class LogCommandHandler implements CommandExecutor {

    Railchess plugin;

    LogCommandHandler(Railchess p) {
        plugin = p;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args[0].equals("list")) {
            ArrayList<Game1Logger> logs = new ArrayList<>();
            plugin.logList.forEach((id, log) -> logs.add(log));
            logs.sort(null);
            Railchess.sendMessage(sender, "已记录 " + logs.size() + " 次对局:");
            for (Game1Logger l: logs)
                sender.sendMessage(l.brief(plugin));
            return true;
        }
        return false;
    }

}
