package me.momochai.railchess;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.NotNull;

public class Railmap {

    String name;
    Map<Integer, Station> station = new HashMap<>();
    List<Integer> spawn = new ArrayList<>();
    List<MutablePair<Integer, Integer>> transferRepellence = new ArrayList<>();
    List<MutablePair<Integer, Integer>> spawnRepellence = new ArrayList<>();
    boolean valid = false;
    long mapId;
    boolean readOnly = false;

    Railmap() {}

    Railmap(@NotNull File file) {
        load(file);
    }

    public boolean load(@NotNull File file) {
        try {
            //  System.out.println("Loading " + file.getName());
            exceptionLoad(file);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    public void exceptionLoad(@NotNull File file) throws IOException {
        Scanner scanner = new Scanner(file);
        int formatVersion = scanner.nextInt();
        if (formatVersion > 1 || formatVersion < 0) {
            scanner.close();
            return;
        }
        if (formatVersion == 1) {
            mapId = scanner.nextInt();
            readOnly = scanner.nextBoolean();
        } else {
            mapId = System.currentTimeMillis();
        }
        int rule = scanner.nextInt();
        if (rule != 1) {
            scanner.close();
            return;
        }
        scanner.nextLine();
        name = scanner.nextLine().replaceAll("\\s+", "");
        int sts = scanner.nextInt();
        for (int i = 0; i < sts; ++i) {
            int id = scanner.nextInt();
            // System.out.println("Loading railmap " + name + ": station " + id);
            int value = scanner.nextInt();
            double x = scanner.nextDouble();
            double y = scanner.nextDouble();
            Station sta = new Station();
            sta.value = value;
            sta.normPos = new MutablePair<>(x, y);
            int neighbours = scanner.nextInt();
            int forbids = scanner.nextInt();
            for (int j = 0; j < neighbours; ++j) {
                int li = scanner.nextInt();
                int st = scanner.nextInt();
                if (st == id || sta.neighbour.contains(MutablePair.of(li, st)))
                    continue;
                sta.neighbour.add(MutablePair.of(li, st));
            }
            for (int j = 0; j < forbids; ++j) {
                int from = scanner.nextInt();
                int line = scanner.nextInt();
                int to = scanner.nextInt();
                sta.forbid.add(new ForbidTrain(from, line, to));
            }
            station.put(id, sta);
            // System.out.println("Loaded railmap " + name + ": station " + id);
        }
        int sps = scanner.nextInt();
        for (int i = 0; i < sps; ++i)
            spawn.add(scanner.nextInt());
        int rep = scanner.nextInt();
        for (int i = 0; i < rep; ++i) {
            int a = scanner.nextInt();
            int b = scanner.nextInt();
            transferRepellence.add(MutablePair.of(a, b));
        }
        int srp = scanner.nextInt();
        for (int i = 0; i < srp; ++i) {
            int a = scanner.nextInt();
            int b = scanner.nextInt();
            spawnRepellence.add(MutablePair.of(a, b));
        }
        System.out.println("Successfully loaded " + file.getName());
        valid = true;
        scanner.close();
        station.forEach((id, sta) -> {
           sta.neighbour.removeIf(nb -> !station.containsKey(nb.getRight()));
           sta.forbid.removeIf(fb -> !station.containsKey(fb.from) || !station.containsKey(fb.to));
        });
        if (formatVersion < 1) {
            file.renameTo(new File(file.getParentFile(), file.getName() + ".old"));
            save(new File(file.getParentFile(), mapId + ".railmap"));
            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {}
        }
    }

    public boolean save(@NotNull File file) {
        try {
            file.createNewFile();
            PrintWriter writer = new PrintWriter(file);
            writer.println(1);
            writer.println(mapId);
            writer.println(readOnly);
            writer.println(1);
            writer.println(name);
            writer.println(station.size());
            station.forEach((Integer a, Station sta) -> {
                writer.println(a + " " + sta.value + " " + sta.normPos.getLeft() + " " +
                        sta.normPos.getRight() + " " + sta.neighbour.size() + " " + sta.forbid.size());
                for (MutablePair<Integer, Integer> pr : sta.neighbour)
                    writer.println(pr.getLeft() + " " + pr.getRight());
                for (ForbidTrain fbt: sta.forbid)
                    writer.println(fbt.from + " " + fbt.line + " " + fbt.to);
            });
            writer.println(spawn.size());
            for (int i: spawn)
                writer.println(i);
            writer.println(transferRepellence.size());
            for (MutablePair<Integer, Integer> p : transferRepellence)
                writer.println(p.getLeft() + " " + p.getRight());
            writer.println(spawnRepellence.size());
            for (MutablePair<Integer, Integer> p : spawnRepellence)
                writer.println(p.getLeft() + " " + p.getRight());
            writer.close();
            System.out.println("Successfully saved " + file.getName());
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

}
