package me.momochai.railchess;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Game1Logger {

    ZonedDateTime time;
    long logId;
    long mapId;
    ArrayList<String> playerName = new ArrayList<>();
    boolean valid = false;

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
            buf.append(positions.size()).append("\n");
            positions.forEach(a -> buf.append(a).append(" "));
            buf.append("\n");
            deads.forEach(a -> buf.append(a).append(" "));
            buf.append("\n").append(current).append("\n").append(buffer.size());
            buffer.forEach(str -> buf.append(str).append("\n"));
            return buf.toString();
        }

    }

    ArrayList<Move> moves = new ArrayList<>();
    int totalMoves = 0;

    public boolean save(File file) {
        try {
            file.createNewFile();
            PrintWriter writer = new PrintWriter(file);
            writer.println(0);
            writer.println(logId);
            writer.println(mapId);
            writer.println(time.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            writer.println(totalMoves);
            for (Move move: moves)
                writer.print(move.str());
            writer.close();
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public boolean load(File file) {
        try {
            Scanner scanner = new Scanner(file);
            int formatVersion = scanner.nextInt();
            if (formatVersion != 0) {
                scanner.close();
                return true;
            }
            logId = scanner.nextInt();
            mapId = scanner.nextInt();
            scanner.nextLine();
            time = ZonedDateTime.parse(scanner.nextLine(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
            totalMoves = scanner.nextInt();
            for (int i = 0; i < totalMoves; ++i) {
                Move move = new Move();
                int stationCount = scanner.nextInt();
                for (int j = 0; j < stationCount; ++j) {
                    int id = scanner.nextInt();
                    int occ = scanner.nextInt();
                    move.occupier.put(id, occ);
                }
                int playerCount = scanner.nextInt();
                for (int j = 0; j < playerCount; ++j)
                    move.positions.add(scanner.nextInt());
                for (int j = 0; j < playerCount; ++j)
                    move.deads.add(scanner.nextBoolean());
                int bufferCount = scanner.nextInt();
                scanner.nextLine();
                for (int j = 0; j < bufferCount; ++j)
                    move.buffer.add(scanner.nextLine());
            }
            System.out.println("Successfully loaded " + logId + ".game1");
            scanner.close();
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public void logMessage(String str) {
        if (totalMoves == 0) return;
        moves.get(totalMoves - 1).buffer.add(str);
    }

    public void advance(final Game1 game) {
        if (totalMoves > 0 && game != null) {
            Move move = moves.get(totalMoves - 1);
            move.current = game.currentPlayer;
            game.stationList.forEach((id, sta) -> {
                if (sta.occupied) move.occupier.put(id.intValue(), sta.occupiedBy);
                else if (sta.dead) move.occupier.put(id.intValue(), -2);
                else move.occupier.put(id.intValue(), -1);
            });
            for (Game1.PlayerWrapper plw : game.playerList) {
                move.positions.add(plw.position);
                move.deads.add(plw.dead);
            }
        }
        moves.add(new Move());
        ++totalMoves;
    }

    Game1Logger(final @NotNull List<Player> players, long mId) {
        time = ZonedDateTime.now();
        logId = System.currentTimeMillis();
        mapId = mId;
        for (Player p: players)
            playerName.add(p.getName());
        valid = true;
        advance(null);
    }

    Game1Logger(File file) {
        valid = load(file);
    }

}
