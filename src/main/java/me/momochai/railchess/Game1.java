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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
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
    Vector hDir;
    Random random = new Random();
    List<Player> subscriber = new ArrayList<>();
    double sizeH; // horizontal size
    double sizeV; // vertical size
    List<MutablePair<Integer, Integer>> transferRepellence;
    List<MutablePair<Integer, Integer>> spawnRepellence;
    List<Integer> currentChoices;
    int currentPlayer;
    int maxStep;
    int maxHurt;
    boolean available = true;
    boolean showChoices = false;
    boolean log = false;
    public static final double BROADCAST_RANGE = 15.0d;
    Game1Logger logger;

    public class SaveLog extends BukkitRunnable {

        @Override
        public void run() {
            logger.save(new File(plugin.logFolder, logger.logId + ".game1"));
        }

        SaveLog() {
            runTaskAsynchronously(plugin);
        }

    }

    public Player getCurrentPlayer() {
        return playerList.get(currentPlayer).player;
    }

    public PlayerWrapper getCurrent() {
        return playerList.get(currentPlayer);
    }

    public void end() {
        if (!available) return;
        available = false;
        broadcast("Final Result: ");
        for (PlayerWrapper pl: playerList)
            broadcast(pl.displayName + " -- " + pl.score);
        close();
    }

    public void advance() {
        if (!available) return;
        logger.advance(this);
        if (remainingPlayers <= 1) end();
        if (getCurrent().step != 0 && !getCurrent().dead) getCurrent().getNextStep();
        do {
            ++currentPlayer;
            if (currentPlayer == n)
                currentPlayer = 0;
        } while (getCurrent().dead);
        if (getCurrent().step == 0) getCurrent().getStep();
        getCurrent().broadcastStep();
        currentChoices = choices(currentPlayer, getCurrent().step, 1, showChoices);
        if (currentChoices.isEmpty()) {
            ++getCurrent().hurt;
            broadcast(getCurrent().displayName + " gets stuck (" + getCurrent().hurt + ")");
            if (getCurrent().hurt == maxHurt)
                getCurrent().quit(true, "gets stuck too many times", true);
            else advance();
        }
    }

    public void broadcast(String s) {
        for (Player pl: subscriber)
            if (pl.isValid())
                pl.sendMessage(s);
        logger.logMessage(s);
        if (!available) return;
        for (Player pl: stand.mid().getNearbyPlayers(BROADCAST_RANGE))
            if (!subscriber.contains(pl))
                pl.sendMessage(s);
    }

    public class PlayerWrapper {

        Player player;
        int score;
        int maxScore;
        ItemStack tile;
        ItemStack tile2;
        int position; // # of the station the player is at in map
        int step;
        boolean dead;
        int hurt;
        String displayName;

        public void getNextStep() {
            step = random.nextInt(maxStep) + 1;
            if (showChoices) return;
            player.sendMessage("Your next move should involve " + step + (step == 1 ? " step" : " steps"));
        }

        public void getStep() {
            step = random.nextInt(maxStep) + 1;
        }

        public void broadcastStep() {
            broadcast(displayName + "'s turn: " + step + (step == 1 ? " step" : " steps"));
        }

        public void quit(boolean hasReason, String reason, boolean triggerEnd) {
            if (!hasReason)
                broadcast(displayName + " left");
            else
                broadcast(displayName + " left: " + reason);
            dead = true;
            boolean alive = false;
            for (PlayerWrapper plw: playerList)
                alive |= (!plw.dead && plw.player.equals(player));
            if (!alive)
                plugin.playerInGame.remove(player.getName());
            if (!triggerEnd) {
                plugin.playerSubGame.remove(player.getName());
                subscriber.remove(player);
            }
            --remainingPlayers;
            if (remainingPlayers == 1) {
                for (PlayerWrapper plw: playerList)
                    if (!plw.dead)
                        plw.score = plw.maxScore;
            }
            if (remainingPlayers <= 1 && triggerEnd)
                end();
            else if (getCurrent().equals(this))
                advance();
        }

        PlayerWrapper(@NotNull Player pl, @NotNull MutablePair<ItemStack, ItemStack> ti, int pos, String prefix) {
            position = pos;
            player = pl;
            tile = ti.getLeft();
            tile2 = ti.getRight();
            score = 0;
            maxScore = 0;
            step = 0;
            dead = false;
            hurt = 0;
            displayName = prefix + pl.getName() + ChatColor.COLOR_CHAR + "r";
        }

    }

    public class StationWrapper {

        Station station;
        boolean occupied;
        boolean dead;
        int occupiedBy;
        ItemDisplay entity, entity2;
        int reachableBy; // sum of (2^(i)) for all reachable player i
        public static final ItemStack DEAD = new ItemStack(Material.GRAY_STAINED_GLASS);
        public static final ItemStack NORMAL = new ItemStack(Material.AIR);

        public void update() {
            if (dead)
                return;
            if (!occupied && (reachableBy == 1 || reachableBy == 2 || reachableBy == 4 ||
                    reachableBy == 8 || reachableBy == 16 || reachableBy == 32)) {
                occupied = true;
                occupiedBy = 0;
                for (int i = 1; i <= 16; i *= 2)
                    if (reachableBy > i) ++occupiedBy;
                playerList.get(occupiedBy).score += station.value;
                autoMark();
                return;
            }
            if (!occupied && reachableBy == 0)
                dead = true;
            autoMark();
        }

        public boolean isReachable(int pl) {
            return ((reachableBy & (1 << pl)) != 0);
        }

        public void setReachable(int pl) {
            reachableBy |= (1 << pl);
        }

        public void autoMark() {
            if (!occupied) {
                mark(new ItemStack(NORMAL));
                return;
            }
            if (dead) {
                mark(new ItemStack(DEAD));
                return;
            }
            mark(playerList.get(occupiedBy).tile);
        }

        public void mark(ItemStack item) {
            if (entity == null || !entity.isValid()) {
                entity = (ItemDisplay) location.getWorld().spawnEntity(getLocation(), EntityType.ITEM_DISPLAY);
                entity.setBrightness(new Display.Brightness(15, 0));
                entity.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f),
                        new Quaternionf(), new Vector3f(0.1f, 0.1f, 0.1f), new Quaternionf()));
            }
            entity.setItemStack(item);
            entity.setInvulnerable(true);
            if (entity2 == null || !entity2.isValid()) {
                entity2 = (ItemDisplay) location.getWorld().spawnEntity(getLocation(), EntityType.ITEM_DISPLAY);
                entity2.setBrightness(new Display.Brightness(15, 0));
                entity2.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f),
                        new Quaternionf(), new Vector3f(0.1f, 0.1f, 0.1f), new Quaternionf()));
            }
            entity2.setItemStack(item);
            entity2.setInvulnerable(true);
        }

        public void close() {
            entity.remove();
            entity2.remove();
        }

        public Location getLocation() {
            World world = location.getWorld();
            // broadcast("Location = (" + location.getX() + ", " + location.getY() + ", " + location.getZ() + ")");
            Vector coord = new Vector(location.getX(), location.getY(), location.getZ());
            coord.add(hDir.clone().normalize().multiply(station.normPos.getLeft() * sizeH));
            coord.add(new Vector(0.0, sizeV, 0.0).multiply(station.normPos.getRight()));
            // broadcast("NewLocation = (" + coord.getX() + ", " + coord.getY() + ", " + coord.getZ() + ")");
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
    public Material darkened(@NotNull Material mat) {
        if (mat.equals(Material.YELLOW_STAINED_GLASS))
            return Material.ORANGE_STAINED_GLASS;
        if (mat.equals(Material.YELLOW_CONCRETE))
            return Material.ORANGE_CONCRETE;
        if (mat.equals(Material.PINK_STAINED_GLASS))
            return Material.MAGENTA_STAINED_GLASS;
        if (mat.equals(Material.PINK_CONCRETE))
            return Material.MAGENTA_CONCRETE;
        if (mat.equals(Material.LIGHT_BLUE_STAINED_GLASS))
            return Material.BLUE_STAINED_GLASS;
        if (mat.equals(Material.LIGHT_BLUE_CONCRETE))
            return Material.BLUE_CONCRETE;
        if (mat.equals(Material.LIME_STAINED_GLASS))
            return Material.GREEN_STAINED_GLASS;
        if (mat.equals(Material.LIME_CONCRETE))
            return Material.GREEN_CONCRETE;
        if (mat.equals(Material.LIGHT_GRAY_STAINED_GLASS))
            return Material.GRAY_STAINED_GLASS;
        if (mat.equals(Material.LIGHT_GRAY_CONCRETE))
            return Material.GRAY_CONCRETE;
        if (mat.equals(Material.WHITE_STAINED_GLASS))
            return Material.LIGHT_GRAY_STAINED_GLASS;
        if (mat.equals(Material.WHITE_CONCRETE))
            return Material.LIGHT_GRAY_CONCRETE;
        if (mat.equals(Material.AIR))
            return Material.WHITE_STAINED_GLASS;
        return Material.BLACK_CONCRETE;
    }

    public void close() {
        for (StationWrapper stw: stationList.values())
            stw.close();
        subscriber.clear();
        new SaveLog();
        for (PlayerWrapper plw: playerList)
            plw.quit(false, "", false);
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
        if (!available || !pl.equals(getCurrentPlayer())) return false;
        int station = getStation(pl);
        if (station == -1) return false;
        return move(currentPlayer, station);
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
            if (!canTransfer(line, nb.getLeft()))
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
                Material mat;
                if (playerList.get(pl).position == i)
                    mat = darkened(tileList.get(pl).getRight().getRight().getType());
                else if (stationList.get(i).occupied)
                    mat = darkened(tileList.get(pl).getRight().getLeft().getType());
                else mat = darkened(StationWrapper.NORMAL.getType());
                stationList.get(i).mark(new ItemStack(mat));
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
            playerList.get(i).maxScore = 0;
            if (playerList.get(i).dead) continue;
            taskQueue.add(new Task(i, playerList.get(i).position));
            stationList.get(playerList.get(i).position).reachableBy = (1 << i);
            stationList.get(playerList.get(i).position).update();
        }
        for (StationWrapper stw: stationList.values()) {
             if (stw.occupied)
                 playerList.get(stw.occupiedBy).maxScore += stw.station.value;
            stw.reachableBy = 0;
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
                    if (!nb.occupied) playerList.get(fr.player).maxScore += nb.station.value;
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
                plw.quit(true, "no more points to gain", true);
            if (!available) return;
        }
        for (int i = 0; i < n; ++i) {
            broadcast(playerList.get(i).displayName + " -- " + playerList.get(i).score + " / " + playerList.get(i).maxScore);
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

    public boolean move(int pl, int destination) {
        if (pl != currentPlayer)
            return false;
        if (!currentChoices.contains(destination))
            return false;
        playerList.get(pl).position = destination;
        update();
        advance();
        return true;
    }

    MutablePair<ItemStack, ItemStack> displayTiles(int colour) {
        return switch (colour) {
            case 1 ->
                    MutablePair.of(new ItemStack(Material.YELLOW_STAINED_GLASS), new ItemStack(Material.YELLOW_CONCRETE));
            case 2 ->
                    MutablePair.of(new ItemStack(Material.PINK_STAINED_GLASS), new ItemStack(Material.PINK_CONCRETE));
            case 3 ->
                    MutablePair.of(new ItemStack(Material.LIME_STAINED_GLASS), new ItemStack(Material.LIME_CONCRETE));
            // case 4 ->
            //      MutablePair.of(new ItemStack(Material.YELLOW_STAINED_GLASS), new ItemStack(Material.YELLOW_CONCRETE));
            case 4 ->
                    MutablePair.of(new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS), new ItemStack(Material.LIGHT_BLUE_CONCRETE));
            default ->
                    MutablePair.of(new ItemStack(Material.BLACK_STAINED_GLASS), new ItemStack(Material.BLACK_CONCRETE));
        };
    }

    Railchess plugin;
    RailchessStand stand;

/*  public Location mid() {
        Location res = location.clone();
        Vector vec = hDir.clone();
        return res.add(vec.multiply(sizeH * 0.5));
    }*/

/*  public boolean isNearBy(Player pl) {
        return mid().getNearbyLivingEntities(RailchessStand.RANGE).contains(pl);
    }*/

    Game1(@NotNull Railchess pp, @NotNull RailchessStand st, @NotNull Railmap playMap, @NotNull List<Player> players, Location loc, double sH, double sV, int mStep, Vector hd, int mH, boolean sC) {
        maxHurt = mH;
        showChoices = sC;
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
        stand.game = this;
        log = playMap.readOnly;
        for (Player pl: players) {
            if (!subscriber.contains(pl)) subscriber.add(pl);
            plugin.playerSubGame.put(pl.getName(), this);
            if (pl.hasPermission("railchess.play")) {
                p.add(pl);
                plugin.playerInGame.put(pl.getName(), this);
            }
        }
        assert p.size() <= 4;
        n = p.size();
        remainingPlayers = p.size();
        Collections.shuffle(p);
        tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "eYellow", displayTiles(1)));
        tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "dPink", displayTiles(2)));
        tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "aLime", displayTiles(3)));
        // tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "eYellow", displayTiles(4)));
        tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "bLight Blue", displayTiles(4)));
        if (log)
            logger = new Game1Logger(p, playMap.mapId);
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
            playerList.add(new PlayerWrapper(pl, tileList.get(j).getRight(), spawn.get(j),
                    tileList.get(j).getLeft().substring(0, 2)));
        }
        broadcast("Game started: Map " + playMap.name + ", Maximum Steps " + maxStep);
        update();
        currentPlayer = n - 1;
        advance();
        // new AutoAdvance();
    }

}
