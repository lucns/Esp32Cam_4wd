package com.esp32_4wd.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.esp32_4wd.R;

public class LedView extends View {

    private Paint paint;
    private Handler handler;
    private boolean flasher;

    public LedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) init();
    }

    public LedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) init();
    }

    public LedView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (!isInEditMode()) init();
    }

    public LedView(Context context) {
        super(context);
        if (!isInEditMode()) init();
    }

    private void init() {
        paint = new Paint();
        handler = new Handler();
        flasher = true;
    }

    public void setFlasher(boolean f) {
        flasher = f;
    }

    @Override
    public void setActivated(boolean activated) {
        super.setActivated(activated);
        restore();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        restore();
    }

    private void restore() {
        if (!flasher) return;
        handler.removeCallbacks(run);
        handler.postDelayed(run, 100);
    }

    private final Runnable run = new Runnable() {
        @Override
        public void run() {
            setActivated(false);
            setEnabled(true);
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;

        if (isEnabled() && isActivated()) paint.setColor(getContext().getColor(R.color.main));
        else if (isEnabled()) paint.setColor(getContext().getColor(R.color.white_2));
        else paint.setColor(getContext().getColor(R.color.red));

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
    }
}
