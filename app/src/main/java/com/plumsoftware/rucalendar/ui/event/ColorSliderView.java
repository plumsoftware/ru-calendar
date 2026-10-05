package com.plumsoftware.rucalendar.ui.event;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.ui.Motion;

/**
 * Ползунок «Оттенок» / «Яркость» (ТЗ п. 4.6): дорожка 16 dp с градиентом, бегунок 28 dp
 * цвета текущего значения. Бегунок следует за пальцем без задержки, при касании увеличивается до 1,15.
 */
public class ColorSliderView extends View {

    public interface Listener {
        void onValueChanged(ColorSliderView slider, int value);
    }

    private final float density;
    private final float trackHeight;
    private final float thumbRadius;
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();

    private int min = 0;
    private int max = 100;
    private int value = 0;
    private int[] trackColors = {0xFF000000, 0xFFFFFFFF};
    private int thumbColor = 0xFF000000;
    private float thumbScale = 1f;
    private float shaderWidth = -1f;
    @Nullable
    private ValueAnimator scaleAnimator;
    @Nullable
    private Listener listener;

    public ColorSliderView(Context context) {
        this(context, null);
    }

    public ColorSliderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        trackHeight = 16 * density;
        thumbRadius = 14 * density;
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(3 * density);
        ringPaint.setColor(0xFFFFFFFF);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(1.5f * density);
        outlinePaint.setColor(ContextCompat.getColor(context, R.color.ds_text_primary));
        setFocusable(true);
        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(SeekBar.class.getName());
                info.setRangeInfo(AccessibilityNodeInfoCompat.RangeInfoCompat.obtain(
                        AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_INT, min, max, value));
                if (value > min) info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
                if (value < max) info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            }

            @Override
            public boolean performAccessibilityAction(@NonNull View host, int action, @Nullable Bundle args) {
                int step = Math.max(1, (max - min) / 20);
                if (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD) {
                    setValueFromUser(value + step);
                    return true;
                }
                if (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) {
                    setValueFromUser(value - step);
                    return true;
                }
                return super.performAccessibilityAction(host, action, args);
            }
        });
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void setRange(int min, int max) {
        this.min = min;
        this.max = max;
        invalidate();
    }

    public int getValue() {
        return value;
    }

    /** Программная установка, без вызова listener. */
    public void setValue(int value) {
        this.value = clamp(value);
        invalidate();
    }

    public void setTrackColors(int[] colors) {
        trackColors = colors;
        trackPaint.setShader(null);
        invalidate();
    }

    public void setThumbColor(int color) {
        thumbColor = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.round(44 * density));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float cy = getHeight() / 2f;
        track.set(thumbRadius, cy - trackHeight / 2f, getWidth() - thumbRadius, cy + trackHeight / 2f);
        if (trackPaint.getShader() == null || shaderWidth != track.width()) {
            trackPaint.setShader(new LinearGradient(track.left, 0, track.right, 0, trackColors, null, Shader.TileMode.CLAMP));
            shaderWidth = track.width();
        }
        canvas.drawRoundRect(track, trackHeight / 2f, trackHeight / 2f, trackPaint);

        float fraction = max == min ? 0 : (value - min) / (float) (max - min);
        float cx = track.left + fraction * track.width();
        float r = thumbRadius * thumbScale;
        thumbPaint.setColor(thumbColor);
        canvas.drawCircle(cx, cy, r, thumbPaint);
        canvas.drawCircle(cx, cy, r - ringPaint.getStrokeWidth() / 2f, ringPaint);
        canvas.drawCircle(cx, cy, r + outlinePaint.getStrokeWidth() / 2f, outlinePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                animateThumb(1.15f);
                setValueFromTouch(event.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                setValueFromTouch(event.getX());
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                // fall through
            case MotionEvent.ACTION_CANCEL:
                animateThumb(1f);
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void setValueFromTouch(float x) {
        float left = thumbRadius;
        float width = getWidth() - 2 * thumbRadius;
        float fraction = width <= 0 ? 0 : Math.max(0f, Math.min(1f, (x - left) / width));
        setValueFromUser(Math.round(min + fraction * (max - min)));
    }

    private void setValueFromUser(int newValue) {
        newValue = clamp(newValue);
        if (newValue == value) return;
        value = newValue;
        invalidate();
        if (listener != null) listener.onValueChanged(this, value);
    }

    private void animateThumb(float target) {
        if (scaleAnimator != null) scaleAnimator.cancel();
        scaleAnimator = ValueAnimator.ofFloat(thumbScale, target);
        scaleAnimator.setDuration(100);
        scaleAnimator.setInterpolator(Motion.DECELERATE);
        scaleAnimator.addUpdateListener(a -> {
            thumbScale = (float) a.getAnimatedValue();
            invalidate();
        });
        scaleAnimator.start();
    }

    private int clamp(int v) {
        return Math.max(min, Math.min(max, v));
    }
}
