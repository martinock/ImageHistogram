package com.martinock.imagehistogram;

/**
 * A class to store matrix constant for filtering.
 */

public class ConstantMatrix {
    public static final int[][] SOBEL_OPERATOR_X = new int[][] {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
    };

    public static final int[][] SOBEL_OPERATOR_Y = new int[][] {
            {1, 2, 1},
            {0, 0, 0},
            {-1, -2, -1}
    };

    public static final int[][] PREWITT_OPERATOR_X = new int[][] {
            {-1, 0, 1},
            {-1, 0, 1},
            {-1, 0, 1}
    };

    public static final int[][] PREWITT_OPERATOR_Y = new int[][] {
            {1, 1, 1},
            {0, 0, 0},
            {-1, -1, -1}
    };
}
