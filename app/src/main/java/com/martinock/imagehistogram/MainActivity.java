package com.martinock.imagehistogram;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static int CAMERA_PIC_REQUEST = 1;
    private static int RESULT_LOAD_IMG = 2;

    private static int BLACK_COLOR = Color.rgb(0, 0, 0);
    private static int WHITE_COLOR = Color.rgb(255, 255, 255);
    private static int RED_COLOR = Color.rgb(255, 0, 0);
    private static int GREEN_COLOR = Color.rgb(0, 255, 0);
    private static int EQUALIZATION_CONSTANT = 255;

    private static int MIN_SKIN_COLOR = 0;
    private static int MAX_SKIN_COLOR = 0;

    private int bwThreshold;
    private int componentCount = 0;
    private int objectCount = 0;
    private ArrayList<ChainCode> objectCodes = new ArrayList<>();
    private boolean isGroupImage = false;

    private ImageView imageView;
    private ImageView imageViewGray;
    private ImageView imageViewBW;
    private ImageView imageViewGrayContrast;
    private ImageView imageViewGraySmooth;
    private Button identifyButton;

    private BitmapDrawable originalImageBitmapDrawable;
    private Bitmap grayScaleBitmap;
    private Bitmap blackAndWhiteBitmap;
    private Bitmap newGrayScaleBitmap;
    private Bitmap smoothedGrayScaleBitmap;

    private TextView tvTitle;
    private TextView tvResultName;
    private int[] grayHistogram = new int[256];

    private int maxX, maxY, minX, minY;

    private List<FaceBound> faceBoundList = new ArrayList<>();

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
        imageViewGrayContrast = (ImageView) findViewById(R.id.iv_photo_gray_processed);
        imageViewGraySmooth = (ImageView) findViewById(R.id.iv_photo_gray_smoothed);
        tvTitle = (TextView) findViewById(R.id.first_title);
        identifyButton = (Button) findViewById(R.id.btn_identify);
        tvResultName = (TextView) findViewById(R.id.tv_person_name);
        setButtonListener();
        NavigationView navigationView = (NavigationView) findViewById(
                R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_camera:
                //Open Camera
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, CAMERA_PIC_REQUEST);
                break;
            case R.id.action_file:
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_photo_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_PIC_REQUEST) {
            if (data == null) {
                return;
            }
            tvTitle.setVisibility(View.GONE);
            Bitmap capturedImageBitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(capturedImageBitmap);
            imageView.setVisibility(View.VISIBLE);
            originalImageBitmapDrawable = (BitmapDrawable) imageView.getDrawable();
            processImage();
            identifyButton.setVisibility(View.VISIBLE);
            tvResultName.setText("");
        } else if (requestCode == RESULT_LOAD_IMG) {
            if (data == null) {
                return;
            }
            if (resultCode == RESULT_OK) {
                try {
                    isGroupImage = true;
                    tvTitle.setVisibility(View.GONE);
                    final Uri imageUri = data.getData();
                    final InputStream imageStream = getContentResolver()
                            .openInputStream(imageUri);
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    Bitmap scaledImage = Bitmap.createScaledBitmap(
                            selectedImage,
                            (int)(selectedImage.getWidth()*0.15),
                            (int)(selectedImage.getHeight()*0.15), true);
                    imageView.setImageBitmap(scaledImage);
                    imageView.setVisibility(View.VISIBLE);
                    originalImageBitmapDrawable = (BitmapDrawable) imageView.getDrawable();
                    identifyButton.setVisibility(View.VISIBLE);
                    tvResultName.setText("");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),
                            "Something went wrong", Toast.LENGTH_LONG)
                            .show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Please select image",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setButtonListener() {
        identifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isGroupImage) {
                    countObject();
                    searchFace();
                } else {
                    searchSkinColor();
                    searchFaces();
                }
            }
        });
    }

    private void searchSkinColor() {
        int redMin = SplashActivity.faceRed.get(0);
        int greenMin = SplashActivity.faceBlue.get(0);
        int blueMin = SplashActivity.faceGreen.get(0);
        int redMax = SplashActivity.faceRed.get(0);
        int greenMax = SplashActivity.faceBlue.get(0);
        int blueMax = SplashActivity.faceGreen.get(0);
        for (int i = 1; i < SplashActivity.faceRed.size(); ++i) {
            int red = SplashActivity.faceRed.get(i);
            int green = SplashActivity.faceGreen.get(i);
            int blue = SplashActivity.faceBlue.get(i);
            if (red < redMin) {
                redMin = red;
            }
            if (red > redMax) {
                redMax = red;
            }
            if (green < greenMin) {
                greenMin = green;
            }
            if (green > greenMax) {
                greenMax = green;
            }
            if (blue < blueMin) {
                blueMin = blue;
            }
            if (blue > blueMax) {
                blueMax = blue;
            }
        }
        MIN_SKIN_COLOR = Color.rgb(redMin, greenMin, blueMin);
        MAX_SKIN_COLOR = Color.rgb(redMax, greenMax, blueMax);
    }

    private void searchFaces() {
        int width = originalImageBitmapDrawable.getBitmap().getWidth();
        int height = originalImageBitmapDrawable.getBitmap().getHeight();
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int pixel = originalImageBitmapDrawable.getBitmap().getPixel(j, i);
                if (Color.red(pixel) >= Color.red(MIN_SKIN_COLOR)
                        && Color.red(pixel) <= Color.red(MAX_SKIN_COLOR)
                        && Color.green(pixel) >= Color.green(MIN_SKIN_COLOR)
                        && Color.green(pixel) <= Color.green(MAX_SKIN_COLOR)
                        && Color.blue(pixel) >= Color.blue(MIN_SKIN_COLOR)
                        && Color.blue(pixel) <= Color.blue(MAX_SKIN_COLOR)) {
                    searchFaceEnd(j, i);
                    drawRect();
                }
            }
        }
        isGroupImage = false;
    }

    private void drawRect() {
        for (int i = minY; i <= maxY; ++i) {
            originalImageBitmapDrawable.getBitmap().setPixel(maxX, i, Color.rgb(0, 255, 0));
            originalImageBitmapDrawable.getBitmap().setPixel(minX, i, Color.rgb(0, 255, 0));
        }
        for (int i = minX; i <= maxX; ++i) {
            originalImageBitmapDrawable.getBitmap().setPixel(i, maxY, Color.rgb(0, 255, 0));
            originalImageBitmapDrawable.getBitmap().setPixel(i, minY, Color.rgb(0, 255, 0));
        }
    }

    private void searchFaceEnd(int x, int y) {
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
            int currentColor = originalImageBitmapDrawable.getBitmap()
                    .getPixel(pointX, pointY);
            int currentRed = Color.red(currentColor);
            int currentGreen = Color.green(currentColor);
            int currentBlue = Color.blue(currentColor);
            int minRed = Color.red(MIN_SKIN_COLOR);
            int maxRed = Color.red(MAX_SKIN_COLOR);
            int minGreen = Color.green(MIN_SKIN_COLOR);
            int maxGreen = Color.green(MAX_SKIN_COLOR);
            int minBlue = Color.blue(MIN_SKIN_COLOR);
            int maxBlue = Color.blue(MAX_SKIN_COLOR);
            if (currentRed < minRed
                    || currentRed > maxRed
                    || currentGreen < minGreen
                    || currentGreen > maxGreen
                    || currentBlue < minBlue
                    || currentBlue > maxBlue) {
                continue;
            }

            int nextX = pointX + 1;
            while ((pointX >= 0) && (Color.red(currentColor) >= Color.red(MIN_SKIN_COLOR)
                    && Color.red(currentColor) <= Color.red(MAX_SKIN_COLOR)
                    && Color.green(currentColor) >= Color.green(MIN_SKIN_COLOR)
                    && Color.green(currentColor) <= Color.green(MAX_SKIN_COLOR)
                    && Color.blue(currentColor) >= Color.blue(MIN_SKIN_COLOR)
                    && Color.blue(currentColor) <= Color.blue(MAX_SKIN_COLOR))) {
                if (pointX < minX) {
                    minX = pointX;
                }
                if (pointY > maxY) {
                    maxY = pointY;
                }
                if (pointY < minY) {
                    minY = pointY;
                }
                originalImageBitmapDrawable.getBitmap()
                        .setPixel(pointX, pointY, WHITE_COLOR);

                int aboveCurrentPixel = originalImageBitmapDrawable.getBitmap()
                        .getPixel(pointX, pointY - 1);
                if ((pointY > 0)
                        && (Color.red(aboveCurrentPixel) >= Color.red(MIN_SKIN_COLOR)
                        && Color.red(aboveCurrentPixel) <= Color.red(MAX_SKIN_COLOR)
                        && Color.green(aboveCurrentPixel) >= Color.green(MIN_SKIN_COLOR)
                        && Color.green(aboveCurrentPixel) <= Color.green(MAX_SKIN_COLOR)
                        && Color.blue(aboveCurrentPixel) >= Color.blue(MIN_SKIN_COLOR)
                        && Color.blue(aboveCurrentPixel) <= Color.blue(MAX_SKIN_COLOR))) {
                    queueX.add(pointX);
                    queueY.add(pointY - 1);
                }
                int belowCurrentPixel = originalImageBitmapDrawable.getBitmap().getPixel(pointX, pointY + 1);
                if ((pointY < originalImageBitmapDrawable.getBitmap().getHeight() - 1)
                        && (Color.red(belowCurrentPixel) >= Color.red(MIN_SKIN_COLOR)
                        && Color.red(belowCurrentPixel) <= Color.red(MAX_SKIN_COLOR)
                        && Color.green(belowCurrentPixel) >= Color.green(MIN_SKIN_COLOR)
                        && Color.green(belowCurrentPixel) <= Color.green(MAX_SKIN_COLOR)
                        && Color.blue(belowCurrentPixel) >= Color.blue(MIN_SKIN_COLOR)
                        && Color.blue(belowCurrentPixel) <= Color.blue(MAX_SKIN_COLOR))) {
                    queueX.add(pointX);
                    queueY.add(pointY + 1);
                }
                pointX--;
                currentColor = originalImageBitmapDrawable.getBitmap()
                        .getPixel(pointX, pointY);
            }
            if (nextX >= originalImageBitmapDrawable.getBitmap().getWidth()) {
                nextX = originalImageBitmapDrawable.getBitmap().getWidth() - 1;
            }
            int rightCurrentPixel = originalImageBitmapDrawable.getBitmap()
                    .getPixel(nextX, pointY);
            while ((nextX < originalImageBitmapDrawable.getBitmap().getWidth() - 1)
                    && (Color.red(rightCurrentPixel) >= Color.red(MIN_SKIN_COLOR)
                    && Color.red(rightCurrentPixel) <= Color.red(MAX_SKIN_COLOR)
                    && Color.green(rightCurrentPixel) >= Color.green(MIN_SKIN_COLOR)
                    && Color.green(rightCurrentPixel) <= Color.green(MAX_SKIN_COLOR)
                    && Color.blue(rightCurrentPixel) >= Color.blue(MIN_SKIN_COLOR)
                    && Color.blue(rightCurrentPixel) <= Color.blue(MAX_SKIN_COLOR))) {
                if (pointX > maxX) {
                    maxX = pointX;
                }
                if (pointY > maxY) {
                    maxY = pointY;
                }
                if (pointY < minY) {
                    minY = pointY;
                }
                originalImageBitmapDrawable.getBitmap()
                        .setPixel(nextX, pointY, WHITE_COLOR);

                int aboveNextPixel = originalImageBitmapDrawable.getBitmap().getPixel(
                        nextX, pointY - 1);
                if ((pointY > 0) && (Color.red(aboveNextPixel) >= Color.red(MIN_SKIN_COLOR)
                        && Color.red(aboveNextPixel) <= Color.red(MAX_SKIN_COLOR)
                        && Color.green(aboveNextPixel) >= Color.green(MIN_SKIN_COLOR)
                        && Color.green(aboveNextPixel) <= Color.green(MAX_SKIN_COLOR)
                        && Color.blue(aboveNextPixel) >= Color.blue(MIN_SKIN_COLOR)
                        && Color.blue(aboveNextPixel) <= Color.blue(MAX_SKIN_COLOR))) {
                    queueX.add(nextX);
                    queueY.add(pointY - 1);
                }
                int belowNextPixel = originalImageBitmapDrawable.getBitmap()
                        .getPixel(nextX, pointY + 1);
                if ((pointY < originalImageBitmapDrawable.getBitmap().getHeight() - 1)
                        && (Color.red(belowNextPixel) >= Color.red(MIN_SKIN_COLOR)
                        && Color.red(belowNextPixel) <= Color.red(MAX_SKIN_COLOR)
                        && Color.green(belowNextPixel) >= Color.green(MIN_SKIN_COLOR)
                        && Color.green(belowNextPixel) <= Color.green(MAX_SKIN_COLOR)
                        && Color.blue(belowNextPixel) >= Color.blue(MIN_SKIN_COLOR)
                        && Color.blue(belowNextPixel) <= Color.blue(MAX_SKIN_COLOR))) {
                    queueX.add(nextX);
                    queueY.add(pointY + 1);
                }
                nextX++;
                rightCurrentPixel = originalImageBitmapDrawable.getBitmap()
                        .getPixel(nextX, pointY);
            }
        }
        faceBoundList.add(new FaceBound(maxX, minX, maxY, minY));
    }

    private void searchFace() {
        int leftX = originalImageBitmapDrawable.getBitmap().getWidth(),
                rightX = 0;
        int topY = originalImageBitmapDrawable.getBitmap().getHeight(),
                bottomY = 0;
        for (ChainCode c : objectCodes) {
            if (c.getCentroidX() < leftX
                    && leftX > 0.1 * originalImageBitmapDrawable.getBitmap()
                    .getWidth()) {
                leftX = c.getCentroidX();
            }
            if (c.getCentroidX() > rightX
                    && rightX < 0.9 * originalImageBitmapDrawable.getBitmap()
                    .getWidth()) {
                rightX = c.getCentroidX();
            }
            if (c.getCentroidY() > bottomY
                    && bottomY < 0.8 * originalImageBitmapDrawable.getBitmap()
                    .getHeight()) {
                bottomY = c.getCentroidY();
            }
            if (c.getCentroidY() < topY
                    && topY > 0.2 * originalImageBitmapDrawable.getBitmap()
                    .getHeight()) {
                topY = c.getCentroidY();
            }
        }
        float photoWidthRatio = (float)(rightX - leftX)
                / (float) blackAndWhiteBitmap.getWidth();
        float photoHeightRatio = (float) (bottomY - topY)
                / (float) blackAndWhiteBitmap.getHeight();
        float minError = Float.MAX_VALUE;
        int idxMinError = -1;
        int i = 0;
        for (Face data : SplashActivity.faces) {
            float dataWidthRatio =
                    (float) (data.getMostRightX() - data.getMostLeftX())
                    / (float) (data.getFaceWidth());
            float dataHeightRatio =
                    (float) (data.getMostBottomY() - data.getMostTopY())
                    / (float) (data.getFaceHeight());
            float widthError = Math.abs(photoWidthRatio - dataWidthRatio);
            float heightError = Math.abs(photoHeightRatio - dataHeightRatio);

            if (widthError + heightError < minError) {
                minError = widthError + heightError;
                idxMinError = i;
            }
            i++;
        }
        determineName(idxMinError);
    }

    private void determineName(int idxMinError) {
        switch (idxMinError) {
            case 0:
                tvResultName.setText("This is Nino");
                break;
            case 1:
                tvResultName.setText("This is Kamal");
                break;
            case 2:
                tvResultName.setText("This is Bimo");
                break;
            case 3:
                tvResultName.setText("This is Fari");
                break;
            case 4:
                tvResultName.setText("This is Diaz");
                break;
            case 5:
                tvResultName.setText("This is Dhika");
                break;
            case 6:
                tvResultName.setText("This is Rudi");
                break;
            case 7:
                tvResultName.setText("This is Febi");
                break;
            case 8:
                tvResultName.setText("This is Nugroho");
                break;
            case 9:
                tvResultName.setText("This is Majid");
                break;
            case 10:
                tvResultName.setText("This is Nathan");
                break;
            case 11:
                tvResultName.setText("This is Umay");
                break;
            default:
                tvResultName.setText("Sorry I don't know this face");
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
                if (pixel == BLACK_COLOR) {
                    ChainCode codeObject = new ChainCode(j, i);
                    traceBoundary(j, i, codeObject, width, height);
                    objectCodes.add(codeObject);
                }
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageViewBW.setImageBitmap(copyOfBW);
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

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        tvTitle.setVisibility(View.GONE);
        objectCount = 0;
        showAllView();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showAllView() {
        imageView.setVisibility(View.VISIBLE);
        imageViewGray.setVisibility(View.VISIBLE);
        imageViewBW.setVisibility(View.VISIBLE);
        imageViewGrayContrast.setVisibility(View.VISIBLE);
    }

    private void processImage() {
        initGrayImage();
        otsuThresholding();
        imageViewBW.setVisibility(View.VISIBLE);
        objectCodes.clear();
    }

    /**
     * Search threshold value using otsu thresholding method.
     */
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

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageViewBW.setImageBitmap(blackAndWhiteBitmap);
            }
        });
    }

    /**
     * Draw a grayscale image into a bitmap and set it to ImageView.
     */
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
        imageViewGray.setImageBitmap(grayScaleBitmap);
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
                    * EQUALIZATION_CONSTANT);
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
        imageViewGrayContrast.setImageBitmap(newGrayScaleBitmap);
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
        componentCount = 0;
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
        imageViewGraySmooth.setImageBitmap(smoothedGrayScaleBitmap);
    }
}