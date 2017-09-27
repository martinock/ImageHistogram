package com.martinock.imagehistogram;

import java.util.ArrayList;

/**
 * Created by Nino on 9/27/2017.
 */

public class ChainCode {
    private int startX;
    private int startY;
    private ArrayList<Integer> code;

    public ChainCode(int x, int y) {
        startX = x;
        startY = y;
        code = new ArrayList<>();
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public void addCode(int dir) {
        code.add(dir);
    }

    public int getCodeSize() {
        return code.size();
    }
}
