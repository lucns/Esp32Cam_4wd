package com.esp32_4wd.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class VerticalSeekBar extends SeekBar {

    private OnSeekBarChangeListener onSeek;
    private int lastProgress;

    public VerticalSeekBar(Context context) {
        super(context);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }

    protected void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);
        super.onDraw(c);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        super.setOnSeekBarChangeListener(l);
        onSeek = l;
    }

    @Override
    public void setMax(int max) {
        super.setMax(max);
        lastProgress = max / 2;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        int height = getHeight();
        int max = getMax();
        int progress = max - ((int) (event.getY() / ((float) height / max)));
        if (progress > max) progress = max;
        if (progress < 0) progress = 0;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (onSeek != null) onSeek.onStartTrackingTouch(this);
            case MotionEvent.ACTION_MOVE:
                if (progress == lastProgress) break;
                lastProgress = progress;
                setProgress(progress);
                if (onSeek != null) onSeek.onProgressChanged(this, progress, true);
                onSizeChanged(getWidth(), height, 0, 0);
                break;
            case MotionEvent.ACTION_UP:
                setProgress(progress);
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                if (onSeek != null) onSeek.onStopTrackingTouch(this);
                break;
        }
        return true;
    }
}