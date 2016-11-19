package com.nuk.meetinggo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DrawView extends View {

    private Bitmap backgroundBitmap;
    private Bitmap showBitmap;
    private int backgroundWidth;
    private int backgroundHeight;
    private int currentY;
    private int showHeight;
    private int topSpace;
    private Canvas mCanvas;
    private Rect backgroundSrc;

    public DrawView(final Bitmap backgroundBitmap, Context context, AttributeSet attrs) {
        super(context, attrs);

        // Get top padding length
        topSpace = (int) 10;

        // Get picture width and length
        backgroundWidth = backgroundBitmap.getWidth();
        backgroundHeight = backgroundBitmap.getHeight();

        // Create a picture which is the same with background picture
        showBitmap = Bitmap.createBitmap(backgroundWidth, backgroundHeight, Config.RGB_565);
        // Creat a canvas and bind to the picture
        mCanvas = new Canvas(showBitmap);
        // Draw backgroundBitmap to canvas(showBitmap)
        mCanvas.drawBitmap(backgroundBitmap, 0, 0, null);
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DrawView(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Set the size of picture to the size of view
        setMeasuredDimension(backgroundWidth, backgroundHeight);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Update view
        canvas.drawBitmap(showBitmap, 0, 0, null);
    }

    @Override
    // Update touch event on canvas
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                break;
            case MotionEvent.ACTION_MOVE:

                break;
            case MotionEvent.ACTION_UP:

                break;
        }
        return true;
    }
}
