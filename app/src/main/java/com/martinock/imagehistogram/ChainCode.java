package com.martinock.imagehistogram;

import java.util.ArrayList;

/**
 * A class represent a single object found in image.
 * Contain start tracking point, centroid, and chain code
 * @author Martino Christanto Khuangga <martino.aksel.11@gmailcom>
 * @since 2017.10.13
 */

public class ChainCode {
    private int startX;
    private int startY;
    private int centroidX;
    private int centroidY;
    private ArrayList<Integer> code;

    public ChainCode(int x, int y) {
        startX = x;
        startY = y;
        centroidX = 0;
        centroidY = 0;
        code = new ArrayList<>();
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public ArrayList<Integer> getCode() {
        return code;
    }

    public void addCode(int dir) {
        code.add(dir);
    }

    public void setCentroidX(int x) {
        centroidX = x;
    }

    public void setCentroidY(int y) {
        centroidY = y;
    }

    public int getCentroidX() {
        return centroidX;
    }

    public int getCentroidY() {
        return centroidY;
    }
}
