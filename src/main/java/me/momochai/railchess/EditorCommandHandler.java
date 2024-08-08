package me.momochai.railchess;

import org.apache.commons.lang3.tuple.MutablePair;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

public class EditorCommandHandler implements CommandExecutor {

    Railchess plugin;

    EditorCommandHandler(Railchess p) {
        plugin = p;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            Player player = sender.getServer().getPlayer(sender.getName());
            if (player == null) return false;
            if (!player.hasPermission("railchess.edit")) return false;
            if (!plugin.playerInEditor.containsKey(sender.getName())) {
                if (!args[0].equals("join"))
                    return false;
                if (plugin.playerInGame.containsKey(player.getName()) || plugin.playerInStand.containsKey(player.getName()))
                    return false;
                if (plugin.playerSubGame.containsKey(player.getName()))
                    plugin.playerSubGame.get(player.getName()).desubscribe(player);
                for (RailchessStand stand: plugin.stand)
                    if (stand.isNearBy(player) && stand.editor != null) {
                        stand.editor.broadcastMessage(player.getName() + " has joined");
                        player.sendMessage("Successfully joined editor " + stand.editor.name + ".railmap");
                        stand.editor.editingPlayer.add(player);
                        plugin.playerInEditor.put(player.getName(), stand.editor);
                        break;
                    }
                return true;
            }
            if (!plugin.playerInEditor.containsKey(sender.getName()))
                return false;
            MapEditor editor = plugin.playerInEditor.get(sender.getName());
            if (args[0].equals("connect")) {
                int value = Integer.parseInt(args[1]);
                if (value == 1) editor.singleConnect();
                if (value == 2) editor.biConnect();
                if (value == 0) {
                    editor.disconnect(editor.currentStation, (l, s) -> (s == editor.previousStation));
                    editor.disconnect(editor.previousStation, (l, s) -> (s == editor.currentStation));
                }
            } else if (args[0].equals("flush")) {
                editor.autoSet();
            } else if (args[0].equals("add")) {
                if (args[1].equals("spawnRepellence") || args[1].equals("nospawn")) {
                    if (!editor.stationList.containsKey(editor.previousStation) ||
                            !editor.stationList.containsKey(editor.currentStation))
                        return false;
                    editor.spawnRepellence.add(MutablePair.of(editor.previousStation, editor.currentStation));
                    editor.broadcastMessage("Successfully added spawn repellence " + editor.previousStation +
                            " <-> " + editor.currentStation);
                }
                if (args[1].equals("transferRepellence") || args[1].equals("notransfer")) {
                    int l1 = Integer.parseInt(args[2]);
                    int l2 = Integer.parseInt(args[3]);
                    if (editor.transferRepellence.contains(MutablePair.of(l1, l2)))
                        return false;
                    editor.transferRepellence.add(MutablePair.of(l1, l2));
                    editor.broadcastMessage("Successfully added transfer repellence " + l1 +
                            " <-> " + l2);
                }
                if (args[1].equals("trainForbid") || args[1].equals("notrain")) {
                    int from = Integer.parseInt(args[2]);
                    int via = Integer.parseInt(args[3]);
                    int to = Integer.parseInt(args[4]);
                    int line = Integer.parseInt(args[5]);
                    editor.addTrainForbid(via, from, to, line);
                }
            } else if (args[0].equals("line") || args[0].equals("selectline")) {
                editor.selectLine(Integer.parseInt(args[1]));
            } else if (args[0].equals("remove")) {
                if (args[1].equals("spawnRepellence") || args[1].equals("nospawn")) {
                    editor.spawnRepellence.remove(MutablePair.of(editor.previousStation, editor.currentStation));
                    editor.broadcastMessage("Successfully removed spawn repellence " + editor.previousStation +
                            " <-> " + editor.currentStation);
                }
                if (args[1].equals("transferRepellence") || args[1].equals("notransfer")) {
                    int l1 = Integer.parseInt(args[2]);
                    int l2 = Integer.parseInt(args[3]);
                    if (!editor.transferRepellence.contains(MutablePair.of(l1, l2)))
                        return false;
                    editor.transferRepellence.remove(MutablePair.of(l1, l2));
                    editor.broadcastMessage("Successfully removed transfer repellence " + l1 +
                            " <-> " + l2);
                }
                if (args[1].equals("trainForbid") || args[1].equals("notrain")) {
                    int from = Integer.parseInt(args[2]);
                    int via = Integer.parseInt(args[3]);
                    int to = Integer.parseInt(args[4]);
                    int line = Integer.parseInt(args[5]);
                    editor.removeTrainForbid(via, from, to, line);
                }
            } else if (args[0].equals("save") && args[1] == null) {
                plugin.railmap.put(editor.name, editor.toRailmap(true)).save(new File(plugin.mapFolder, editor.name + ".railmap"));
            } else if (args[0].equals("save")) {
                plugin.railmap.put(args[1], editor.toRailmap(true, args[1])).save(new File(plugin.mapFolder, args[1] + ".railmap"));
            } else if (args[0].equals("leave")) {
                editor.editingPlayer.remove(player);
                plugin.playerInEditor.remove(player.getName());
                editor.broadcastMessage(player.getName() + " has left");
                player.sendMessage("Successfully left editor");
            } else if (args[0].equals("close")) {
                editor.broadcastMessage("Editor closed");
                editor.close();
            } else {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

}
