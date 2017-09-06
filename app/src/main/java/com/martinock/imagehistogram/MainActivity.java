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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int COLOR_BOUNDARIES = 256;
    private int intensity = 1;

    private ImageView imageView;
    private ImageView grayImage;
    private ImageView normalizedImage;
    private ImageView equalizedImage;
    private TextView tvNormalizeTitle;
    private TextView tvEqualizeTitle;

    private LinearLayout llSeekbar;
    private SeekBar seekbarValue;
    private TextView tvValue;
    private int[] redPixel = new int[COLOR_BOUNDARIES];
    private int[] greenPixel = new int[COLOR_BOUNDARIES];
    private int[] bluePixel = new int[COLOR_BOUNDARIES];
    private int[] grayPixel = new int[COLOR_BOUNDARIES];

    private double[] probability = new double[COLOR_BOUNDARIES];
    private double[] cumulative = new double[COLOR_BOUNDARIES];

    private Bitmap grayScaleBitmap;

    private BarChart redChart;
    private BarChart greenChart;
    private BarChart blueChart;
    private BarChart grayChart;
    private ProgressBar progressBar;
    private Button normalizeButton;
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
        grayImage = (ImageView) findViewById(R.id.gray_image);
        normalizedImage = (ImageView) findViewById(R.id.normalize_photo);
        equalizedImage = (ImageView) findViewById(R.id.new_photo);
        tvNormalizeTitle = (TextView) findViewById(R.id.tv_normalize_title);
        tvEqualizeTitle = (TextView) findViewById(R.id.tv_equalize_title);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        redChart = (BarChart) findViewById(R.id.red_chart);
        greenChart = (BarChart) findViewById(R.id.green_chart);
        blueChart = (BarChart) findViewById(R.id.blue_chart);
        grayChart = (BarChart) findViewById(R.id.gray_chart);
        tvTitle = (TextView) findViewById(R.id.first_title);
        normalizeButton = (Button) findViewById(R.id.btn_normalize);
        llSeekbar = (LinearLayout) findViewById(R.id.ll_seekbar);
        seekbarValue = (SeekBar) findViewById(R.id.seekbar_intensity);
        tvValue = (TextView) findViewById(R.id.tv_value);
        setButtonAction();
        setSeekbarListener();
        NavigationView navigationView = (NavigationView) findViewById(
                R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setSeekbarListener() {
        seekbarValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                intensity = progress;
                tvValue.setText(String.valueOf(intensity));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setButtonAction() {
        normalizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeGrayScaleImage();
                showLoading();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        equalizeImage();
                        normalizeImage();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideLoading();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    private void normalizeImage() {
        BitmapDrawable bd = (BitmapDrawable) imageView.getDrawable();
        int height = bd.getBitmap().getHeight();
        int width = bd.getBitmap().getWidth();
        final Bitmap output = bd.getBitmap().copy(Bitmap.Config.RGB_565, true);
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int pixel = bd.getBitmap().getPixel(j, i);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);

                double normalRed = ((double)red/(double)(red + green + blue))
                        * 255;
                double normalGreen = ((double)green/(double)(red + green + blue))
                        * 255;
                double normalBlue = ((double)blue/(double)(red + green + blue))
                        * 255;

                int newPixel = Color.rgb(
                        (int)normalRed,
                        (int)normalGreen,
                        (int)normalBlue);
                output.setPixel(j, i, newPixel);
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                normalizedImage.setImageBitmap(output);
                normalizedImage.setVisibility(View.VISIBLE);
                tvNormalizeTitle.setVisibility(View.VISIBLE);
            }
        });
    }

    private void makeGrayScaleImage() {
        grayImage.setImageBitmap(grayScaleBitmap);
    }

    private void equalizeImage() {
        BitmapDrawable bd = (BitmapDrawable) grayImage.getDrawable();
        int height = bd.getBitmap().getHeight();
        int width = bd.getBitmap().getWidth();
        int totalPixel = height * width;
        //count probability
        for (int i = 0; i < COLOR_BOUNDARIES; ++i) {
            probability[i] = (double)grayPixel[i] / (double)totalPixel;
            cumulative[i] = 0;
        }
        //cumulative
        for (int i = 0; i < COLOR_BOUNDARIES; ++i) {
            for (int j = 0; j <= i; ++j) {
                cumulative[i] = cumulative[i] + probability[j];
            }
        }
        for (int i = 0; i < COLOR_BOUNDARIES; ++i) {
            cumulative[i] = Math.floor((cumulative[i] * intensity));
        }
        final Bitmap output = bd.getBitmap().copy(Bitmap.Config.RGB_565, true);
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int pixel = bd.getBitmap().getPixel(j, i);
                int intensity = Color.red(pixel);
                int newPixel = Color.rgb(
                        (int)cumulative[intensity],
                        (int)cumulative[intensity],
                        (int)cumulative[intensity]);
                output.setPixel(j, i, newPixel);
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                equalizedImage.setImageBitmap(output);
                grayImage.setVisibility(View.VISIBLE);
                equalizedImage.setVisibility(View.VISIBLE);
                tvEqualizeTitle.setVisibility(View.VISIBLE);
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
        } else {
            changeImage(R.drawable.photo_12);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        showLoading();
        new Thread(new Runnable() {
            @Override
            public void run() {
                analyzeBitmap();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoading();
                    }
                });
            }
        }).start();
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
        grayImage.setVisibility(View.GONE);
        equalizedImage.setVisibility(View.GONE);
        normalizedImage.setVisibility(View.GONE);
        tvNormalizeTitle.setVisibility(View.GONE);
        tvEqualizeTitle.setVisibility(View.GONE);
        redChart.setVisibility(View.GONE);
        greenChart.setVisibility(View.GONE);
        blueChart.setVisibility(View.GONE);
        grayChart.setVisibility(View.GONE);
        tvTitle.setVisibility(View.GONE);
        normalizeButton.setVisibility(View.GONE);
        llSeekbar.setVisibility(View.GONE);
    }

    private void showAllView() {
        imageView.setVisibility(View.VISIBLE);
        llSeekbar.setVisibility(View.VISIBLE);
        redChart.setVisibility(View.VISIBLE);
        greenChart.setVisibility(View.VISIBLE);
        blueChart.setVisibility(View.VISIBLE);
        grayChart.setVisibility(View.VISIBLE);
        normalizeButton.setVisibility(View.VISIBLE);
    }

    private void changeImage(int newImageId) {
        Drawable newImage = ResourcesCompat.getDrawable(
                getResources(), newImageId, null);
        imageView.setImageDrawable(newImage);
    }

    private void analyzeBitmap() {
        BitmapDrawable bd = (BitmapDrawable) imageView.getDrawable();
        int height = bd.getBitmap().getHeight();
        int width = bd.getBitmap().getWidth();
        grayScaleBitmap = bd.getBitmap().copy(Bitmap.Config.RGB_565, true);
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int pixel = bd.getBitmap().getPixel(j, i);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                int gray = ((red + green + blue) / 3) % 256;
                redPixel[red]++;
                greenPixel[green]++;
                bluePixel[blue]++;
                grayPixel[gray]++;
                int newPixel = Color.rgb(gray, gray, gray);
                grayScaleBitmap.setPixel(j, i, newPixel);
            }
        }
        visualizeHistogram();
    }

    private void visualizeHistogram() {
        List<BarEntry> redEntries = new ArrayList<>();
        List<BarEntry> greenEntries = new ArrayList<>();
        List<BarEntry> blueEntries = new ArrayList<>();
        List<BarEntry> grayEntries = new ArrayList<>();

        for (int i = 0; i < COLOR_BOUNDARIES; ++i) {
            redEntries.add(new BarEntry(i, redPixel[i]));
            greenEntries.add(new BarEntry(i, greenPixel[i]));
            blueEntries.add(new BarEntry(i, bluePixel[i]));
            grayEntries.add(new BarEntry(i, grayPixel[i]));
        }

        BarDataSet redDataSet = new BarDataSet(redEntries, "Red");
        BarDataSet greenDataSet = new BarDataSet(greenEntries, "Green");
        BarDataSet blueDataSet = new BarDataSet(blueEntries, "Blue");
        BarDataSet grayDataSet = new BarDataSet(grayEntries, "Gray");

        redDataSet.setColor(Color.RED);
        greenDataSet.setColor(Color.GREEN);
        blueDataSet.setColor(Color.BLUE);
        grayDataSet.setColor(Color.GRAY);

        BarData redData = new BarData(redDataSet);
        BarData greenData = new BarData(greenDataSet);
        BarData blueData = new BarData(blueDataSet);
        BarData grayData = new BarData(grayDataSet);

        redChart.setData(redData);
        greenChart.setData(greenData);
        blueChart.setData(blueData);
        grayChart.setData(grayData);

        redChart.invalidate();
        greenChart.invalidate();
        blueChart.invalidate();
        grayChart.invalidate();
    }
}
