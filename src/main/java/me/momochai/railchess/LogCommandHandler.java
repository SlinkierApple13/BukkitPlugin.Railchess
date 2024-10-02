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
        try {
            if (args[0].equals("all")) {
                ArrayList<Game1Logger> logs = new ArrayList<>();
                plugin.logList.forEach((id, log) -> logs.add(log));
                logs.sort(null);
                Railchess.sendMessage(sender, "已记录 " + logs.size() + " 次对局:");
                for (Game1Logger l : logs)
                    sender.sendMessage(l.brief(plugin));
                sender.sendMessage("使用 /rclog list [数量] 查看最近几次对局.");
                return true;
            }
        } catch (Exception ignored) {}
        try {
            if (args[0].equals("list")) {
                int n;
                try {
                    n = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    n = 5;
                }
                if (n <= 0) n = 5;
                ArrayList<Game1Logger> logs = new ArrayList<>();
                plugin.logList.forEach((id, log) -> logs.add(log));
                logs.sort(null);
                n = Math.min(n, logs.size());
                Railchess.sendMessage(sender, "已记录 " + logs.size() + " 次对局. 最近 " + n + " 次对局:");
                for (int i = 0; i < n; ++i)
                    sender.sendMessage(logs.get(i).brief(plugin));
                sender.sendMessage("使用 /rclog all 查看全部对局.");
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

}
