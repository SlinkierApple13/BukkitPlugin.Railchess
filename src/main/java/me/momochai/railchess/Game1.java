package me.momochai.railchess;

import org.apache.commons.lang3.tuple.MutablePair;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Game1 {

    int n;
    int remainingPlayers;
    ArrayList<Player> p = new ArrayList<>();
    ArrayList<MutablePair<String, MutablePair<ItemStack, ItemStack>>> tileList = new ArrayList<>();
    ArrayList<Integer> spawn = new ArrayList<>();
    Location location;
    Vector hDir = new Vector();
    Random random = new Random();
    List<Player> subscriber = new ArrayList<>();
    double sizeH; // horizontal size
    double sizeV; // vertical size
    List<MutablePair<Integer, Integer>> transferRepellence;
    List<MutablePair<Integer, Integer>> spawnRepellence;
    int currentPlayer;
    int maxStep;
    int maxHurt;
    boolean available = true;

    public Player getCurrentPlayer() {
        return playerList.get(currentPlayer).player;
    }

    public PlayerWrapper getCurrent() {
        return playerList.get(currentPlayer);
    }

    public void end() {
        available = false;
        broadcast("Final Result: ");
        for (PlayerWrapper pl: playerList)
            broadcast(pl.player.getName() + " -- " + pl.score);
        close();
    }

    public void advance() {
        if (remainingPlayers <= 1) end();
        if (getCurrent().step != 0) getCurrent().getNextStep();
        do {
            ++currentPlayer;
            if (currentPlayer == n)
                currentPlayer = 0;
        } while (getCurrent().dead);
        if (getCurrent().step == 0) getCurrent().getStep();
        getCurrent().broadcastStep();
        if (choices(currentPlayer, getCurrent().step, 1, true).isEmpty()) {
            broadcast(getCurrentPlayer().getName() + " gets stuck");
            ++getCurrent().hurt;
            if (getCurrent().hurt == maxHurt)
                getCurrent().quit(true, "gets stuck too many times");
            advance();
        }
    }

    public void broadcast(String s) {
        for (Player pl: subscriber)
            if (pl.isValid())
                pl.sendMessage(s);
    }

    public class PlayerWrapper {

        Player player;
        int score;
        int maxScore;
        ItemStack tile;
        ItemStack tile2;
        int position; // # of the station the player is at in map
        int step = 0;
        boolean dead;
        int hurt;

        public void getNextStep() {
            step = random.nextInt(maxStep) + 1;
            player.sendMessage("Your next move should involve " + step + (step == 1 ? " step" : " steps"));
        }

        public void getStep() {
            step = random.nextInt(maxStep) + 1;
        }

        public void broadcastStep() {
            broadcast(player.getName() + "'s turn: " + step + (step == 1 ? " step" : " steps"));
        }

        public void quit(boolean hasReason, String reason) {
            if (!hasReason)
                broadcast(player.getName() + " quits");
            else
                broadcast(player.getName() + " quits: " + reason);
            dead = true;
            plugin.playerInGame.remove(player.getName());
            if (getCurrent().equals(this))
                advance();
            --remainingPlayers;
            if (remainingPlayers <= 1)
                end();
        }

        PlayerWrapper(Player pl, @NotNull MutablePair<ItemStack, ItemStack> ti, int pos) {
            position = pos;
            player = pl;
            tile = ti.getLeft();
            tile2 = ti.getRight();
            score = 0;
            maxScore = 0;
            step = 0;
            dead = false;
            hurt = 0;
        }

    }

    public class StationWrapper {

        Station station;
        boolean occupied;
        boolean dead;
        int occupiedBy;
        ItemDisplay entity;
        int reachableBy; // sum of (2^(i)) for all reachable player i
        public static final ItemStack CHOICE = new ItemStack(Material.GRAY_STAINED_GLASS);
        public static final ItemStack CHOICE_OCCUPIED = new ItemStack(Material.GRAY_CONCRETE);

        public void update() {
            if (dead || occupied)
                return;
            if (reachableBy == 1 || reachableBy == 2 || reachableBy == 4 ||
                reachableBy == 8 || reachableBy == 16 || reachableBy == 32) {
                occupied = true;
                occupiedBy = 0;
                for (int i = 1; i <= 16; i *= 2)
                    if (reachableBy > i) ++occupiedBy;
                playerList.get(occupiedBy).score += station.value;
                autoMark();
            }
            if (reachableBy == 0) {
                dead = true;
                autoMark();
            }
        }

        public boolean isReachable(int pl) {
            return ((reachableBy & (1 << pl)) != 0);
        }

        public void setReachable(int pl) {
            reachableBy |= (1 << pl);
        }

        public void autoMark() {
            if (!occupied) {
                mark(new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS));
                return;
            }
            if (dead) {
                mark(new ItemStack(Material.GRAY_STAINED_GLASS));
                return;
            }
            mark(playerList.get(occupiedBy).tile);
        }

        public void mark(ItemStack item) {
            if (entity == null || !entity.isValid()) {
                entity = (ItemDisplay) location.getWorld().spawnEntity(getLocation(), EntityType.ITEM_DISPLAY);
                entity.setBrightness(new Display.Brightness(15, 15));
                entity.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f),
                        new Quaternionf(), new Vector3f(0.15f, 0.15f, 0.15f), new Quaternionf()));
            }
            entity.setItemStack(item);
            entity.setInvulnerable(true);
        }

        public void close() {
            entity.remove();
        }

        public Location getLocation() {
            World world = location.getWorld();
            broadcast("Location = (" + location.getX() + ", " + location.getY() + ", " + location.getZ() + ")");
            Vector coord = new Vector(location.getX(), location.getY(), location.getZ());
            coord.add(hDir.clone().normalize().multiply(station.normPos.getLeft() * sizeH));
            coord.add(new Vector(0.0, sizeV, 0.0).multiply(station.normPos.getRight()));
            broadcast("NewLocation = (" + coord.getX() + ", " + coord.getY() + ", " + coord.getZ() + ")");
            return new Location(world, coord.getX(), coord.getY(), coord.getZ());
        }

        StationWrapper(Station sta) {
            int j = 1;
            reachableBy = 0;
            for (int i = 0; i < n; ++i) {
                reachableBy += j;
                j *= 2;
            }
            occupiedBy = -1;
            occupied = false;
            station = sta;
            dead = false;
            autoMark();
        }

    }

    ArrayList<PlayerWrapper> playerList = new ArrayList<>();
    Map<Integer, StationWrapper> stationList = new HashMap<>();

    public void close() {
        for (StationWrapper stw: stationList.values())
            stw.close();
        subscriber.clear();
        for (PlayerWrapper plw: playerList)
            plw.quit(false, "");
        stand.game = null;
    }

    public PlayerWrapper getPlayerWrapper(Player pl) {
        for (PlayerWrapper plw: playerList) {
            if (plw.player.equals(pl))
                return plw;
        }
        return null;
    }

    public Vector normal() {
        return hDir.getCrossProduct(new Vector(0.0, 1.0, 0.0)).normalize();
    }

    public MutablePair<Boolean, MutablePair<Double, Double>> getSight(@NotNull Player pl) {
        if (!pl.equals(getCurrentPlayer()))
            return MutablePair.of(false, new MutablePair<>());
        Location eye = pl.getEyeLocation();
        if (!eye.getWorld().equals(location.getWorld()))
            return MutablePair.of(false, new MutablePair<>());
        Vector eyeOffset = eye.toVector().subtract(location.toVector());
        Vector sight = eye.getDirection();
        Vector normal = normal();
        if (sight.dot(normal) >= -0.1)
            return MutablePair.of(false, new MutablePair<>());
        Vector pointing = sight.multiply(-(eyeOffset.dot(normal)) / (sight.dot(normal)));
        Vector onMap = pointing.add(eyeOffset);
        double normH = onMap.dot(hDir) / sizeH;
        double normV = onMap.dot(new Vector(0.0, 1.0, 0.0)) / sizeV;
        boolean valid = (0.0 <= normH && normH <= 1.0 && 0.0 <= normV && normV <= 1.0);
        return MutablePair.of(valid, MutablePair.of(normH, normV));
    }

    public int getStation(Player pl) {
        MutablePair<Boolean, MutablePair<Double, Double>> sight = getSight(pl);
        if (!sight.getLeft()) return -1;
        double x = sight.getRight().getLeft();
        double y = sight.getRight().getRight();
        AtomicReference<Double> minDist2 = new AtomicReference<>(sizeH * sizeH + sizeV * sizeV);
        AtomicInteger sta = new AtomicInteger(-1);
        stationList.forEach((Integer id, StationWrapper stw) -> {
            if (dist2(x, y, stw.station.normPos) < minDist2.get()) {
                minDist2.set(dist2(x, y, stw.station.normPos));
                sta.set(id);
            }
        });
        return sta.get();
    }

    public boolean play(Player pl) {
        if (!available) return false;
        int station = getStation(pl);
        if (station == -1) return false;
        return move(currentPlayer, getCurrent().step, 1, station);
    }

    public double dist2(double a, double b, @NotNull MutablePair<Double, Double> nPos) {
        double c = nPos.getLeft();
        double d = nPos.getRight();
        return (a - c) * (a - c) * sizeH * sizeH + (b - d) * (b - d) * sizeV * sizeV;
    }

    private class Task {
        int player;
        int station;

        Task(int pl, int st) {
            player = pl;
            station = st;
        }

    }

