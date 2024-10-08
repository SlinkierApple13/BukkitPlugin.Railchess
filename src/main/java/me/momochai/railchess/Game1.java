package me.momochai.railchess;

import org.apache.commons.lang3.tuple.MutablePair;
import org.bukkit.*;
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
    boolean available = false;
    boolean showChoices = false;
    boolean log = false;
    public static final double BROADCAST_RANGE = 10.0d;
    Game1Logger logger;
    int maxNameLength = 0;
    public static final float BIG_BUTTON_SIZE = 0.115f;

    public class SaveLog extends BukkitRunnable {

        @Override
        public void run() {
            logger.save(new File(plugin.logFolder, logger.logId + ".game1"));
            // plugin.logList.put(logger.logId, logger);
            plugin.loadLog(logger.logId);
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
        broadcast("最终结果: ");
        for (PlayerWrapper pl: playerList)
            plainBroadcast(String.format("%" + (maxNameLength + 4) + "s", pl.displayName) + " -" +
                    String.format("%" + 5 + "s", pl.score));
        close();
    }

    public void advance() {
        if (!available) return;
        if (log) logger.advance(this, false);
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
            broadcast(getCurrent().displayName + " 卡住了 (" + getCurrent().hurt + "/" + (maxHurt - 1) + ")");
            if (getCurrent().hurt == maxHurt)
                getCurrent().quit(true, "卡住次数超过上限", true, true);
            else advance();
        }
    }

    public void broadcast(String s) {
        for (Player pl: subscriber)
            if (pl.isValid())
                Railchess.sendMessage(pl, s);
        if (log) logger.logMessage(ChatColor.COLOR_CHAR + "6对局回放 > " + ChatColor.COLOR_CHAR + "r" + s);
        if (!available) return;
        for (Player pl: stand.mid().getNearbyPlayers(BROADCAST_RANGE))
            if (!subscriber.contains(pl))
                Railchess.sendMessage(pl, s);
    }

    public void plainBroadcast(String s) {
        for (Player pl: subscriber)
            if (pl.isValid())
                pl.sendMessage(s);
        if (log) logger.logMessage(s);
        if (!available) return;
        for (Player pl: stand.mid().getNearbyPlayers(BROADCAST_RANGE))
            if (!subscriber.contains(pl))
                Railchess.sendMessage(pl, s, "");
    }

    public class PlayerWrapper {

        Player player;
        String playerName;
        int score;
        int maxScore;
        int prevScore = -1;
        ItemStack tile;
        ItemStack tile2;
        int position; // # of the station the player is at in map
        int step;
        boolean dead;
        int hurt;
        String displayName;
        // Color glowColour;

        public void getNextStep() {
            step = random.nextInt(maxStep) + 1;
            if (showChoices) return;
            player.sendMessage("你的下一个随机数是 " + step + ".");
        }

        public void getStep() {
            step = random.nextInt(maxStep) + 1;
        }

        public void broadcastStep() {
            broadcast(displayName + " 的回合: 随机数是 " + step + ".");
        }

        public void quit(boolean hasReason, String reason, boolean triggerEnd, boolean showMessage) {
            if (showMessage) {
                if (!hasReason)
                    broadcast(displayName + " 离开了.");
                else
                    broadcast(displayName + " 离开了: " + reason + ".");
            }
            if (player != null && player.isValid())
                if (player.getGameMode() != GameMode.CREATIVE &&
                    player.getGameMode() != GameMode.SPECTATOR)
                    player.setAllowFlight(false);
            dead = true;
            boolean duplicateAlive = false;
            for (PlayerWrapper plw: playerList)
                duplicateAlive |= (!plw.dead && plw.playerName.equals(playerName));
            if (!duplicateAlive)
                plugin.playerInGame.remove(playerName);
            if (!triggerEnd) {
                plugin.playerSubGame.remove(playerName);
                subscriber.remove(player);
            }
            --remainingPlayers;
            if (triggerEnd) {
                update();
                choices(currentPlayer, getCurrent().step, 1, showChoices);
            }
//            if (remainingPlayers == 1) {
//                for (PlayerWrapper plw: playerList)
//                    if (!plw.dead)
//                        plw.score = plw.maxScore;
//            }
            if (remainingPlayers <= 1 && triggerEnd)
                end();
            else if (getCurrent().equals(this) && triggerEnd)
                advance();
        }

        PlayerWrapper(@NotNull Player pl, @NotNull MutablePair<ItemStack, ItemStack> ti, int pos, String prefix) {
            position = pos;
            player = pl;
            playerName = pl.getName();
            tile = ti.getLeft();
            tile2 = ti.getRight();
            score = 0;
            maxScore = 0;
            step = 0;
            dead = false;
            hurt = 0;
            displayName = prefix + pl.getName() + ChatColor.COLOR_CHAR + "r";
//            try {
//                char ch = prefix.charAt(1);
//                glowColour = Color.WHITE;
//                if (ch == '6') glowColour = Color.ORANGE;
//                if (ch == 'a') glowColour = Color.LIME;
//                if (ch == 'b') glowColour = Color.AQUA;
//                if (ch == 'd') glowColour = Color.FUCHSIA;
//                if (ch == 'e') glowColour = Color.YELLOW;
//            } catch (Exception ignored) {}
        }

    }

    public class StationWrapper {

        Station station;
        boolean occupied;
        boolean dead;
        int occupiedBy;
        ItemDisplay entity;
        ItemDisplay entity2;
        ItemDisplay glower;
        int reachableBy; // sum of (2^(i)) for all reachable player i
        boolean cancelMark = false;
        public static final ItemStack DEAD = new ItemStack(Material.GRAY_STAINED_GLASS);
        public static final ItemStack NORMAL = new ItemStack(Material.AIR);

        public boolean allows(int pl) {
            return !occupied || occupiedBy == pl;
        }

        public void update(boolean isPlayerPos, ItemStack displayStack) {
            if (dead || (isPlayerPos && displayStack == null))
                return;
            if (!occupied && (reachableBy == 1 || reachableBy == 2 || reachableBy == 4 ||
                    reachableBy == 8 || reachableBy == 16 || reachableBy == 32)) {
                occupied = true;
                occupiedBy = 0;
                for (int i = 1; i <= 16; i *= 2)
                    if (reachableBy > i) ++occupiedBy;
                playerList.get(occupiedBy).score += station.value;
                if (!isPlayerPos) autoMark();
                if (isPlayerPos) {
                    mark(displayStack, false, true);
                    cancelMark = true;
                }
                return;
            }
            if (!occupied && reachableBy == 0)
                dead = true;
            if (!isPlayerPos) autoMark();
            if (isPlayerPos) {
                mark(displayStack, false, true);
                cancelMark = true;
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
                mark(new ItemStack(NORMAL), false, false);
                return;
            }
            if (dead) {
                mark(new ItemStack(DEAD), false, false);
                return;
            }
            mark(playerList.get(occupiedBy).tile, false, false);
        }

        public void mark(ItemStack item, boolean bold, boolean big) {
//            unglow();
//            if (glowColour != null) glow(glowColour);
            if (cancelMark) {
                cancelMark = false;
                return;
            }
            final float size = big ? BIG_BUTTON_SIZE : Railchess.BUTTON_SIZE;
            if (entity == null || !entity.isValid()) {
                entity = (ItemDisplay) location.getWorld().spawnEntity(getLocation(), EntityType.ITEM_DISPLAY);
                entity.setBrightness(new Display.Brightness(15, 0));
            }
            entity.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f),
                    new Quaternionf(), new Vector3f(size, size, size), new Quaternionf()));
            entity.setItemStack(item);
            entity.setInvulnerable(true);
            entity.addScoreboardTag("railchess");
            if (!bold) {
                if (entity2 != null && entity2.isValid())
                    entity2.remove();
                return;
            }
            if (entity2 == null || !entity2.isValid()) {
                entity2 = (ItemDisplay) location.getWorld().spawnEntity(getLocation(), EntityType.ITEM_DISPLAY);
                entity2.setBrightness(new Display.Brightness(15, 0));
            }
            entity2.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f),
                    new Quaternionf(), new Vector3f(size, size, size), new Quaternionf()));
            entity2.setItemStack(item);
            entity2.setInvulnerable(true);
            entity2.addScoreboardTag("railchess");
        }

