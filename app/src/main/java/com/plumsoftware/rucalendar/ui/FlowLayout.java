package com.plumsoftware.rucalendar.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/** Раскладывает дочерние View в строку с переносом; отступ между элементами — 6 dp. */
public class FlowLayout extends ViewGroup {
    private final int gap;

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        gap = Math.round(6 * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        boolean unbounded = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED;
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        int usedWidth = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            int w = child.getMeasuredWidth();
            int h = child.getMeasuredHeight();
            if (!unbounded && x > 0 && x + w > maxWidth) {
                x = 0;
                y += rowHeight + gap;
                rowHeight = 0;
            }
            x += w + gap;
            usedWidth = Math.max(usedWidth, x - gap);
            rowHeight = Math.max(rowHeight, h);
        }
        int width = resolveSize(usedWidth + getPaddingLeft() + getPaddingRight(), widthMeasureSpec);
        int height = resolveSize(y + rowHeight + getPaddingTop() + getPaddingBottom(), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxWidth = r - l - getPaddingLeft() - getPaddingRight();
        int x = 0;
        int y = 0;
        int rowHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            int w = child.getMeasuredWidth();
            int h = child.getMeasuredHeight();
            if (x > 0 && x + w > maxWidth) {
                x = 0;
                y += rowHeight + gap;
                rowHeight = 0;
            }
            int left = getPaddingLeft() + x;
            int top = getPaddingTop() + y;
            child.layout(left, top, left + w, top + h);
            x += w + gap;
            rowHeight = Math.max(rowHeight, h);
        }
    }
}