/*  private class Task1 {
        int station;
        int line;
        int interchanges;
        int previous;
        int remainingSteps;

        Task1(int st, int l, int i, int prev, int rem) {
            station = st;
            line = l;
            interchanges = i;
            previous = prev;
            remainingSteps = rem;
        }

    }*/

    public void choices0(int pl, int pos, int prev, int steps, int line, int interchanges, List<Integer> res, List<MutablePair<Integer, Integer>> visited) {
        if (steps == 0) {
            res.add(pos);
            return;
        }
        if (!stationList.containsKey(pos))
            return;
        for (MutablePair<Integer, Integer> nb: stationList.get(pos).station.neighbour) {
            if (!stationList.containsKey(nb.getRight()))
                continue;
            if (stationList.get(nb.getRight()).occupied && stationList.get(nb.getRight()).occupiedBy != pl)
                continue;
            if (nb.getRight() == prev)
                continue;
            if (visited.contains(MutablePair.of(pos, nb.getRight())) || visited.contains(MutablePair.of(nb.getRight(), pos)))
                continue;
            int itc = interchanges - (((line == nb.getLeft() || line == -1) &&
                    !stationList.get(pos).station.forbid.contains(new ForbidTrain(
                            prev, line, nb.getRight()
                    ))) ? 0 : 1);
            if (itc < 0) continue;
            visited.add(MutablePair.of(pos, nb.getRight()));
            choices0(pl, nb.getRight(), pos, steps - 1, nb.getLeft(), itc, res, visited);
            visited.removeIf(p -> Objects.equals(p, MutablePair.of(pos, nb.getRight())));
        }
    }

    public List<Integer> choices(int pl, int steps, int interchanges, boolean showAvail) {
        List<Integer> res = new ArrayList<>();
        choices0(pl, playerList.get(pl).position, -1, steps, -1, interchanges, res, new ArrayList<>());
        if (showAvail) res.forEach(i -> {
            try {
            stationList.get(i).mark(stationList.get(i).occupied ? StationWrapper.CHOICE_OCCUPIED : StationWrapper.CHOICE);
            } catch (Exception e) {}
        });
        return res;
    }

