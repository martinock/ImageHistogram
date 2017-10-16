package com.martinock.imagehistogram;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static int BLACK_COLOR = Color.rgb(0, 0, 0);
    private static int WHITE_COLOR = Color.rgb(255, 255, 255);
    private static int RED_COLOR = Color.rgb(255, 0, 0);

    private int bwThreshold = 0;
    private int componentCount = 0;
    private int objectCount = 0;
    private ArrayList<ChainCode> objectCodes = new ArrayList<>();

    private ImageView imageView;
    private ImageView imageViewGray;
    private ImageView imageViewBW;
    private Button countButton;
    private ImageView ivResult;

    private LinearLayout llObjectCount0;
    private TextView tvObjectCount0;
    private BitmapDrawable originalImageBitmap;
    private Bitmap grayscaleBitmap;
    private Bitmap blackAndWhiteBitmap;

    private ProgressBar progressBar;
    private TextView tvTitle;
    private int[] grayHistogram = new int[256];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        imageView = (ImageView) findViewById(R.id.iv_photo);
        imageViewGray = (ImageView) findViewById(R.id.iv_photo_gray);
        imageViewBW = (ImageView) findViewById(R.id.iv_photo_bw);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        tvTitle = (TextView) findViewById(R.id.first_title);
        tvObjectCount0 = (TextView) findViewById(R.id.tv_object_count);
        llObjectCount0 = (LinearLayout) findViewById(R.id.ll_object_count);
        countButton = (Button) findViewById(R.id.btn_count_object);
        ivResult = (ImageView) findViewById(R.id.iv_result);
        setButtonListener();
        NavigationView navigationView = (NavigationView) findViewById(
                R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setButtonListener() {
        countButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoading();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        countObject();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvObjectCount0.setText(
                                        String.valueOf(objectCount));
                                llObjectCount0.setVisibility(View.VISIBLE);
                                ivResult.setVisibility(View.VISIBLE);
                                hideLoading();
                            }
                            });
                    }
                }).start();
            }
        });
    }

    private void countObject() {
        objectCount = 0;
        int height = blackAndWhiteBitmap.getHeight();
        int width = blackAndWhiteBitmap.getWidth();
        final Bitmap copyOfBW = blackAndWhiteBitmap.copy(
                Bitmap.Config.RGB_565, true);
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
        drawResult();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(copyOfBW);
            }
        });
    }

    private void drawResult() {
        int width = originalImageBitmap.getBitmap().getWidth();
        int height = originalImageBitmap.getBitmap().getHeight();
        final Bitmap resultBitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.RGB_565);
        for (ChainCode c : objectCodes) {
            int currentX = c.getStartX();
            int currentY = c.getStartY();
            resultBitmap.setPixel(currentX, currentY, RED_COLOR);
            for (int dir : c.getCode()) {
                switch (dir) {
                    case 0:
                        currentX = currentX + 1;
                        break;
                    case 1:
                        currentX = currentX + 1;
                        currentY = currentY - 1;
                        break;
                    case 2:
                        currentY = currentY - 1;
                        break;
                    case 3:
                        currentX = currentX - 1;
                        currentY = currentY - 1;
                        break;
                    case 4:
                        currentX = currentX - 1;
                        break;
                    case 5:
                        currentX = currentX - 1;
                        currentY = currentY + 1;
                        break;
                    case 6:
                        currentY = currentY + 1;
                        break;
                    case 7:
                        currentX = currentX + 1;
                        currentY = currentY + 1;
                        break;
                }
                resultBitmap.setPixel(currentX, currentY, RED_COLOR);
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ivResult.setImageBitmap(resultBitmap);
            }
        });
    }

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
                    if (componentCount >= 40) {
                        objectCount++;
                    }
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

    private void floodFill(int x, int y, int prevColor, int newColor) {
        componentCount = 0;
        Queue<Integer> queueX = new LinkedList<Integer>();
        Queue<Integer> queueY = new LinkedList<Integer>();
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
                blackAndWhiteBitmap.setPixel(pointX, pointY, newColor);
                componentCount++;
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
                blackAndWhiteBitmap.setPixel(nextX, pointY, newColor);
                componentCount++;

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

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.photo_1) {
            changeImage(R.drawable.photo_1);
        } else if (id == R.id.photo_2) {
            changeImage(R.drawable.photo_2);
        } else if (id == R.id.photo_3) {
            changeImage(R.drawable.photo_3);
        } else if (id == R.id.photo_4) {
            changeImage(R.drawable.photo_4);
        } else if (id == R.id.photo_5) {
            changeImage(R.drawable.photo_5);
        } else if (id == R.id.photo_6) {
            changeImage(R.drawable.photo_6);
        } else if (id == R.id.photo_7) {
            changeImage(R.drawable.photo_7);
        } else if (id == R.id.photo_8) {
            changeImage(R.drawable.photo_8);
        } else if (id == R.id.photo_9) {
            changeImage(R.drawable.photo_9);
        } else if (id == R.id.photo_10) {
            changeImage(R.drawable.photo_10);
        } else if (id == R.id.photo_11) {
            changeImage(R.drawable.photo_11);
        } else if (id == R.id.photo_12) {
            changeImage(R.drawable.photo_12);
        }
        tvTitle.setVisibility(View.GONE);
        objectCount = 0;
        showAllView();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        showAllView();
    }

    private void showLoading() {
        hideAllView();
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideAllView() {
        imageView.setVisibility(View.GONE);
        imageViewGray.setVisibility(View.GONE);
        imageViewBW.setVisibility(View.GONE);
        tvTitle.setVisibility(View.GONE);
        llObjectCount0.setVisibility(View.GONE);
        countButton.setVisibility(View.GONE);
    }

    private void showAllView() {
        imageView.setVisibility(View.VISIBLE);
        imageViewGray.setVisibility(View.VISIBLE);
        imageViewBW.setVisibility(View.VISIBLE);
    }

    private void changeImage(int newImageId) {
        Drawable newImage = ResourcesCompat.getDrawable(
                getResources(), newImageId, null);
        imageView.setImageDrawable(newImage);
        originalImageBitmap = (BitmapDrawable) imageView.getDrawable();
        initGrayImage();
        ivResult.setImageDrawable(null);
        ivResult.setImageBitmap(null);
        ivResult.setImageResource(0);
        objectCodes.clear();
    }

    private void initGrayImage() {
        int height = originalImageBitmap.getBitmap().getHeight();
        int width = originalImageBitmap.getBitmap().getWidth();
        grayscaleBitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.RGB_565);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pixel = originalImageBitmap.getBitmap().getPixel(j, i);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                int gray = ((red + green + blue) / 3);
                grayHistogram[gray]++;
                int grayColor = Color.rgb(gray, gray, gray);
                grayscaleBitmap.setPixel(j, i, grayColor);
            }
        }
        imageViewGray.setImageBitmap(grayscaleBitmap);
    }

    //TODO: implement this with otsu thresholding
    private void convertToBW() {
        int height = originalImageBitmap.getBitmap().getHeight();
        int width = originalImageBitmap.getBitmap().getWidth();
        blackAndWhiteBitmap = Bitmap.createBitmap(
                width, height, Bitmap.Config.RGB_565);
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int pixel = originalImageBitmap.getBitmap().getPixel(j, i);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                int gray = ((red + green + blue) / 3) % 256;
                if (gray < bwThreshold) {
                    blackAndWhiteBitmap.setPixel(j, i, BLACK_COLOR);
                } else {
                    blackAndWhiteBitmap.setPixel(j, i, WHITE_COLOR);
                }
            }
        }

        imageViewBW.setImageBitmap(blackAndWhiteBitmap);
    }
}