package me.momochai.railchess;

public class ForbidTrain {

    int from, line, to;

    ForbidTrain(int f, int l, int t) {
        from = f;
        line = l;
        to = t;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ForbidTrain))
            return false;
        ForbidTrain fbt = (ForbidTrain) obj;
        if (from == fbt.from && line == fbt.line && to == fbt.to)
            return true;
        return false;
    }

}