package com.martinock.imagehistogram;

/**
 * A class representing face.
 * @author Martino Christanto Khuanga
 * @since 2017.10.18
 */
public class Face {
    private int mostLeftX;
    private int mostRightX;
    private int mostTopY;
    private int mostBottomY;
    private int faceWidth;
    private int faceHeight;

    public Face(int width, int height) {
        faceWidth = width;
        faceHeight = height;
    }

    public int getFaceHeight() {
        return faceHeight;
    }

    public int getFaceWidth() {
        return faceWidth;
    }

    public int getMostLeftX() {
        return mostLeftX;
    }

    public int getMostRightX() {
        return mostRightX;
    }

    public int getMostTopY() {
        return mostTopY;
    }

    public int getMostBottomY() {
        return mostBottomY;
    }

    public void setMostLeftX(int mostLeftX) {
        this.mostLeftX = mostLeftX;
    }

    public void setMostRightX(int mostRightX) {
        this.mostRightX = mostRightX;
    }

    public void setMostTopY(int mostTopY) {
        this.mostTopY = mostTopY;
    }

    public void setMostBottomY(int mostBottomY) {
        this.mostBottomY = mostBottomY;
    }
}
