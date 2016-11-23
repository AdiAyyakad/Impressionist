package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;
import java.text.MessageFormat;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImpressionistImageView imageView;

    private Canvas offScreenCanvas = null;
    private Bitmap offScreenBitmap = null;
    private Paint paint = new Paint();

    private Rect imageViewRect;
    private Bitmap imageViewBitmap;
    private int alpha = 150;
    private boolean useMotionSpeedForBrushStrokeSize = false;
    private VelocityTracker velocityTracker = VelocityTracker.obtain();
    private Paint paintBorder = new Paint();
    private BrushType brushType = BrushType.Square;

    private final float MAX_SPEED = 3000;
    private final float MIN_BRUSH_SIZE = 5;
    private final float DEFAULT_BRUSH_SIZE = 20;
    private final float MAX_BRUSH_SIZE = 50;

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
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
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

    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if (bitmap != null) {
            offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            offScreenCanvas = new Canvas(offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImpressionistImageView imageView){
        this.imageView = imageView;
        updateImageViewInfo();
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        this.brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        if (offScreenCanvas != null) {
            Paint whitePaint = new Paint();
            whitePaint.setColor(Color.WHITE);
            whitePaint.setStyle(Paint.Style.FILL);
            offScreenCanvas.drawRect(0, 0, getWidth(), getHeight(), whitePaint);

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
    public boolean onTouchEvent(MotionEvent motionEvent){
        int index = motionEvent.getActionIndex();
        int pointerId = motionEvent.getPointerId(index);

        float curTouchX = motionEvent.getX();
        float curTouchY = motionEvent.getY();

        if (!validCoordinates(curTouchX, curTouchY)) {
            return true;
        }

        int touchedRGB = getPixelColor(curTouchX, curTouchY);
        paint.setColor(touchedRGB);

        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                velocityTracker.clear();
                velocityTracker.addMovement(motionEvent);
                break;
            case MotionEvent.ACTION_MOVE:
                velocityTracker.addMovement(motionEvent);
                velocityTracker.computeCurrentVelocity(1000);
                double velocity = calculateVelocity(pointerId);
                float velocityBrushSize = calculateBrushSize(velocity);
                int historySize = motionEvent.getHistorySize();
                for (int i = 0; i < historySize; i++) {
                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);

                    if (!validCoordinates(touchX, touchY)) {
                        return true;
                    }

                    paint.setColor(getPixelColor(touchX, touchY));
                    drawShape(touchX, touchY, velocityBrushSize);
                    imageView.addCircle(touchX, touchY, (useMotionSpeedForBrushStrokeSize) ? velocityBrushSize : DEFAULT_BRUSH_SIZE);
                }

                if (!validCoordinates(motionEvent.getX(), motionEvent.getY())) {
                    return true;
                }

                paint.setColor(getPixelColor(motionEvent.getX(), motionEvent.getY()));
                drawShape(motionEvent.getX(), motionEvent.getY(), velocityBrushSize);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                imageView.removeCircle();
        }
        return true;
    }

    public double calculateVelocity (int pointerId) {
        float xVelocity = VelocityTrackerCompat.getXVelocity(velocityTracker, pointerId);
        float yVelocity = VelocityTrackerCompat.getYVelocity(velocityTracker, pointerId);
        double zVelocity = Math.sqrt(Math.pow(xVelocity, 2) + Math.pow(yVelocity, 2));
        return (zVelocity > MAX_SPEED) ? MAX_SPEED : zVelocity;
    }

    private boolean validCoordinates(float x, float y) {
        return validCoordinates((int) x, (int) y);
    }

    private boolean validCoordinates(int x, int y) {
        return (imageViewRect == null) ? false :
            x > imageViewRect.left && x < imageViewRect.right && y > imageViewRect.top && y < imageViewRect.bottom;
    }

    private int getPixelColor(float x, float y) {
        return getPixelColor((int) x, (int) y);
    }

    private int getPixelColor(int x, int y) {
        return (validCoordinates(x, y)) ? imageViewBitmap.getPixel(x, y) : Color.WHITE;
    }

    private void drawShape(float x, float y, float speedBrushSize) {
        float brushSize = (useMotionSpeedForBrushStrokeSize) ? speedBrushSize : DEFAULT_BRUSH_SIZE;
        switch (brushType) {
            case Circle:
                offScreenCanvas.drawCircle(x, y, brushSize, paint);
                break;
            case Square:
                offScreenCanvas.drawRect(x - brushSize, y - brushSize, x + brushSize, y + brushSize, paint);
                break;
            case Line:
                float temp = paint.getStrokeWidth();
                paint.setStrokeWidth(brushSize/4);
                offScreenCanvas.drawLine(x - brushSize, y - brushSize, x + brushSize, y + brushSize, paint);
                paint.setStrokeWidth(temp);
                break;
            default:
                break;
        }
    }

    public float calculateBrushSize(double speed) {
        float brushSize = Math.round((MAX_BRUSH_SIZE * speed) / MAX_SPEED);
        return (brushSize < MIN_BRUSH_SIZE) ? MIN_BRUSH_SIZE : brushSize;
    }

    public void updateImageViewInfo() {
        BitmapDrawable imgDrawable = ((BitmapDrawable) imageView.getDrawable());
        if (imgDrawable != null) {
            imageViewBitmap = imgDrawable.getBitmap();
            imageViewRect = getBitmapPositionInsideImageView(imageView);
        }
    }

    public void speedOfMotionCheckBox(Boolean checked) {
        useMotionSpeedForBrushStrokeSize = checked;
    }

    public Bitmap savePainting() {
        return offScreenBitmap;
    }

    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    public static Rect getBitmapPositionInsideImageView(ImageView imageView){
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

        int top = (imgViewH - heightActual)/2;
        int left = (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

