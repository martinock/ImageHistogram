package com.martinock.imagehistogram;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SplashActivity extends AppCompatActivity {

    public static List<Face> faces = new ArrayList<>();
    private static List<Integer> imagesId = new ArrayList<>();

    private static int BLACK_COLOR = Color.rgb(0, 0, 0);
    private static int WHITE_COLOR = Color.rgb(255, 255, 255);
    private static int RED_COLOR = Color.rgb(255, 0, 0);
    private static int GREEN_COLOR = Color.rgb(0, 255, 0);

    private BitmapDrawable originalImageBitmapDrawable;
    private Bitmap grayScaleBitmap;
    private Bitmap blackAndWhiteBitmap;
    private Bitmap newGrayScaleBitmap;
    private Bitmap smoothedGrayScaleBitmap;

    private ArrayList<ChainCode> objectCodes = new ArrayList<>();

    private int[] grayHistogram = new int[256];
    private int bwThreshold;
    private int maxX, maxY, minX, minY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initImageId();

        new Thread(new Runnable() {
            @Override
            public void run() {
                preProcessImages();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(getApplicationContext(),
                                MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        }).start();
    }

    private void initImageId() {
        imagesId.add(R.drawable.photo_1);
        imagesId.add(R.drawable.photo_2);
        imagesId.add(R.drawable.photo_3);
        imagesId.add(R.drawable.photo_4);
        imagesId.add(R.drawable.photo_5);
        imagesId.add(R.drawable.photo_6);
        imagesId.add(R.drawable.photo_7);
        imagesId.add(R.drawable.photo_8);
        imagesId.add(R.drawable.photo_9);
        imagesId.add(R.drawable.photo_10);
        imagesId.add(R.drawable.photo_11);
        imagesId.add(R.drawable.photo_12);
    }

    private void preProcessImages() {
        for (int imageId : imagesId) {
            Drawable newImage = ResourcesCompat.getDrawable(
                    getResources(), imageId, null);
            originalImageBitmapDrawable =
                    (BitmapDrawable) newImage;
            initGrayImage();
            otsuThresholding();
            countObject();
            Face face = new Face(
                    originalImageBitmapDrawable.getBitmap().getWidth(),
                    originalImageBitmapDrawable.getBitmap().getHeight());
            int leftX = 255, rightX = 0;
            int topY = 255, bottomY = 0;
            for (ChainCode c : objectCodes) {
                if (c.getCentroidX() < leftX
                        && leftX > 0.1 * face.getFaceWidth()) {
                    leftX = c.getCentroidX();
                }
                if (c.getCentroidX() > rightX
                        && rightX < 0.9 * face.getFaceWidth()) {
                    rightX = c.getCentroidX();
                }
                if (c.getCentroidY() > bottomY
                        && bottomY < 0.9 * face.getFaceHeight()) {
                    bottomY = c.getCentroidY();
                }
                if (c.getCentroidY() < topY && topY >= 10) {
                    topY = c.getCentroidY();
                }
            }
            face.setMostLeftX(leftX);
            face.setMostRightX(rightX);
            face.setMostTopY(topY);
            face.setMostBottomY(bottomY);
            faces.add(face);
            objectCodes.clear();
        }
    }

    private void countObject() {
        int height = blackAndWhiteBitmap.getHeight();
        int width = blackAndWhiteBitmap.getWidth();
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int pixel = blackAndWhiteBitmap.getPixel(j, i);
                if (pixel == BLACK_COLOR) {
                    ChainCode codeObject = new ChainCode(j, i);
                    traceBoundary(j, i, codeObject, width, height);
                    objectCodes.add(codeObject);
                }
            }
        }
    }

    /**
     * Algorithm to get chaincode from an object.
     * @param x starting point
     * @param y starting point
     * @param object chaincode object to store the direction list
     * @param width image width
     * @param height image height
     */
    private void traceBoundary(int x, int y, ChainCode object,
                               int width, int height) {
        int dir = 7;
        int currentX = x;
        int currentY = y;
        boolean isDone = false;
        if (dir % 2 == 0) {
            dir = (dir + 7) % 8;
        } else {
            dir = (dir + 6) % 8;
        }
        int firstDir = dir;
        while (!isDone) {
            //change the direction initialization
            int neighbourPixel = WHITE_COLOR;
            int neighbourX = currentX;
            int neighbourY = currentY;
            switch (dir) {
                case 0:
                    if (currentX != width - 1) {
                        neighbourX = currentX + 1;
                        neighbourY = currentY;
                        neighbourPixel = blackAndWhiteBitmap.getPixel(
                                neighbourX, neighbourY);
                    }
                    break;
                case 1:
                    if (currentX != width - 1 && currentY != 0) {
                        neighbourX = currentX + 1;
                        neighbourY = currentY - 1;
                        neighbourPixel = blackAndWhiteBitmap.getPixel(
                                neighbourX, neighbourY);
                    }
                    break;
                case 2:
                    if (currentY != 0) {
                        neighbourX = currentX;
                        neighbourY = currentY - 1;
                        neighbourPixel = blackAndWhiteBitmap.getPixel(
                                neighbourX, neighbourY);
                    }
                    break;
                case 3:
                    if (currentX != 0 && currentY != 0) {
                        neighbourX = currentX - 1;
                        neighbourY = currentY - 1;
                        neighbourPixel = blackAndWhiteBitmap.getPixel(
                                neighbourX, neighbourY);
                    }
                    break;
                case 4:
                    if (currentX != 0) {
                        neighbourX = currentX - 1;
                        neighbourY = currentY;
                        neighbourPixel = blackAndWhiteBitmap.getPixel(
                                neighbourX, neighbourY);
                    }
                    break;
                case 5:
                    if (currentX != 0 && currentY != height-1) {
                        neighbourX = currentX - 1;
                        neighbourY = currentY + 1;
                        neighbourPixel = blackAndWhiteBitmap.getPixel(
                                neighbourX, neighbourY);
                    }
                    break;
                case 6:
                    if (currentY != height-1) {
                        neighbourX = currentX;
                        neighbourY = currentY + 1;
                        neighbourPixel = blackAndWhiteBitmap.getPixel(
                                neighbourX, neighbourY);
                    }
                    break;
                case 7:
                    if (currentX != width - 1 && currentY != height-1) {
                        neighbourX = currentX + 1;
                        neighbourY = currentY + 1;
                        neighbourPixel = blackAndWhiteBitmap.getPixel(
                                neighbourX, neighbourY);
                    }
                    break;
            }
            if (neighbourPixel == BLACK_COLOR) {
                currentX = neighbourX;
                currentY = neighbourY;
                object.addCode(dir);
                if (currentX == x && currentY == y) {
                    isDone = true;
                    floodFill(x, y, BLACK_COLOR, WHITE_COLOR);
                    object.setCentroidX((maxX + minX)/2);
                    object.setCentroidY((maxY + minY)/2);
                }
                if (dir % 2 == 0) {
                    dir = (dir + 7) % 8;
                } else {
                    dir = (dir + 6) % 8;
                }
                firstDir = dir;
            } else {
                dir = (dir + 1) % 8;
                if (dir == firstDir) {
                    isDone = true;
                }
            }
        }
    }

    /**
     * Flood fill algorithm without recursive.
     * @param x starting point
     * @param y starting point
     * @param prevColor color to change
     * @param newColor color written
     */
    private void floodFill(int x, int y, int prevColor, int newColor) {
        maxX = x;
        maxY = y;
        minX = x;
        minY = y;
        Queue<Integer> queueX = new LinkedList<>();
        Queue<Integer> queueY = new LinkedList<>();
        queueX.add(x);
        queueY.add(y);
        while (queueX.size() > 0 && queueY.size() > 0) {
            int pointX = queueX.poll();
            int pointY = queueY.poll();
            if (blackAndWhiteBitmap.getPixel(pointX, pointY) != prevColor) {
                continue;
            }

            int nextX = pointX + 1;
            while ((pointX >= 0) && (blackAndWhiteBitmap.getPixel(pointX, pointY)
                    == prevColor)) {
                if (pointX < minX) {
                    minX = pointX;
                }
                if (pointY > maxY) {
                    maxY = pointY;
                }
                if (pointY < minY) {
                    minY = pointY;
                }
                blackAndWhiteBitmap.setPixel(pointX, pointY, newColor);
                if ((pointY > 0)
                        && (blackAndWhiteBitmap.getPixel(pointX, pointY - 1)
                        == newColor)) {
                    queueX.add(pointX);
                    queueY.add(pointY - 1);
                }
                if ((pointY < blackAndWhiteBitmap.getHeight() - 1)
                        && (blackAndWhiteBitmap.getPixel(pointX, pointY + 1)
                        == prevColor)) {
                    queueX.add(pointX);
                    queueY.add(pointY + 1);
                }
                pointX--;
            }
            while ((nextX < blackAndWhiteBitmap.getWidth() - 1)
                    && (blackAndWhiteBitmap.getPixel(nextX, pointY)
                    == prevColor)) {
                if (pointX > maxX) {
                    maxX = pointX;
                }
                if (pointY > maxY) {
                    maxY = pointY;
                }
                if (pointY < minY) {
                    minY = pointY;
                }
                blackAndWhiteBitmap.setPixel(nextX, pointY, newColor);

                if ((pointY > 0) && (blackAndWhiteBitmap.getPixel(
                        nextX, pointY - 1) == prevColor)) {
                    queueX.add(nextX);
                    queueY.add(pointY - 1);
                }
                if ((pointY < blackAndWhiteBitmap.getHeight() - 1)
                        && (blackAndWhiteBitmap.getPixel(nextX, pointY + 1)
                        == prevColor)) {
                    queueX.add(nextX);
                    queueY.add(pointY + 1);
                }
                nextX++;
            }
        }
    }

    private void initGrayImage() {
        int height = originalImageBitmapDrawable.getBitmap().getHeight();
        int width = originalImageBitmapDrawable.getBitmap().getWidth();
        grayScaleBitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.RGB_565);

        for (int i = 0; i < grayHistogram.length; i++) {
            grayHistogram[i] = 0;
        }

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pixel = originalImageBitmapDrawable.getBitmap().getPixel(j, i);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                int gray = ((red + green + blue) / 3);
                grayHistogram[gray]++;
                int grayColor = Color.rgb(gray, gray, gray);
                grayScaleBitmap.setPixel(j, i, grayColor);
            }
        }
        equalizeGrayImage(height, width);
        smoothPicture();
    }

    private void equalizeGrayImage(int height, int width) {
        int pixelCount = height * width;
        double[] probabilityArray = new double[256];
        double[] cummulativeProbability = new double[256];
        newGrayScaleBitmap = Bitmap.createBitmap(
                width, height, Bitmap.Config.RGB_565);
        for (int i = 0; i < grayHistogram.length; i++) {
            probabilityArray[i] = (double) grayHistogram[i]
                    / (double) pixelCount;
            if (i == 0) {
                cummulativeProbability[i] = probabilityArray[i];
            } else {
                cummulativeProbability[i] = (cummulativeProbability[i-1]
                        + probabilityArray[i]);
            }
        }
        int[] roundingArray = new int[256];
        for (int i = 0; i < cummulativeProbability.length; i++) {
            roundingArray[i] = (int) Math.floor(cummulativeProbability[i]
                    * 255);
        }

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pixel = grayScaleBitmap.getPixel(j, i);
                int red = Color.red(pixel);
                int newPixel = Color.rgb(roundingArray[red],
                        roundingArray[red],
                        roundingArray[red]);
                newGrayScaleBitmap.setPixel(j, i, newPixel);
            }
        }
    }

    private void smoothPicture() {
        int height = newGrayScaleBitmap.getHeight();
        int width = newGrayScaleBitmap.getWidth();
        smoothedGrayScaleBitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.RGB_565);
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                double mean = 0.0;
                int k, l;
                int heightBound = i + 1;
                int widthBound = j + 1;
                if (i == 0) {
                    k = 0;
                } else if (i == height - 1) {
                    k = i - 1;
                    heightBound = i;
                } else {
                    k = i - 1;
                }
                if (j == 0) {
                    l = 0;
                } else if (j == width - 1) {
                    l = j - 1;
                    widthBound = j;
                } else {
                    l = j - 1;
                }
                int count = 0;
                while (k <= heightBound) {
                    while (l <= widthBound) {
                        int neighbourPixel = newGrayScaleBitmap.getPixel(l, i);
                        int intensity = Color.red(neighbourPixel);
                        mean = mean + (double)intensity;
                        count++;
                        ++l;
                    }
                    ++k;
                }
                mean = mean / (double) count;
                int flooredMean = (int)Math.floor(mean);
                int newColor = Color.rgb(flooredMean, flooredMean, flooredMean);
                if (i == 0) {
                    k = 0;
                } else {
                    k = i - 1;
                }
                if (j == 0) {
                    l = 0;
                } else {
                    l = j - 1;
                }
                while (k <= heightBound) {
                    while (l <= widthBound) {
                        smoothedGrayScaleBitmap.setPixel(l, k, newColor);
                        ++l;
                    }
                    ++k;
                }
            }
        }
    }

    private void otsuThresholding() {
        int width = originalImageBitmapDrawable.getBitmap().getWidth();
        int height = originalImageBitmapDrawable.getBitmap().getHeight();
        int pixelCount = width * height;

        //Calculate sum of pixel value
        float sum = 0;
        for (int i = 0; i < 256; i++) {
            sum = sum + (float)(i * grayHistogram[i]);
        }

        float sumBackground = 0;
        int weightBackground = 0;
        int weightForeground;

        float maxVariance = 0;
        bwThreshold = 0;

        for (int i = 0; i < 40; i++) {
            //Make sure the first sum of element is not zero
            weightBackground = weightBackground + grayHistogram[i];
            if (weightBackground == 0) {
                continue;
            }

            //Make sure that the next element is not zero
            weightForeground = pixelCount - weightBackground;
            if (weightForeground == 0) {
                break;
            }

            sumBackground = sumBackground + (float) (i * grayHistogram[i]);

            float meanBackground = sumBackground / weightBackground;
            float meanForeground = (sum - sumBackground) / weightForeground;

            //Calculate Between Class Variance
            float betweenVariance = (float)weightBackground
                    * (float)weightForeground
                    * (meanBackground - meanForeground)
                    * (meanBackground - meanForeground);

            //The max value is the threshold we search
            if (betweenVariance > maxVariance) {
                maxVariance = betweenVariance;
                bwThreshold = i;
            }
        }
        convertToBW();
    }

    private void convertToBW() {
        int height = smoothedGrayScaleBitmap.getHeight();
        int width = smoothedGrayScaleBitmap.getWidth();
        blackAndWhiteBitmap = Bitmap.createBitmap(
                width, height, Bitmap.Config.RGB_565);
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int pixel = smoothedGrayScaleBitmap.getPixel(j, i);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                int gray = ((red + green + blue) / 3);
                if (gray < bwThreshold) {
                    blackAndWhiteBitmap.setPixel(j, i, BLACK_COLOR);
                } else {
                    blackAndWhiteBitmap.setPixel(j, i, WHITE_COLOR);
                }
            }
        }
    }
}