/*  public List<Integer> choices1(int pl, int steps, int interchanges) {
        List<Integer> res = new ArrayList<>();
        Queue<Task1> taskQueue = new LinkedList<>();
        taskQueue.add(new Task1(playerList.get(pl).position, -1, interchanges, -1, steps));
        while (!taskQueue.isEmpty()) {
            Task1 fr = taskQueue.poll();
            if (fr.remainingSteps == 0) {
                res.add(fr.station);
                continue;
            }
            for (MutablePair<Integer, Integer> nb: stationList.get(fr.station).station.neighbour) {
                if (fr.previous != -1 && nb.getRight() == fr.previous) continue;
                if (stationList.get(nb.getRight()).occupied && stationList.get(nb.getRight()).occupiedBy != pl)
                    continue;
                if (fr.line == -1 || nb.getLeft() == fr.line) {
                    taskQueue.add(new Task1(nb.getRight(), nb.getLeft(), fr.interchanges,
                            fr.station, fr.remainingSteps - 1));
                } else if (fr.interchanges > 0 && canTransfer(fr.line, nb.getLeft())) {
                    taskQueue.add(new Task1(nb.getRight(), nb.getLeft(), fr.interchanges - 1,
                            fr.station, fr.remainingSteps - 1));
                }
            }
        }
        return res;
    }*/

    public void update() {
        // broadcast("Updating...");
        Queue<Task> taskQueue = new LinkedList<>();
        for (int i = 0; i < n; ++i) {
            if (playerList.get(i).dead) continue;
            taskQueue.add(new Task(i, playerList.get(i).position));
            playerList.get(i).maxScore = stationList.get(playerList.get(i).position).station.value;
            stationList.get(playerList.get(i).position).reachableBy = (1 << i);
            stationList.get(playerList.get(i).position).update();
        }
        for (StationWrapper stw: stationList.values()) {
            if (stw.occupied)
                stw.reachableBy = 1 << stw.occupiedBy;
            else stw.reachableBy = 0;
        }
        try {
            while (!taskQueue.isEmpty()) {
                // StringBuilder buf = new StringBuilder();
                // for (Task t: taskQueue)
                //     buf.append("(" + t.player + ", " + t.station + ") ");
                // broadcast(buf.toString());
                Task fr = taskQueue.poll();
                for (MutablePair<Integer, Integer> i: stationList.get(fr.station).station.neighbour) {
                    if (!stationList.containsKey(i.getRight())) continue;
                    StationWrapper nb = stationList.get(i.getRight());
                    if (nb.isReachable(fr.player) || (nb.occupied && nb.occupiedBy != fr.player))
                        continue;
                    nb.setReachable(fr.player);
                    playerList.get(fr.player).maxScore += nb.station.value;
                    taskQueue.add(new Task(fr.player, i.getRight()));
                }
            }
        } catch (Exception e) {
            broadcast(e.getMessage());
        }
        // broadcast("Station claim updated successfully.");
        for (StationWrapper stw: stationList.values())
            stw.update();
        for (PlayerWrapper plw: playerList) {
            stationList.get(plw.position).mark(plw.tile2);
            if (plw.dead) continue;
            if (plw.maxScore == plw.score)
                plw.quit(true, "no more points to gain");
        }
        for (int i = 0; i < n; ++i) {
            broadcast(playerList.get(i).player.getName() + " (" + tileList.get(i).getLeft() +
                    ChatColor.COLOR_CHAR + "r): " + playerList.get(i).score + " / " + playerList.get(i).maxScore);
        }
    }

    Boolean canTransfer(int a, int b) {
        return !(transferRepellence.contains(MutablePair.of(a, b)) ||
                transferRepellence.contains(MutablePair.of(b, a)) ||
                (Math.abs(a - b) > 1000));
    }

    public void subscribe(Player pl) {
        if (subscriber.contains(pl)) return;
        subscriber.add(pl);
        pl.sendMessage("Spectating game");
        if (plugin.playerSubGame.containsKey(pl.getName()) && plugin.playerSubGame.get(pl.getName()) != null)
            plugin.playerSubGame.get(pl.getName()).desubscribe(pl);
        plugin.playerSubGame.put(pl.getName(), this);
    }

    public void desubscribe(@NotNull Player pl) {
        pl.sendMessage("Leaving game");
        subscriber.remove(pl);
        plugin.playerSubGame.remove(pl.getName());
    }

    public boolean move(int pl, int steps, int interchanges, int destination) {
        if (pl != currentPlayer)
            return false;
        if (!choices(pl, steps, interchanges, true).contains(destination))
            return false;
        playerList.get(pl).position = destination;
        update();
        advance();
        return true;
    }

    MutablePair<ItemStack, ItemStack> displayTiles(int colour) {
        return switch (colour) {
            case 1 ->
                    MutablePair.of(new ItemStack(Material.ORANGE_STAINED_GLASS), new ItemStack(Material.ORANGE_CONCRETE));
            case 2 ->
                    MutablePair.of(new ItemStack(Material.PINK_STAINED_GLASS), new ItemStack(Material.PINK_CONCRETE));
            case 3 ->
                    MutablePair.of(new ItemStack(Material.LIME_STAINED_GLASS), new ItemStack(Material.ORANGE_CONCRETE));
            case 4 ->
                    MutablePair.of(new ItemStack(Material.YELLOW_STAINED_GLASS), new ItemStack(Material.YELLOW_CONCRETE));
            case 5 ->
                    MutablePair.of(new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS), new ItemStack(Material.LIGHT_BLUE_CONCRETE));
            default ->
                    MutablePair.of(new ItemStack(Material.BLACK_STAINED_GLASS), new ItemStack(Material.BLACK_CONCRETE));
        };
    }

    Railchess plugin;
    RailchessStand stand;

    public Location mid() {
        Location res = location.clone();
        Vector vec = hDir.clone();
        return res.add(vec.multiply(sizeH * 0.5));
    }

    public boolean isNearBy(Player pl) {
        return mid().getNearbyLivingEntities(RailchessStand.RANGE).contains(pl);
    }

    Game1(@NotNull Railchess pp, @NotNull RailchessStand st, @NotNull Railmap playMap, @NotNull List<Player> players, Location loc, double sH, double sV, int mStep, Vector hd) {
        stand = st;
        plugin = pp;
        sizeH = sH;
        sizeV = sV;
        hDir = hd;
        maxStep = (mStep > 0) ? mStep : 12;
        location = loc;
        currentPlayer = 0;
        spawnRepellence = playMap.spawnRepellence;
        transferRepellence = playMap.transferRepellence;
        for (Player pl: players) {
            if (!subscriber.contains(pl)) subscriber.add(pl);
            plugin.playerSubGame.put(pl.getName(), this);
            if (pl.hasPermission("railchess.play")) {
                p.add(pl);
                plugin.playerInGame.put(pl.getName(), this);
            }
        }
        assert p.size() <= 5;
        n = p.size();
        remainingPlayers = p.size();
        Collections.shuffle(p);
        tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "6Orange", displayTiles(1)));
        tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "dPink", displayTiles(2)));
        tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "aLime", displayTiles(3)));
        tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "eYellow", displayTiles(4)));
        tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "bLight Blue", displayTiles(5)));
        Collections.shuffle(tileList);
        spawn.addAll(playMap.spawn);
        playMap.station.forEach((Integer id, Station sta) -> {
//          broadcast("Loading station " + id);
            stationList.put(id, new StationWrapper(sta));
//          broadcast("Loaded station " + id);
        });
        for (int t = 0; t < 16; ++t) { // try to spawn at most 16 times to avoid spawn repellence
            Collections.shuffle(spawn);
            boolean flag = false;
            for (int i = 1; i < n; ++i)
                for (int j = 0; j < i; ++j)
                    if (spawnRepellence.contains(MutablePair.of(spawn.get(i), spawn.get(j))) ||
                            spawnRepellence.contains(MutablePair.of(spawn.get(j), spawn.get(i)))) {
                        flag = true;
                        break;
                    }
            if (!flag)
                break;
        }
        for (int j = 0; j < n; ++j) {
            Player pl = p.get(j);
            broadcast("The colour for " + p.get(j).getName() + " is " + tileList.get(j).getLeft());
            playerList.add(new PlayerWrapper(pl, tileList.get(j).getRight(), spawn.get(j)));
        }
        update();
        currentPlayer = n - 1;
        advance();
    }

}
