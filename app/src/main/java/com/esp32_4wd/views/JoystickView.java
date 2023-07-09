package com.esp32_4wd.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;

import com.esp32_4wd.R;

public class JoystickView extends RelativeLayout {

    public interface Callback {

        void onTouchEvent(boolean touched);

        void onChange(int intensity);
    }

    private View track;
    private CircleView circleView;
    private int maxScroll, centerPosition, trackSize, lastIntensity;
    private boolean rotate;
    private Callback callback;
    private final int MAX_VALUE = 1023;

    public JoystickView(Context context) {
        super(context);
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        insertViews();
        track.invalidate();
        circleView.invalidate();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void rotate(boolean rotate) {
        this.rotate = rotate;
    }

    private void insertViews() {
        int circleSize = (int) (getWidth() / 1.5f);
        int trackHalf = getHeight() / 2;
        trackSize = trackHalf * 2;
        maxScroll = trackHalf - (circleSize / 2);
        if (rotate) centerPosition = getWidth() - trackHalf - (circleSize / 2);
        else centerPosition = getHeight() - trackHalf - (circleSize / 2);

        if (track == null) {
            LayoutParams trackParams;
            if (rotate) {
                trackParams = new LayoutParams(trackHalf * 2, 2);
                trackParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
                trackParams.setMargins(0, 0, 0, 0);
            } else {
                trackParams = new LayoutParams(2, getHeight());
                trackParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                trackParams.setMargins(0, 0, 0, 0);
            }
            track = new View(getContext());
            track.setBackgroundColor(Color.WHITE);
            addView(track, trackParams);
            if (rotate) track.setY(trackHalf);
            else track.setX(getWidth() / 2f);
        }

        if (circleView == null) {
            LayoutParams circleParams = new LayoutParams(circleSize, circleSize);
            circleView = new CircleView(getContext());
            addView(circleView, circleParams);
            circleView.setLayoutParams(circleParams);
            circleView.setY((getHeight() / 2f) - (circleSize / 2f));
            circleView.setX((getWidth() / 2f) - (circleSize / 2f));
        }
       //setBackgroundColor(Color.GRAY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int moved, intensity = 0;
        int max = centerPosition + maxScroll;
        int min = centerPosition - maxScroll;
        int circleHalf = (circleView.getWidth() / 2);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                circleView.setPressed(true);
                if (rotate) moved = (int) event.getX() - circleHalf;
                else moved = (int) event.getY() - circleHalf;

                if (moved > max) {
                    moved = max;
                    intensity = -MAX_VALUE;
                } else if (moved < min) {
                    moved = min;
                    intensity = MAX_VALUE;
                } else if (moved == centerPosition) {
                    intensity = 0;
                } else {
                    float scale = 1f - ((float) moved / (maxScroll * 2));
                    intensity = (int) ((float) MAX_VALUE * scale);
                }
                move(moved, 100);
                if (callback != null && lastIntensity != intensity) {
                    callback.onTouchEvent(true);
                    callback.onChange(intensity);
                }
            case MotionEvent.ACTION_MOVE:
                if (rotate) moved = (int) event.getX() - circleHalf;
                else moved = (int) event.getY() - circleHalf;

                if (moved > max) {
                    moved = max;
                    intensity = -MAX_VALUE;
                } else if (moved < min) {
                    moved = min;
                    intensity = MAX_VALUE;
                } else if (moved == centerPosition) {
                    intensity = 0;
                } else {
                    float scale = (float) moved / (maxScroll * 2);
                    intensity = MAX_VALUE - (((int) (MAX_VALUE * scale)) * 2);
                }
                if (rotate) circleView.setX(moved);
                else circleView.setY(moved);
                if (callback != null && lastIntensity != intensity) callback.onChange(intensity);
                break;
            case MotionEvent.ACTION_UP:
                release();
                break;
        }
        lastIntensity = intensity;
        return true;
    }

    public void release() {
        circleView.setPressed(false);
        move(centerPosition, 100);
        if (callback != null) {
            callback.onChange(0);
            callback.onTouchEvent(false);
        }
    }

    private void move(float to, int duration) {
        ViewPropertyAnimator anim = circleView.animate();
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(duration);
        if (rotate) anim.translationX(to);
        else anim.translationY(to);
        anim.start();
    }

    private static class CircleView extends View {

        private final Paint paint;
        private boolean pressed;

        public CircleView(Context context) {
            super(context);
            paint = new Paint();
        }

        @Override
        public void setPressed(boolean pressed) {
            this.pressed = pressed;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int size = CircleView.this.getWidth();
            paint.setColor(getContext().getColor(pressed ? R.color.main : R.color.white_2));
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        }
    }
}
