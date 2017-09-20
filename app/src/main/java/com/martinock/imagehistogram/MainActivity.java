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
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static int BLACK_COLOR = Color.rgb(0, 0, 0);
    private static int WHITE_COLOR = Color.rgb(255, 255, 255);

    private List<Integer> zeroDirection = new ArrayList<>();
    private List<Integer> oneDirection = new ArrayList<>();

    private int bwThreshold = 0;
    private int objectCount = 0;
    private boolean detectObject;

    private ImageView imageView;
    private Button convertButton;
    private LinearLayout llSeekbar;
    private SeekBar thresholdSeekbar;
    private TextView tvSeekbar;
    private Button countButton;

    private LinearLayout llObjectCount0;
    private LinearLayout llObjectCount1;
    private TextView tvObjectCount0;
    private TextView tvObjectCount1;
    private BitmapDrawable originalImageBitmap;
    private Bitmap blackAndWhiteBitmap;

    private ProgressBar progressBar;
    private TextView tvTitle;

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
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        convertButton = (Button) findViewById(R.id.btn_convert_to_bw);
        thresholdSeekbar = (SeekBar) findViewById(R.id.seekbar_bw_threshold);
        tvSeekbar = (TextView) findViewById(R.id.tv_bw_threshold);
        llSeekbar = (LinearLayout) findViewById(R.id.ll_seekbar);
        tvTitle = (TextView) findViewById(R.id.first_title);
        tvObjectCount0 = (TextView) findViewById(R.id.tv_object_count);
        tvObjectCount1 = (TextView) findViewById(R.id.tv_object_count_1);
        llObjectCount0 = (LinearLayout) findViewById(R.id.ll_object_count);
        llObjectCount1 = (LinearLayout) findViewById(R.id.ll_object_count_1);
        countButton = (Button) findViewById(R.id.btn_count_object);
        setSeekbarListener();
        setButtonListener();
        NavigationView navigationView = (NavigationView) findViewById(
                R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setButtonListener() {
        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoading();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        convertToBW();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideLoading();
                                countButton.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }).start();
            }
        });
        countButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoading();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
//                        if (!detectObject) {
                            countObject();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tvObjectCount0.setText(
                                            String.valueOf(objectCount));
                                    llObjectCount0.setVisibility(View.VISIBLE);
                                    hideLoading();
                                }
                            });
//                        } else {
//                            countZeroAndOne();
//                        }
                    }
                }).start();
            }
        });
    }

    private void countZeroAndOne() {
        int heigth = blackAndWhiteBitmap.getHeight();
        int width = blackAndWhiteBitmap.getWidth();
        boolean[][] historyMatrix = new boolean[width][heigth];
        for (int i = 0; i < heigth; ++i) {
            for (int j = 0; j < width; ++j) {
                int pixel = blackAndWhiteBitmap.getPixel(j, i);
                if (pixel == BLACK_COLOR) {
                    blackAndWhiteBitmap.setPixel(j, i, Color.rgb(255, 0, 0));
                } else {
                    historyMatrix[j][i] = true;
                }
            }
        }
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
                int red = Color.red(pixel);
                if (red == 0) {
                    floodFill(j, i, BLACK_COLOR, WHITE_COLOR);
                    objectCount++;
                }
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(copyOfBW);
            }
        });
    }

    private void floodFill(int x, int y, int prevColor, int newColor) {
        Queue<Integer> queueX = new LinkedList<Integer>();
        Queue<Integer> queueY = new LinkedList<Integer>();
        queueX.add(x);
        queueY.add(y);
        while (queueX.size() > 0) {
            int pointX = queueX.poll();
            int pointY = queueY.poll();
            if (blackAndWhiteBitmap.getPixel(pointX, pointY) != prevColor) {
                continue;
            }

            int nextX = pointX + 1;
            while ((pointX > 0) && (blackAndWhiteBitmap.getPixel(pointX, pointY)
                    == prevColor)) {
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

    private void setSeekbarListener() {
        thresholdSeekbar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser) {
                bwThreshold = progress;
                tvSeekbar.setText(String.valueOf(bwThreshold));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
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
            changeImage(R.drawable.photo_date);
            detectObject = true;
        } else {
            changeImage(R.drawable.photo_1);
            detectObject = false;
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
        tvTitle.setVisibility(View.GONE);
        convertButton.setVisibility(View.GONE);
        llSeekbar.setVisibility(View.GONE);
        llObjectCount0.setVisibility(View.GONE);
        llObjectCount1.setVisibility(View.GONE);
        countButton.setVisibility(View.GONE);
    }

    private void showAllView() {
        imageView.setVisibility(View.VISIBLE);
        convertButton.setVisibility(View.VISIBLE);
        llSeekbar.setVisibility(View.VISIBLE);
        convertButton.setVisibility(View.VISIBLE);
        thresholdSeekbar.setVisibility(View.VISIBLE);
        tvSeekbar.setVisibility(View.VISIBLE);
    }

    private void changeImage(int newImageId) {
        Drawable newImage = ResourcesCompat.getDrawable(
                getResources(), newImageId, null);
        imageView.setImageDrawable(newImage);
        originalImageBitmap = (BitmapDrawable) imageView.getDrawable();
    }

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

        imageView.setImageBitmap(blackAndWhiteBitmap);
    }
}