//        public void glow(Color colour) {
//            if (colour == null) return;
//            if (glower == null || !glower.isValid()) {
//                glower = (ItemDisplay) location.getWorld().spawnEntity(getLocation(), EntityType.ITEM_DISPLAY);
//                glower.setBrightness(new Display.Brightness(15, 0));
//                glower.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f),
//                        new Quaternionf(), new Vector3f(BIG_BUTTON_SIZE, BIG_BUTTON_SIZE, BIG_BUTTON_SIZE), new Quaternionf()));
//            }
//            glower.setItemStack(new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS));
//            glower.setGlowColorOverride(colour);
//            glower.setGlowing(true);
//        }
//
//        public void unglow() {
//            if (glower == null || !glower.isValid()) return;
//            glower.setGlowing(false);
//        }

        public void close() {
            entity.remove();
            if (entity2 != null && entity2.isValid())
                entity2.remove();
            if (glower != null && glower.isValid())
                glower.remove();
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
            return Material.LIGHT_GRAY_STAINED_GLASS;
        return Material.BLACK_CONCRETE;
    }

    public void close() {
        if (log && !plugin.disabled) {
            logger.advance(this, true);
            try {
                new SaveLog();
            } catch (Exception ignored) {}
        }
        subscriber.clear();
        for (PlayerWrapper plw: playerList)
            plw.quit(false, "", false, false);
        for (StationWrapper stw: stationList.values())
            stw.close();
        stand.game = null;
    }

    public PlayerWrapper getPlayerWrapper(String playerName) {
        for (PlayerWrapper plw: playerList) {
            if (plw.playerName.equals(playerName))
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
        try {
            if (!available || !pl.equals(getCurrentPlayer())) return false;
            int station = getStation(pl);
            if (station == -1) return false;
            return move(currentPlayer, station);
        } catch (Exception ignored) { return false; }
    }

    public double dist2(double a, double b, @NotNull MutablePair<Double, Double> nPos) {
        double c = nPos.getLeft();
        double d = nPos.getRight();
        return (a - c) * (a - c) * sizeH * sizeH + (b - d) * (b - d) * sizeV * sizeV;
    }

    private class Task {
        int player;
        int station;
        boolean fromThoroughfare;

        Task(int pl, int st) {
            player = pl;
            station = st;
            fromThoroughfare = false;
        }

        Task(int pl, int st, boolean th) {
            player = pl;
            station = st;
            fromThoroughfare = th;
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
            if (nb.getRight() == prev || nb.getLeft() == Railmap.THOROUGHFARE)
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
        Station sta = stationList.get(playerList.get(pl).position).station;
        for (MutablePair<Integer, Integer> p: sta.neighbour) {
            if (p.getLeft() == Railmap.THOROUGHFARE && stationList.get(p.getRight()).allows(pl))
                choices0(pl, p.getRight(), -1, steps, -1, interchanges, res, new ArrayList<>());
        }
        if (showAvail) res.forEach(i -> {
            try {
                Material mat;
                if (playerList.get(pl).position == i)
                    mat = darkened(tileList.get(pl).getRight().getRight().getType());
                else if (stationList.get(i).occupied)
                    mat = darkened(tileList.get(pl).getRight().getLeft().getType());
                else mat = darkened(StationWrapper.NORMAL.getType());
                stationList.get(i).mark(new ItemStack(mat), playerList.get(pl).position != i
                        /* && stationList.get(i).occupied */, true);
            } catch (Exception ignored) {}
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
        Set<MutablePair<Integer, Integer>> visitedFromThoroughfareOnly = new HashSet<>(); // (player, station)
        for (int i = 0; i < n; ++i) {
            playerList.get(i).maxScore = 0;
            if (playerList.get(i).dead) continue;
            taskQueue.add(new Task(i, playerList.get(i).position));
            stationList.get(playerList.get(i).position).reachableBy = (1 << i);
            stationList.get(playerList.get(i).position).update(true, playerList.get(i).tile2);
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
                    if (fr.fromThoroughfare && i.getLeft() == Railmap.THOROUGHFARE) continue;
                    StationWrapper nb = stationList.get(i.getRight());
                    if ((nb.isReachable(fr.player) && !visitedFromThoroughfareOnly.contains(MutablePair.of(fr.player, i.getRight())))
                            || (nb.occupied && nb.occupiedBy != fr.player))
                        continue;
                    if (!nb.isReachable(fr.player) && i.getLeft() == Railmap.THOROUGHFARE)
                        visitedFromThoroughfareOnly.add(MutablePair.of(fr.player, i.getRight()));
                    if (nb.isReachable(fr.player) && i.getLeft() != Railmap.THOROUGHFARE)
                        visitedFromThoroughfareOnly.remove(MutablePair.of(fr.player, i.getRight()));
                    if (!nb.occupied && !nb.isReachable(fr.player)) playerList.get(fr.player).maxScore += nb.station.value;
                    nb.setReachable(fr.player);
                    taskQueue.add(new Task(fr.player, i.getRight(), i.getLeft() == Railmap.THOROUGHFARE));
                }
            }
        } catch (Exception e) {
            broadcast(e.getMessage());
        }
        // broadcast("Station claim updated successfully.");
        for (StationWrapper stw: stationList.values())
            stw.update(false, null);
        for (PlayerWrapper plw: playerList) {
            stationList.get(plw.position).mark(plw.tile2, false, true);
            if (plw.dead) continue;
            if (plw.maxScore == plw.score)
                plw.quit(true, "已占领所有可能到达的车站", true, true);
            if (!available) return;
        }
        for (int i = 0; i < n; ++i) {
            String tail = ChatColor.COLOR_CHAR + "6" + ChatColor.COLOR_CHAR + "r";
            if (!(playerList.get(i).prevScore == -1 || playerList.get(i).prevScore == playerList.get(i).score))
                tail = ChatColor.COLOR_CHAR + "6+" + (playerList.get(i).score - playerList.get(i).prevScore) + ChatColor.COLOR_CHAR + "r";
            plainBroadcast(String.format("%" + (maxNameLength + 4) + "s", playerList.get(i).displayName) + " -" +
                    String.format("%" + 5 + "s", playerList.get(i).score) + " /" + ChatColor.COLOR_CHAR + "7" +
                    String.format("%" + 5 + "s", playerList.get(i).maxScore) + ChatColor.COLOR_CHAR + "r" +
                    String.format("%" + 10 + "s", tail));
            playerList.get(i).prevScore = playerList.get(i).score;
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
        Railchess.sendMessage(pl, " 已加入旁观.");
        if (plugin.playerSubGame.containsKey(pl.getName()) && plugin.playerSubGame.get(pl.getName()) != null)
            plugin.playerSubGame.get(pl.getName()).desubscribe(pl);
        plugin.playerSubGame.put(pl.getName(), this);
    }

    public void desubscribe(@NotNull Player pl) {
        Railchess.sendMessage(pl, " 已退出旁观.");
        subscriber.remove(pl);
        plugin.playerSubGame.remove(pl.getName());
    }

    public boolean move(int pl, int destination) {
        if (pl != currentPlayer)
            return false;
        if (!currentChoices.contains(destination))
            return false;
        playerList.get(pl).position = destination;
        broadcast(getCurrent().displayName + " 已落子.");
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
            case 4 ->
                    MutablePair.of(new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS), new ItemStack(Material.LIGHT_BLUE_CONCRETE));
            default ->
                    MutablePair.of(new ItemStack(Material.BLACK_STAINED_GLASS), new ItemStack(Material.BLACK_CONCRETE));
        };
    }

    int getColour(String str) {
        if (Objects.equals(str, ChatColor.COLOR_CHAR + "e黄色"))
            return 1;
        if (Objects.equals(str, ChatColor.COLOR_CHAR + "d粉色"))
            return 2;
        if (Objects.equals(str, ChatColor.COLOR_CHAR + "a绿色"))
            return 3;
        if (Objects.equals(str, ChatColor.COLOR_CHAR + "b蓝色"))
            return 4;
        return -1;
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
        available = false;
        maxHurt = mH + 1;
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
                pl.setAllowFlight(true);
            }
            if (maxNameLength < pl.getName().length()) maxNameLength = pl.getName().length();
        }
        assert p.size() <= 4;
        n = p.size();
        remainingPlayers = p.size();
        Collections.shuffle(p);
        tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "e黄色", displayTiles(1)));
        tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "d粉色", displayTiles(2)));
        tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "a绿色", displayTiles(3)));
        tileList.add(MutablePair.of(ChatColor.COLOR_CHAR + "b蓝色", displayTiles(4)));
        Collections.shuffle(tileList);
        if (log) {
            logger = new Game1Logger(playMap.mapId);
            for (int i = 0; i < n; ++i)
                logger.playerColour.add(getColour(tileList.get(i).getLeft()));
        }
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
            plainBroadcast(p.get(j).getName() + " 的颜色是 " + tileList.get(j).getLeft() + ChatColor.COLOR_CHAR + "r.");
            playerList.add(new PlayerWrapper(pl, tileList.get(j).getRight(), spawn.get(j),
                    tileList.get(j).getLeft().substring(0, 2)));

        }
        broadcast("游戏开始: 地图 " + playMap.name + ", 随机数上限 " + maxStep + ", 最多卡住 " + mH + " 次.");
        available = true;
        update();
        currentPlayer = n - 1;
        advance();
        // new AutoAdvance();
    }

}
