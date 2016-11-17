package com.cmsc434.adi.impressionist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by Adi on 11/17/16.
 */

public class ImpressionistView extends View {

    ImageView sourceView;
    Bitmap sourceBitmap;
    ArrayList<ImpressionistPath> paths = new ArrayList<>();
    ImpressionistPath currentPath;
    Paint drawPaint = new Paint();
    int paintColor = 00000000;
    Canvas drawCanvas;

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setupPaint();
    }

    private void setupPaint() {
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(20);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        drawCanvas = new Canvas(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
    }

    @Override
    public void onDraw(Canvas canvas) {
        for (ImpressionistPath impressionistPath : paths) {
            for (int i = 0; i < impressionistPath.path.size(); i++) {
                Coord coord = impressionistPath.path.get(i);
                drawPaint.setColor(coord.color);

                if (i == 0) {
                    canvas.drawPoint(coord.x, coord.y, drawPaint);
                } else {
                    Coord lastCoord = impressionistPath.path.get(i-1);
                    canvas.drawLine(lastCoord.x, lastCoord.y, coord.x, coord.y, drawPaint);
                }
            }
        }
    }

    // MARK: - On Touch Events

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new ImpressionistPath(new Coord(x, y, getPixel(x, y)));
                paths.add(currentPath);
                break;
            case MotionEvent.ACTION_MOVE:
                int color = getPixel(x, y);
                Coord lastCoord = currentPath.getLastCoord();

                drawPaint.setColor(color);
                currentPath.add(new Coord(x, y, color));
                drawCanvas.drawLine(lastCoord.x, lastCoord.y, x, y, drawPaint);
                break;
            case MotionEvent.ACTION_UP:
                currentPath = null;
                break;
            default:
                return false;
        }

        invalidate();
        return true;

    }

    private int clamp(int low, int high, int value) {
        return value > high ? high : value < low ? low : value;
    }

    private int getPixel(float x, float y) {
        int imageX = clamp(0, sourceView.getWidth(), (int) x);
        int imageY = clamp(0, sourceView.getHeight(), (int) y);

        return sourceBitmap.getPixel(imageX, imageY);
    }

}

class ImpressionistPath {
    ArrayList<Coord> path = new ArrayList<>();

    public ImpressionistPath(Coord coord) {
        path.add(coord);
    }

    public void add(Coord coord) {
        path.add(coord);
    }

    public Coord getLastCoord() {
        return path.get(path.size()-1);
    }
}

class Coord {
    int x, y, color;

    public Coord(float x, float y, int color) {
        this.x = (int) x;
        this.y = (int) y;
        this.color = color;
    }
}
