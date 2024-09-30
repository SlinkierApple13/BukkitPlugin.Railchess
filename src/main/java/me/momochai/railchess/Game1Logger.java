package me.momochai.railchess;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

public class Game1Logger implements Comparable {

    ZonedDateTime time;
    long logId;
    long mapId;
    // ArrayList<String> playerName = new ArrayList<>();
    ArrayList<Integer> playerColour = new ArrayList<>();
    boolean valid = false;

    public String brief(@NotNull Railchess plugin) {
        if (!plugin.railmap.containsKey(mapId)) return null;
        return ("对局 " + logId + ": 地图 " + plugin.railmap.get(mapId).name + ", 共" + (totalMoves - 1) + " 手, 开始时间 "
                + time.format(DateTimeFormatter.ISO_ZONED_DATE_TIME) + ".");
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (Objects.equals(this, o)) return 0;
        assert o instanceof Game1Logger;
        Game1Logger l = (Game1Logger) o;
        return (l.logId < this.logId) ? -1 : 1;
    }

    public class Move {

        Map<Integer, Integer> occupier = new HashMap<>(); // -1 for unoccupied, -2 for dead
        ArrayList<Integer> positions = new ArrayList<>();
        ArrayList<Boolean> deads = new ArrayList<>(); // 0: normal, 1: dead
        int current = 0; // currentPlayer
        ArrayList<String> buffer = new ArrayList<>(); // text to display

        public String str() {
            StringBuffer buf = new StringBuffer();
            buf.append(occupier.size()).append("\n");
            occupier.forEach((id, occ) -> buf.append(id).append(" ").append(occ).append("\n"));
            // buf.append(positions.size()).append("\n");
            positions.forEach(a -> buf.append(a).append(" "));
            buf.append("\n");
            deads.forEach(a -> buf.append(a).append(" "));
            buf.append("\n").append(current).append("\n").append(buffer.size()).append("\n");
            buffer.forEach(str -> buf.append(str.replaceAll(String.valueOf(ChatColor.COLOR_CHAR), "&")).append("\n"));
            return buf.toString();
        }

        public Move subtract(final Move from) {
            ArrayList<Integer> toBeRemoved = new ArrayList<>();
            occupier.forEach((id, occ) -> {
                if (from.occupier.containsKey(id) && from.occupier.get(id).equals(occ))
                    toBeRemoved.add(id);
            });
            toBeRemoved.forEach(id -> occupier.remove(id));
            return this;
        }

        public Move add(final @NotNull Move from) {
            from.occupier.forEach((id, occ) -> {
               if (!occupier.containsKey(id))
                   occupier.put(id, occ);
            });
            return this;
        }

    }

    ArrayList<Move> moves = new ArrayList<>();
    int totalMoves = 0;

    public boolean save(File file) {
        if (totalMoves < 1) return false;
        try {
            file.createNewFile();
            PrintWriter writer = new PrintWriter(file);
            writer.println(0);
            writer.println(logId);
            writer.println(mapId);
            writer.println(time.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            int n = playerColour.size();
            writer.println(n);
            for (int i = 0; i < n; ++i)
                writer.println(playerColour.get(i));
            writer.println(totalMoves);
            for (int i = totalMoves - 1; i >= 1; --i)
                moves.get(i).subtract(moves.get(i - 1));
            for (Move move: moves)
                writer.print(move.str());
            writer.close();
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, e.getMessage());
            return false;
        }
    }

    public boolean load(File file) {
        try {
            Scanner scanner = new Scanner(file);
            int formatVersion = scanner.nextInt();
            if (formatVersion != 0) {
                scanner.close();
                return false;
            }
            logId = scanner.nextLong();
            // Bukkit.getLogger().log(Level.INFO, "logId = " + logId);
            mapId = scanner.nextLong();
            // Bukkit.getLogger().log(Level.INFO, "mapId = " + mapId);
            scanner.nextLine();
            time = ZonedDateTime.parse(scanner.nextLine(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
            // Bukkit.getLogger().log(Level.INFO, "Log time loaded successfully: " + time.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            int playerCount = scanner.nextInt();
            for (int i = 0; i < playerCount; ++i)
                playerColour.add(scanner.nextInt());
            totalMoves = scanner.nextInt();
            for (int i = 0; i < totalMoves; ++i) {
                // Bukkit.getLogger().log(Level.INFO, "Loading move " + i);
                Move move = new Move();
                int stationCount = scanner.nextInt();
                for (int j = 0; j < stationCount; ++j) {
                    int id = scanner.nextInt();
                    int occ = scanner.nextInt();
                    move.occupier.put(id, occ);
                }
                // int playerCount = scanner.nextInt();
                for (int j = 0; j < playerCount; ++j)
                    move.positions.add(scanner.nextInt());
                for (int j = 0; j < playerCount; ++j)
                    move.deads.add(scanner.nextBoolean());
                move.current = scanner.nextInt();
                int bufferCount = scanner.nextInt();
                scanner.nextLine();
                for (int j = 0; j < bufferCount; ++j)
                    move.buffer.add(scanner.nextLine().replaceAll("&", String.valueOf(ChatColor.COLOR_CHAR)));
                if (i > 0)
                    move.add(moves.get(i - 1));
                moves.add(move);
            }
            Bukkit.getLogger().log(Level.INFO, "Successfully loaded " + logId + ".game1");
            scanner.close();
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, e.getMessage());
            return false;
        }
    }

    public void logMessage(String str) {
        if (totalMoves == 0) return;
        moves.get(totalMoves - 1).buffer.add(str);
    }

    public void advance(final Game1 game, boolean isFinal) {
        if (totalMoves > 0 && game != null) {
            Move move = moves.get(totalMoves - 1);
            move.current = game.currentPlayer;
            game.stationList.forEach((id, sta) -> {
                if (sta.occupied) move.occupier.put(id.intValue(), sta.occupiedBy);
                else if (sta.dead) move.occupier.put(id.intValue(), -2);
                else move.occupier.put(id.intValue(), -1);
            });
            for (Game1.PlayerWrapper plw: game.playerList) {
                // Bukkit.getLogger().log(Level.INFO, "Saving move " + totalMoves + ": " + plw.displayName);
                move.positions.add(plw.position);
                move.deads.add(plw.dead);
            }
        }
        if (isFinal) return;
        moves.add(new Move());
        ++totalMoves;
    }

    @Contract(pure = true)
    public final @Nullable Move getMove(int id) {
        if (id >= totalMoves || id < 0) return null;
        return moves.get(id);
    }

    Game1Logger(long mId) {
        time = ZonedDateTime.now();
        logId = System.currentTimeMillis();
        mapId = mId;
        valid = true;
        advance(null, false);
    }

    Game1Logger(File file) {
        valid = load(file);
    }

}
