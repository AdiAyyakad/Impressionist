package com.cmsc434.adi.impressionist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.UUID;

public class ImpressionistView extends View {
    private Rect imageViewRect;

    private Bitmap imageViewBitmap;

    private ImpressionistImageView imageView;

    private Canvas offScreenCanvas = null;
    private Bitmap offScreenBitmap = null;
    private Paint paint = new Paint();

    private int alpha = 150;
    private float minBrushRadius = 5;
    private float currBrushSize = minBrushRadius;
    private boolean useMotionSpeedForBrushStrokeSize = true;
    private Paint paintBorder = new Paint();
    private BrushType brushType = BrushType.Circle;
    private VelocityTracker mVelocityTracker  = null;
    private Random r;

    private static final double MAX_SPEED = 3000.0;
    private static final float MAX_BRUSH_SIZE = 50;
    private static final float MIN_BRUSH_SIZE = 5;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle) {

        this.setDrawingCacheEnabled(true);

        paint.setColor(Color.RED);
        paint.setAlpha(alpha);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(4);

        paintBorder.setColor(Color.BLACK);
        paintBorder.setStrokeWidth(3);
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setAlpha(50);

        r = new Random();

        //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {

        Bitmap bitmap = getDrawingCache();
        if (bitmap != null) {
            offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            offScreenCanvas = new Canvas(offScreenBitmap);
            clearPainting();
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImpressionistImageView imageView) {
        this.imageView = imageView;
        updateImageViewInfo();
    }


    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType) {
        this.brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting() {
        if (offScreenCanvas != null) {
            Paint whitePaint = new Paint();
            whitePaint.setColor(Color.WHITE);
            whitePaint.setStyle(Paint.Style.FILL);
            offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), whitePaint);
            invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (offScreenBitmap != null) {
            canvas.drawBitmap(offScreenBitmap, 0, 0, paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(imageView), paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int index = motionEvent.getActionIndex();
        int pointerId = motionEvent.getPointerId(index);

        float curTouchX = motionEvent.getX();
        float curTouchY = motionEvent.getY();

        if (!validCoordinates(Math.round(curTouchX), Math.round(curTouchY))) {
            Log.d("ImpressionistView", "Invalid Coordinates 1");
            return true;
        }
        int touchedRGB = getPixelColor(Math.round(curTouchX), Math.round(curTouchY));
        paint.setColor(touchedRGB);

        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mVelocityTracker == null) {
                    // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                    mVelocityTracker = VelocityTracker.obtain();
                }
                else {
                    // Reset the velocity tracker back to its initial state.
                    mVelocityTracker.clear();
                }
                // Add a user's movement to the tracker.
                mVelocityTracker.addMovement(motionEvent);
                break;
            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(motionEvent);
                // When you want to determine the velocity, call
                // computeCurrentVelocity(). Then call getXVelocity()
                // and getYVelocity() to retrieve the velocity for each pointer ID.
                mVelocityTracker.computeCurrentVelocity(1000);
                double velocity = calculateVelocity(pointerId);
                float velocityBrushSize = calculateBrushSize(velocity);
                int historySize = motionEvent.getHistorySize();
                for (int i = 0; i < historySize; i++) {
                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);

                    if (!validCoordinates(Math.round(touchX), Math.round(touchY))) {
                        Log.d("ImpressionitView", "Invalid Coordinates 2");
                        return true;
                    }

                    // TODO: draw to the offscreen bitmap for historical x,y points
                    paint.setColor(getPixelColor(Math.round(touchX), Math.round(touchY)));
                    drawShape(touchX, touchY, velocityBrushSize);
                    imageView.addCircle(touchX, touchY, velocityBrushSize);
                }
                // TODO: draw to the offscreen bitmap for current x,y point.
                // Insert one line of code here
                if (!validCoordinates(Math.round(motionEvent.getX()), Math.round(motionEvent.getY()))) {
                    Log.d("ImpressionitView", "Invalid Coordinates 3");
                    return true;
                }
                paint.setColor(getPixelColor(Math.round(motionEvent.getX()), Math.round(motionEvent.getY())));
                drawShape(motionEvent.getX(), motionEvent.getY(), velocityBrushSize);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                imageView.removeCircle();
        }
        return true;
    }

    public double calculateVelocity (int pointerId) {
        float xVelocity = VelocityTrackerCompat.getXVelocity(mVelocityTracker, pointerId);
        float yVelocity = VelocityTrackerCompat.getYVelocity(mVelocityTracker, pointerId);
        double zVelocity = Math.sqrt(Math.pow(xVelocity, 2) + Math.pow(yVelocity, 2));
        Log.d("VELOCITY", "Velocity = " + zVelocity);
        if (zVelocity > MAX_SPEED) {
            return MAX_SPEED;
        } else {
            return zVelocity;
        }

    }



    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    public static Rect getBitmapPositionInsideImageView(ImageView imageView) {
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }

    private boolean validCoordinates(int x, int y) {
        if (imageViewRect == null || x < 0 || y < 0) {
            return false;
        } else {
           return x > imageViewRect.left && x < imageViewRect.right && y > imageViewRect.top && y < imageViewRect.bottom;
        }
    }

    private int getPixelColor(int x, int y) {
        if (validCoordinates(x, y)) {
            return imageViewBitmap.getPixel(x, y);
        } else {
            return Color.WHITE;
        }
    }

    private void drawShape(float x, float y, float speedBrushSize) {
        switch (brushType) {
            case Circle:
                if (useMotionSpeedForBrushStrokeSize) {
                    offScreenCanvas.drawCircle(x, y, speedBrushSize, paint);
                } else {
                    offScreenCanvas.drawCircle(x, y, currBrushSize, paint);
                }
                break;
            case Square:
                if (useMotionSpeedForBrushStrokeSize) {
                    offScreenCanvas.drawRect(x - speedBrushSize, y - speedBrushSize, x + speedBrushSize, y + speedBrushSize, paint);
                } else {
                    offScreenCanvas.drawRect(x - currBrushSize, y - currBrushSize, x + currBrushSize, y + currBrushSize, paint);
                }
                break;
            case Triangle:
                if (useMotionSpeedForBrushStrokeSize) {

                }
            case Random:
                if (r.nextBoolean()) {
                    offScreenCanvas.drawCircle(x, y, currBrushSize, paint);
                } else {
                    offScreenCanvas.drawRect(x - currBrushSize, y - currBrushSize, x + currBrushSize, y + currBrushSize, paint);
                }
                break;
        }
    }

    public float calculateBrushSize(double speed) {
        float brushSize = Math.round((MAX_BRUSH_SIZE * speed) / MAX_SPEED);
        if (brushSize < MIN_BRUSH_SIZE) {
            return MIN_BRUSH_SIZE;
        } else {
            return brushSize;
        }
    }

    public void updateImageViewInfo() {
        imageViewRect = getBitmapPositionInsideImageView(imageView);
        BitmapDrawable imgDrawable = (BitmapDrawable) imageView.getDrawable();
        imageViewBitmap = imgDrawable.getBitmap();
    }

    public void saveImagePainting(Context context) {

        String fName = UUID.randomUUID().toString() + ".png";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), fName);
        try {
            boolean compressSucceeded = offScreenBitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(file));
            // FileUtils.addImageToGallery(file.getAbsolutePath(), context);
            Toast.makeText(context, "Saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void speedOfMotionCheckBox(Boolean checked) {
        if (checked) {
            useMotionSpeedForBrushStrokeSize = true;

        } else {
            useMotionSpeedForBrushStrokeSize = false;
        }
    }

    public void changeBrushSize(float size) {
        currBrushSize = size;
    }
}

