package com.martinock.imagehistogram;

/**
 * Created by tes on 11/1/2017.
 */

public class FaceBound {
    public int maxX;
    public int maxY;
    public int minX;
    public int minY;

    public FaceBound(int maxX, int minX, int maxY, int minY) {
        this.maxX = maxX;
        this.maxY = maxY;
        this.minX = minX;
        this.minY = minY;
    }

    public boolean isInBoundary(int x, int y) {
        return (x >= minX && x <= maxX && y >= minY && y <= maxY);
    }
}
