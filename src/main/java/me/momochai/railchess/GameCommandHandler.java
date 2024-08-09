package me.momochai.railchess;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class GameCommandHandler implements CommandExecutor {

    Railchess plugin;
    GameCommandHandler(Railchess p) {
        plugin = p;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            Player player = sender.getServer().getPlayer(sender.getName());
            if (args[0] == null || player == null || !player.isValid()) return false;
            String name = player.getName();
            if (!player.hasPermission("railchess.subscribe"))
                return false;
            if (Objects.equals(args[0], "spectate")) {
                if (plugin.playerInGame.containsKey(name) || plugin.playerInEditor.containsKey(name) ||
                    plugin.playerInStand.containsKey(name))
                    return false;
                for (RailchessStand st: plugin.stand)
                    if (st.isNearBy(player)) {
                        if (st.game != null && st.game.available)
                            st.game.subscribe(player);
                        break;
                    }
            } else if (Objects.equals(args[0], "despectate")) {
                if (plugin.playerSubGame.containsKey(name) && !plugin.playerInGame.containsKey(name))
                    plugin.playerSubGame.get(name).desubscribe(player);
            } else if (Objects.equals(args[0], "quit")) {
                if (plugin.playerInGame.containsKey(name))
                    plugin.playerInGame.get(name).getPlayerWrapper(player).quit(false, "");
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
