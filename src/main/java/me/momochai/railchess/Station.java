package me.momochai.railchess;

import org.apache.commons.lang3.tuple.MutablePair;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class Station implements Cloneable {

    List<MutablePair<Integer, Integer>> neighbour = new ArrayList<>(); // pair<line #, station #>
    List<ForbidTrain> forbid = new ArrayList<>();
    int value = -1;
    MutablePair<Double, Double> normPos = new MutablePair<>(); // position, normalised between 0 and 1

    @Override
    public Station clone() {
        Station sta = new Station();
        sta.neighbour.addAll(neighbour);
        sta.forbid.addAll(forbid);
        sta.value = value;
        sta.normPos = MutablePair.of(normPos.getLeft(), normPos.getRight());
        return sta;
    }

}

