package com.cmsc434.adi.impressionist;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Adi on 11/21/16.
 */

public class ImpressionistImageView extends ImageView {

    private int imageWidth, imageHeight;
    private Rect imageRect;
    private float x = -1, y = -1, size = -1;
    private Resources res;
    // private Bitmap imageBitmap;
    private Paint paint = new Paint();

    public ImpressionistImageView(Context context) {
        super(context);
        setBackgroundColor(Color.WHITE);
        setup();
    }

    public  ImpressionistImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setup();
    }

    public  ImpressionistImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setup();
    }

    private void setup() {
        res = getResources();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        imageRect = ImpressionistView.getBitmapPositionInsideImageView(this);

        imageWidth = imageRect.width();
        imageHeight = imageRect.height();
    }

//    @Override
//    public void setImageBitmap(Bitmap bitmap) {
//        super.setImageBitmap(bitmap);
//        imageBitmap = bitmap;
//    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (x != -1 && y != -1) {
            paint.setColor(((BitmapDrawable) getDrawable()).getBitmap().getPixel((int) x, (int) y));
            canvas.drawCircle(x, y, size, paint);
        }
    }

    public void removeCircle() {
        x = -1;
        y = -1;
        size = -1;

        invalidate();
    }

    public void addCircle(float x, float y, float size) {

        this.x = clamp(x, imageRect.left, imageRect.right);
        this.y = clamp(y, imageRect.top, imageRect.bottom);
        this.size = size;

        invalidate();
    }

    private float clamp(float val, float min, float max) {
        return (val > max) ? max : (val < min) ? min : val;
    }

}
