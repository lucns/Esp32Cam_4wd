package com.esp32_4wd.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;

public class AngleView extends FrameLayout {

    private int totalWidth;
    private RulerView rulerView;
    private float lastAngle;
    private RotateAnimation animation;

    public AngleView(Context context) {
        super(context);
    }

    public AngleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAngle(float angle) {
        if (rulerView == null || (animation != null && !animation.hasEnded()) || lastAngle == angle)
            return;
        float a;
        if (angle - lastAngle > 180) {
            a = 360 - angle;
        } else if (angle - lastAngle < -180) {
            a = angle + 360;
        } else {
            a = angle;
        }
        animation = new RotateAnimation(lastAngle, a, RotateAnimation.ABSOLUTE, rulerView.getWidth() / 2f, RotateAnimation.ABSOLUTE, rulerView.getWidth() / 2f);
        animation.setDuration(100);
        animation.setFillAfter(true);
        rulerView.startAnimation(animation);
        lastAngle = angle;
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                initializeRuler();
            }
        });
    }

    private void initializeRuler() {
        int DISPLAY_WIDTH, DISPLAY_HEIGHT;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            WindowMetrics windowMetrics = windowManager.getCurrentWindowMetrics();
            WindowInsets windowInsets = windowMetrics.getWindowInsets();
            DisplayCutout displayCutout = windowInsets.getDisplayCutout();
            Rect cameraSpace = displayCutout.getBoundingRectLeft();
            //Log.d("Lucas", cameraSpace.width() + "x" + cameraSpace.height());
            int a = windowMetrics.getBounds().width();
            int b = windowMetrics.getBounds().height();
            DISPLAY_WIDTH = Math.min(a, b);
            DISPLAY_HEIGHT = Math.max(a, b) - (cameraSpace.height() / 2) ;
        } else {
            int a = Resources.getSystem().getDisplayMetrics().widthPixels;
            int b = Resources.getSystem().getDisplayMetrics().heightPixels;
            DISPLAY_WIDTH = Math.min(a, b);
            DISPLAY_HEIGHT = Math.max(a, b);
        }
        totalWidth = (int) (DISPLAY_WIDTH * 1.5f);
        //totalWidth = WIDTH_DISPLAY;
        Log.d("lucas", DISPLAY_WIDTH + "x" + DISPLAY_HEIGHT); // 2324x1041
        Log.d("lucas", "totalWidth " + totalWidth);
        rulerView = new RulerView(getContext());
        rulerView.setLayoutParams(new FrameLayout.LayoutParams(totalWidth, totalWidth));
        addView(rulerView);
        setX((DISPLAY_HEIGHT / 2f));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getWidth() == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        setMeasuredDimension(totalWidth, totalWidth);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(totalWidth, MeasureSpec.EXACTLY);
            int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(totalWidth, MeasureSpec.EXACTLY);
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
    }

    private static class RulerView extends View {

        private Paint paint, paintText;
        private Rect rect;
        private final int textSize = 48;

        public RulerView(Context context) {
            super(context);
            initialize();
        }

        private void initialize() {
            paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paintText = new Paint();
            paintText.setColor(Color.WHITE);
            paintText.setTextSize(textSize);
            rect = new Rect();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int radius = RulerView.this.getWidth() / 2;
            //paint.setColor(Color.GREEN);
            //canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(4f);
            canvas.drawCircle(radius, radius, radius, paint);
            paint.setStrokeWidth(1f);
            paint.setColor(Color.WHITE);
            for (int i = 0; i < 360; i += 45)
                drawRotatedText(canvas, i, radius, ((int) (radius / 2.5d)));

            int lineSize = radius / 3;
            int[] coordinates, coordinates2;
            for (double i = 0; i < 360; i += 2.25d) {
                coordinates = getCoordinateAtAngle(i - 90, radius, radius, radius);
                if (i % 45 == 0) {
                    paint.setColor(Color.WHITE);
                    coordinates2 = getCoordinateAtAngle(i - 90, radius - lineSize, radius, radius);
                    canvas.drawLine(coordinates[0], coordinates[1], coordinates2[0], coordinates2[1], paint);
                } else if (i % 11.25d == 0) {
                    paint.setColor(Color.WHITE);
                    coordinates2 = getCoordinateAtAngle(i - 90, radius - ((int) (lineSize / 1.5d)), radius, radius);
                    canvas.drawLine(coordinates[0], coordinates[1], coordinates2[0], coordinates2[1], paint);
                } else {
                    paint.setColor(Color.GRAY);
                    coordinates2 = getCoordinateAtAngle(i - 90, radius - ((int) (lineSize / 2.5d)), radius, radius);
                    canvas.drawLine(coordinates[0], coordinates[1], coordinates2[0], coordinates2[1], paint);
                }
            }

            paintText.setTextSize(textSize / 1.5f);
            paintText.setColor(Color.GRAY);
            for (double i = 22.5d; i < 360; i += 22.5d) {
                if (i % 45 == 0) continue;
                drawRotatedText(canvas, i, radius, ((int) (radius / 5d)));
            }
            paintText.setTextSize(textSize);

            paint.setColor(Color.WHITE);
            int linePadding = (int) (radius / 2f);
            canvas.drawLine(radius, radius, radius - linePadding, radius, paint);
            canvas.drawLine(radius, radius, radius, radius - linePadding, paint);
            canvas.drawLine(radius, radius, radius + linePadding, radius, paint);
            canvas.drawLine(radius, radius, radius, radius + linePadding, paint);
        }

        private void drawRotatedText(Canvas canvas, double angle, int radius, int textPadding) {
            int[] coordinates = getCoordinateAtAngle(angle - 90, radius, radius, radius);
            String text;
            if (angle > 180) text = String.valueOf(angle - 360);
            else text = String.valueOf(angle);
            if (text.endsWith("0") && text.contains("."))
                text = text.substring(0, text.indexOf("."));
            paintText.getTextBounds(text, 0, text.length(), rect);
            canvas.rotate((int) angle, coordinates[0], coordinates[1]);
            canvas.drawText(text, coordinates[0] - (rect.width() / 2f), coordinates[1] + textSize + textPadding, paintText);
            canvas.rotate((int) -angle, coordinates[0], coordinates[1]);
        }

        private void drawCircle(Canvas canvas, int radius, int x, int y) {
            for (double angle = 0; angle < 360; angle += 0.1d) {
                int[] coordinates = getCoordinateAtAngle(angle, radius, x, y);
                canvas.drawRect(coordinates[0], coordinates[1], coordinates[0] + 1, coordinates[1] + 1, paint);
            }
        }

        private int[] getCoordinateAtAngle(double angle, int radius, int x, int y) {
            int x2 = (int) (radius * Math.cos(angle * Math.PI / 180));
            int y2 = (int) (radius * Math.sin(angle * Math.PI / 180));
            return new int[]{x + x2, y + y2};
        }
    }
}
